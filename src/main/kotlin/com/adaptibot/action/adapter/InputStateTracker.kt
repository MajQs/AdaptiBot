package com.adaptibot.action.adapter

import com.adaptibot.action.adapter.winapi.User32
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

internal object InputStateTracker {

    private val logger = LoggerFactory.getLogger(InputStateTracker::class.java)

    private const val KEYEVENTF_KEYUP = 0x0002
    private const val KEYEVENTF_UNICODE = 0x0004

    private val user32 get() = User32.INSTANCE

    private val heldKeys = ConcurrentHashMap.newKeySet<Int>()
    private val heldCharacters = ConcurrentHashMap.newKeySet<Short>()
    private val heldMouseButtons = ConcurrentHashMap<Int, Int>() // down flag -> up flag

    fun <T> holdingKey(virtualKeyCode: Int, whileHeld: () -> T): T {
        heldKeys.add(virtualKeyCode)
        try {
            user32.keybd_event(virtualKeyCode.toByte(), 0, 0, 0)
            return whileHeld()
        } finally {
            user32.keybd_event(virtualKeyCode.toByte(), 0, KEYEVENTF_KEYUP, 0)
            heldKeys.remove(virtualKeyCode)
        }
    }

    fun <T> holdingKeys(
        virtualKeyCodes: List<Int>,
        afterEachPress: () -> Unit = {},
        whileHeld: () -> T
    ): T {
        val first = virtualKeyCodes.firstOrNull() ?: return whileHeld()
        return holdingKey(first) {
            afterEachPress()
            holdingKeys(virtualKeyCodes.drop(1), afterEachPress, whileHeld)
        }
    }

    fun <T> holdingCharacter(character: Char, whileHeld: () -> T): T {
        val scanCode = character.code.toShort()
        heldCharacters.add(scanCode)
        try {
            user32.keybd_event(0, scanCode, KEYEVENTF_UNICODE, 0)
            return whileHeld()
        } finally {
            user32.keybd_event(0, scanCode, KEYEVENTF_UNICODE or KEYEVENTF_KEYUP, 0)
            heldCharacters.remove(scanCode)
        }
    }

    fun <T> holdingMouseButton(downFlag: Int, upFlag: Int, whileHeld: () -> T): T {
        heldMouseButtons[downFlag] = upFlag
        try {
            user32.mouse_event(downFlag, 0, 0, 0, 0)
            return whileHeld()
        } finally {
            user32.mouse_event(upFlag, 0, 0, 0, 0)
            heldMouseButtons.remove(downFlag)
        }
    }

    fun releaseAll() {
        heldMouseButtons.values.toList().forEach { upFlag ->
            runCatching { user32.mouse_event(upFlag, 0, 0, 0, 0) }
                .onFailure { logger.warn("Failed to release mouse button flag $upFlag", it) }
        }
        heldMouseButtons.clear()

        heldCharacters.toList().forEach { scanCode ->
            runCatching { user32.keybd_event(0, scanCode, KEYEVENTF_UNICODE or KEYEVENTF_KEYUP, 0) }
                .onFailure { logger.warn("Failed to release character $scanCode", it) }
        }
        heldCharacters.clear()

        heldKeys.toList().forEach { virtualKeyCode ->
            runCatching { user32.keybd_event(virtualKeyCode.toByte(), 0, KEYEVENTF_KEYUP, 0) }
                .onFailure { logger.warn("Failed to release key $virtualKeyCode", it) }
        }
        heldKeys.clear()
    }
}

