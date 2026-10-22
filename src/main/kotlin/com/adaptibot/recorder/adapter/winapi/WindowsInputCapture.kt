package com.adaptibot.recorder.adapter.winapi

import com.adaptibot.recorder.domain.InputCapture
import com.adaptibot.recorder.domain.InputCaptureException
import com.adaptibot.recorder.domain.RecordedEvent
import com.adaptibot.recorder.domain.ScreenPoint
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal class WindowsInputCapture(
    private val nativeApi: WinApiInputCaptureNativeApi = WinApiInputCaptureNativeApi(),
    private val startupTimeoutMillis: Long = 2_000
) : InputCapture {

    private val lock = Any()
    private var session: CaptureSession? = null

    override fun start(onEvent: (RecordedEvent) -> Unit, onFailure: (Throwable) -> Unit) {
        val newSession = synchronized(lock) {
            if (session != null) throw InputCaptureException("Input capture is already running")
            CaptureSession(onEvent, onFailure).also { session = it }
        }

        try {
            newSession.thread = Thread(
                { runMessageLoop(newSession) },
                "recorder-mouse-hook"
            ).apply {
                isDaemon = true
                start()
            }
        } catch (failure: Throwable) {
            synchronized(lock) {
                if (session === newSession) session = null
            }
            throw InputCaptureException("Could not start the Windows mouse hook thread", failure)
        }

        val started = try {
            newSession.started.await(startupTimeoutMillis, TimeUnit.MILLISECONDS)
        } catch (interrupted: InterruptedException) {
            stopSession(newSession)
            Thread.currentThread().interrupt()
            throw InputCaptureException("Interrupted while installing the Windows mouse hook", interrupted)
        }
        if (!started) {
            stopSession(newSession)
            throw InputCaptureException("Timed out while installing the Windows mouse hook")
        }

        newSession.startFailure?.let { failure ->
            stopSession(newSession)
            throw InputCaptureException("Could not install the Windows mouse hook", failure)
        }
    }

    override fun stop() {
        val activeSession = synchronized(lock) { session } ?: return
        stopSession(activeSession)
    }

    private fun runMessageLoop(capture: CaptureSession) {
        var startedSuccessfully = false
        try {
            capture.threadId = nativeApi.currentThreadId()
            nativeApi.createMessageQueue()
            if (capture.stopRequested.get()) {
                throw InputCaptureException("Windows mouse hook startup was cancelled")
            }
            capture.hook = nativeApi.installMouseHook(capture.callback)
                ?: throw InputCaptureException("SetWindowsHookExW returned null")
            if (capture.stopRequested.get()) nativeApi.requestLoopExit(capture.threadId)
            startedSuccessfully = true
            capture.started.countDown()

            while (nativeApi.waitForNextMessage()) Unit
        } catch (failure: Throwable) {
            if (!startedSuccessfully) {
                capture.startFailure = failure
                capture.started.countDown()
            } else if (!capture.stopRequested.get()) {
                capture.terminalFailure = InputCaptureException("Windows mouse hook loop failed", failure)
            }
        } finally {
            capture.hook?.let(nativeApi::removeMouseHook)
            capture.hook = null
            capture.finished.countDown()
            synchronized(lock) {
                if (session === capture) session = null
            }
            capture.terminalFailure?.let { failure ->
                if (capture.failureReported.compareAndSet(false, true)) {
                    runCatching { capture.onFailure(failure) }
                }
            }
        }
    }

    private fun stopSession(capture: CaptureSession) {
        if (capture.stopRequested.compareAndSet(false, true)) {
            val threadId = capture.threadId
            if (threadId != 0) nativeApi.requestLoopExit(threadId)
        }

        val thread = capture.thread
        if (thread != null && thread !== Thread.currentThread()) {
            var interrupted = false
            while (thread.isAlive) {
                try {
                    thread.join()
                } catch (_: InterruptedException) {
                    interrupted = true
                }
            }
            if (interrupted) Thread.currentThread().interrupt()
        } else if (thread === Thread.currentThread()) {
            // A callback can request stop. Remove the hook synchronously so no later callback can arrive;
            // the current platform thread consumes WM_QUIT as soon as this callback returns.
            capture.hook?.let(nativeApi::removeMouseHook)
            capture.hook = null
        }
    }

    private inner class CaptureSession(
        val onEvent: (RecordedEvent) -> Unit,
        val onFailure: (Throwable) -> Unit
    ) {
        val started = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val stopRequested = AtomicBoolean(false)
        val failureReported = AtomicBoolean(false)
        @Volatile var threadId: Int = 0
        @Volatile var thread: Thread? = null
        @Volatile var hook: WinUser.HHOOK? = null
        @Volatile var startFailure: Throwable? = null
        @Volatile var terminalFailure: Throwable? = null

        // Strongly owned by the session for the complete native hook lifetime.
        val callback = InputCaptureUser32.LowLevelMouseProc { code, wParam, lParam ->
            handleHookCallback(this, code, wParam, lParam)
        }
    }

    private fun handleHookCallback(
        capture: CaptureSession,
        code: Int,
        wParam: WinDef.WPARAM,
        lParam: WinDef.LPARAM
    ): WinDef.LRESULT {
        try {
            if (code >= 0 && wParam.toInt() == InputCaptureUser32.WM_LBUTTONDOWN) {
                val data = nativeApi.readMouseHookData(lParam)
                val injectedMask = InputCaptureUser32.LLMHF_INJECTED or
                    InputCaptureUser32.LLMHF_LOWER_IL_INJECTED
                if (data.flags.toInt() and injectedMask == 0) {
                    capture.onEvent(
                        RecordedEvent.MouseClick(
                            point = ScreenPoint(data.pt.x, data.pt.y),
                            nativeTimestampMillis = data.time.toLong() and 0xFFFF_FFFFL
                        )
                    )
                }
            }
        } catch (failure: Throwable) {
            if (capture.terminalFailure == null) {
                capture.terminalFailure = InputCaptureException("Windows mouse hook callback failed", failure)
                if (capture.stopRequested.compareAndSet(false, true)) {
                    nativeApi.requestLoopExit(capture.threadId)
                }
            }
        }
        return nativeApi.callNextHook(capture.hook, code, wParam, lParam)
    }
}
