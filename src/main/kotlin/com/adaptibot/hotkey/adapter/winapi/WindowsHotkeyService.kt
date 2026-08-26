package com.adaptibot.hotkey.adapter.winapi

import com.adaptibot.hotkey.domain.GlobalHotkeyService
import com.adaptibot.hotkey.domain.HotkeyNativeApi
import com.adaptibot.hotkey.model.HotkeyCombination
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

internal class WindowsHotkeyService(
    private val nativeApi: HotkeyNativeApi,
    private val loopTimeoutMs: Long = 2_000
) : GlobalHotkeyService {

    private val logger = LoggerFactory.getLogger(WindowsHotkeyService::class.java)

    private val idGenerator = AtomicInteger(1)
    private val registrations = ConcurrentHashMap<HotkeyCombination, Registration>()

    override fun register(combination: HotkeyCombination, onTriggered: () -> Unit): Boolean {
        if (registrations.containsKey(combination)) {
            logger.warn("Hotkey $combination is already registered")
            return true
        }

        val registration = Registration(idGenerator.getAndIncrement())
        val registered = CountDownLatch(1)

        // Registration and the message loop must live on the same thread - that thread owns the hotkey
        registration.thread = Thread({ runHotkeyLoop(combination, registration, registered, onTriggered) },
            "global-hotkey-${combination.keyName}")
            .apply { isDaemon = true }
            .also { it.start() }

        if (!registered.await(loopTimeoutMs, TimeUnit.MILLISECONDS)) {
            logger.warn("Timed out while registering hotkey $combination")
            return false
        }
        if (!registration.successful) {
            logger.warn("Hotkey $combination is unavailable, probably taken by another application")
            return false
        }

        registrations[combination] = registration
        logger.info("Global hotkey registered: $combination")
        return true
    }

    override fun unregister(combination: HotkeyCombination) {
        val registration = registrations.remove(combination) ?: return
        nativeApi.requestLoopExit(registration.threadId)
        registration.thread?.join(loopTimeoutMs)
        logger.info("Global hotkey released: $combination")
    }

    override fun close() {
        registrations.keys.toList().forEach(::unregister)
    }

    private fun runHotkeyLoop(
        combination: HotkeyCombination,
        registration: Registration,
        registered: CountDownLatch,
        onTriggered: () -> Unit
    ) {
        registration.threadId = nativeApi.currentThreadId()
        registration.successful = nativeApi.registerHotKey(
            id = registration.id,
            modifierMask = combination.modifierMask,
            virtualKeyCode = combination.virtualKeyCode
        )
        registered.countDown()

        if (!registration.successful) return

        try {
            while (true) {
                val triggeredId = nativeApi.waitForNextHotkey() ?: break
                if (triggeredId != registration.id) continue
                runCatching { onTriggered() }
                    .onFailure { logger.error("Hotkey handler for $combination failed", it) }
            }
        } finally {
            nativeApi.unregisterHotKey(registration.id)
        }
    }

    private class Registration(val id: Int) {
        @Volatile var threadId: Int = 0
        @Volatile var successful: Boolean = false
        @Volatile var thread: Thread? = null
    }
}

