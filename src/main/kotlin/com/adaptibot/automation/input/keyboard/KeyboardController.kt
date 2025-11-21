package com.adaptibot.automation.input.keyboard

import com.adaptibot.automation.winapi.User32
import com.adaptibot.automation.winapi.VirtualKeyCodes
import org.slf4j.LoggerFactory

object KeyboardController {
    
    private val logger = LoggerFactory.getLogger(KeyboardController::class.java)
    private val user32 = User32.INSTANCE
    
    // Key event flags
    private const val KEYEVENTF_KEYUP = 0x0002
    private const val KEYEVENTF_UNICODE = 0x0004
    
    fun typeText(text: String): Boolean {
        return try {
            text.forEach { char ->
                typeCharacter(char)
                Thread.sleep(20) // Small delay between characters
            }
            true
        } catch (e: Exception) {
            logger.error("Failed to type text: $text", e)
            false
        }
    }
    
    private fun typeCharacter(char: Char) {
        val vkCode = char.code.toShort()
        
        // Use Unicode input for non-ASCII characters
        user32.keybd_event(0.toByte(), vkCode, KEYEVENTF_UNICODE, 0)
        Thread.sleep(10)
        user32.keybd_event(0.toByte(), vkCode, (KEYEVENTF_UNICODE or KEYEVENTF_KEYUP), 0)
    }
    
    fun pressKey(keyName: String): Boolean {
        return try {
            val vkCode = VirtualKeyCodes.getKeyCode(keyName)
            if (vkCode == null) {
                logger.error("Unknown key name: $keyName")
                return false
            }
            
            user32.keybd_event(vkCode.toByte(), 0, 0, 0)
            Thread.sleep(50)
            user32.keybd_event(vkCode.toByte(), 0, KEYEVENTF_KEYUP, 0)
            true
        } catch (e: Exception) {
            logger.error("Failed to press key: $keyName", e)
            false
        }
    }
    
    fun pressKeyCombination(keys: List<String>): Boolean {
        if (keys.isEmpty()) {
            logger.error("Key combination is empty")
            return false
        }
        
        return try {
            // Convert all keys to virtual key codes
            val vkCodes = keys.mapNotNull { keyName ->
                VirtualKeyCodes.getKeyCode(keyName).also {
                    if (it == null) logger.error("Unknown key in combination: $keyName")
                }
            }
            
            if (vkCodes.size != keys.size) {
                logger.error("Failed to resolve all keys in combination: $keys")
                return false
            }
            
            // Press all keys down
            vkCodes.forEach { vkCode ->
                user32.keybd_event(vkCode.toByte(), 0, 0, 0)
                Thread.sleep(20)
            }
            
            Thread.sleep(50)
            
            // Release all keys in reverse order
            vkCodes.reversed().forEach { vkCode ->
                user32.keybd_event(vkCode.toByte(), 0, KEYEVENTF_KEYUP, 0)
                Thread.sleep(20)
            }
            
            true
        } catch (e: Exception) {
            logger.error("Failed to press key combination: $keys", e)
            false
        }
    }
}

