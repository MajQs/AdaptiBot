package com.adaptibot.recorder.domain

import com.adaptibot.recorder.dto.CompletionReason
import com.adaptibot.recorder.dto.CoordinateDto
import com.adaptibot.recorder.dto.RecordedEventDto
import com.adaptibot.recorder.dto.RecordingDto
import com.adaptibot.recorder.dto.RecordingResultDto
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * Recording lifecycle: IDLE -> RECORDING -> STOPPING -> IDLE. Delivers exactly one result per session.
 */
internal class Recorder(
    private val inputCapture: InputCapture,
    private val recordingDurationLimitMillis: Long = MAX_RECORDING_DURATION_MILLIS,
    private val acceptedClickLimit: Int = MAX_ACCEPTED_CLICKS
) {

    private val logger = LoggerFactory.getLogger(Recorder::class.java)

    private val lock = Any()
    private var state = RecorderState.IDLE
    private var onCompleted: ((RecordingResultDto) -> Unit)? = null
    private val events = ArrayList<RecordedEvent.MouseClick>()
    private var recordingDeadlineNanos = 0L
    private var deadlineThread: Thread? = null

    fun isRecording(): Boolean = synchronized(lock) { state == RecorderState.RECORDING }

    /** @return `false` when a session is already active; otherwise the session is started and the callback fires once. */
    fun start(onCompleted: (RecordingResultDto) -> Unit): Boolean {
        synchronized(lock) {
            if (state != RecorderState.IDLE) return false
            state = RecorderState.RECORDING
            this.onCompleted = onCompleted
            events.clear()
            recordingDeadlineNanos = System.nanoTime() +
                TimeUnit.MILLISECONDS.toNanos(recordingDurationLimitMillis)
        }
        try {
            inputCapture.start(::onEvent, ::onCaptureFailure)
            startDeadlineTimer()
        } catch (e: Exception) {
            if (beginStopping()) {
                finishAfterCaptureStopped(::failed)
            }
        }
        return true
    }

    fun stop() {
        if (!beginStopping()) return
        finishAfterCaptureStopped { completed(CompletionReason.USER_STOPPED) }
    }

    fun close() {
        if (!beginStopping()) return
        finishAfterCaptureStopped(::failed)
    }

    private fun onEvent(event: RecordedEvent) {
        if (event !is RecordedEvent.MouseClick) return
        var completionReason: CompletionReason? = null
        synchronized(lock) {
            if (state != RecorderState.RECORDING) return
            if (deadlineReached()) {
                state = RecorderState.STOPPING
                completionReason = CompletionReason.TIME_LIMIT_REACHED
            } else {
                events += event
                if (events.size >= acceptedClickLimit) {
                    state = RecorderState.STOPPING
                    completionReason = CompletionReason.CLICK_LIMIT_REACHED
                }
            }
        }
        completionReason?.let(::completeAutomaticallyAsync)
    }

    private fun onCaptureFailure(cause: Throwable) {
        if (!beginStopping()) return
        finishAfterCaptureStopped(::failed)
    }

    private fun beginStopping(): Boolean = synchronized(lock) {
        if (state != RecorderState.RECORDING) return false
        state = RecorderState.STOPPING
        true
    }

    private fun finishAfterCaptureStopped(result: () -> RecordingResultDto) {
        val outcome = try {
            inputCapture.stop()
            result()
        } catch (e: Exception) {
            failed()
        }
        finish { outcome }
    }

    private fun finish(result: () -> RecordingResultDto) {
        val callback: ((RecordingResultDto) -> Unit)?
        val outcome: RecordingResultDto
        synchronized(lock) {
            callback = onCompleted
            onCompleted = null
            outcome = result()
            events.clear()
            cancelDeadlineTimer()
            state = RecorderState.IDLE
        }
        callback?.invoke(outcome)
    }

    private fun failed(): RecordingResultDto = RecordingResultDto.Failed

    private fun completed(reason: CompletionReason): RecordingResultDto {
        val snapshot = synchronized(lock) { events.toList() }
        var previous: Long? = null
        val dtos = snapshot.map { click ->
            val delay = previous?.let { previousTimestamp ->
                val difference = (click.nativeTimestampMillis - previousTimestamp) and WINDOWS_TIMESTAMP_MASK
                if (difference >= INVALID_TIMESTAMP_DIFFERENCE) 0L else difference
            } ?: 0L
            previous = click.nativeTimestampMillis
            RecordedEventDto.MouseClick(CoordinateDto(click.point.x, click.point.y), delay)
        }
        return RecordingResultDto.Completed(RecordingDto(dtos, reason))
    }

    private fun startDeadlineTimer() {
        synchronized(lock) {
            if (state != RecorderState.RECORDING) return
            val timer = Thread.ofVirtual().name("recorder-deadline").unstarted {
                if (awaitDeadline() && beginStoppingForDeadline(Thread.currentThread())) {
                    completeAutomaticallyAsync(CompletionReason.TIME_LIMIT_REACHED)
                }
            }
            deadlineThread = timer
            timer.start()
        }
    }

    private fun awaitDeadline(): Boolean {
        try {
            while (true) {
                val remainingNanos = synchronized(lock) {
                    if (state != RecorderState.RECORDING) return false
                    recordingDeadlineNanos - System.nanoTime()
                }
                if (remainingNanos <= 0L) return true
                TimeUnit.NANOSECONDS.sleep(remainingNanos)
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            return false
        }
    }

    private fun beginStoppingForDeadline(timer: Thread): Boolean = synchronized(lock) {
        if (state != RecorderState.RECORDING || deadlineThread !== timer || !deadlineReached()) return false
        state = RecorderState.STOPPING
        true
    }

    private fun deadlineReached(): Boolean = System.nanoTime() - recordingDeadlineNanos >= 0L

    private fun completeAutomaticallyAsync(reason: CompletionReason) {
        Thread.ofVirtual().name("recorder-limit-completion").start {
            completeAutomatically(reason)
        }
    }

    private fun completeAutomatically(reason: CompletionReason) {
        when (reason) {
            CompletionReason.TIME_LIMIT_REACHED ->
                logger.warn("Recording stopped after reaching the 10-minute limit.")
            CompletionReason.CLICK_LIMIT_REACHED ->
                logger.warn("Recording stopped after reaching the 10,000-click limit.")
            CompletionReason.USER_STOPPED -> error("User stop is not an automatic completion reason")
        }
        finishAfterCaptureStopped { completed(reason) }
    }

    private fun cancelDeadlineTimer() {
        deadlineThread?.takeUnless { it === Thread.currentThread() }?.interrupt()
        deadlineThread = null
    }

    private companion object {
        const val MAX_RECORDING_DURATION_MILLIS = 10L * 60L * 1_000L
        const val MAX_ACCEPTED_CLICKS = 10_000
        const val WINDOWS_TIMESTAMP_MASK = 0xFFFF_FFFFL
        const val INVALID_TIMESTAMP_DIFFERENCE = 0x8000_0000L
    }
}
