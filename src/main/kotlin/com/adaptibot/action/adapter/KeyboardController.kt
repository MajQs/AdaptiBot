package com.adaptibot.action.adapter

import com.adaptibot.action.adapter.winapi.User32
import com.adaptibot.action.adapter.winapi.VirtualKeyCodes
import com.adaptibot.common.model.Key
import org.slf4j.LoggerFactory

object KeyboardController {

    private val logger = LoggerFactory.getLogger(KeyboardController::class.java)
    private val user32 = User32.Companion.INSTANCE

    private const val KEYEVENTF_KEYUP = 0x0002
    private const val KEYEVENTF_UNICODE = 0x0004

    fun typeText(text: String) {
        text.forEach { char ->
            typeCharacter(char)
            Thread.sleep(20)
        }
    }

    private fun typeCharacter(char: Char) {
        val vkCode = char.code.toShort()
        user32.keybd_event(0.toByte(), vkCode, KEYEVENTF_UNICODE, 0)
        Thread.sleep(10)
        user32.keybd_event(0.toByte(), vkCode, (KEYEVENTF_UNICODE or KEYEVENTF_KEYUP), 0)
    }

    fun pressKeys(keys: List<Key>) {
        val vkCodes = keys.map { VirtualKeyCodes.getKeyCode(it) }

        vkCodes.forEach { vkCode ->
            user32.keybd_event(vkCode.toByte(), 0, 0, 0)
            Thread.sleep(20)
        }

        Thread.sleep(50)

        vkCodes.reversed().forEach { vkCode ->
            user32.keybd_event(vkCode.toByte(), 0, KEYEVENTF_KEYUP, 0)
            Thread.sleep(20)
        }
    }
}