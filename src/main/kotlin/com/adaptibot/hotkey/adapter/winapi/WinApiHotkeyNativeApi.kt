package com.adaptibot.hotkey.adapter.winapi

import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import org.slf4j.LoggerFactory

internal class WinApiHotkeyNativeApi(
    private val user32: HotkeyUser32 = HotkeyUser32.INSTANCE,
    private val kernel32: Kernel32 = Kernel32.INSTANCE
) {

    private val logger = LoggerFactory.getLogger(WinApiHotkeyNativeApi::class.java)

    fun registerHotKey(id: Int, modifierMask: Int, virtualKeyCode: Int): Boolean =
        user32.RegisterHotKey(null, id, modifierMask, virtualKeyCode)

    fun unregisterHotKey(id: Int): Boolean = user32.UnregisterHotKey(null, id)

    fun currentThreadId(): Int = kernel32.GetCurrentThreadId()

    /** @return id of the triggered hotkey, or null when the message loop should terminate */
    fun waitForNextHotkey(): Int? {
        val message = WinUser.MSG()
        while (true) {
            when (val result = user32.GetMessage(message, null, 0, 0)) {
                0 -> return null
                -1 -> {
                    logger.warn("GetMessage failed with result $result, terminating hotkey loop")
                    return null
                }
                else -> if (message.message == HotkeyUser32.WM_HOTKEY) return message.wParam.toInt()
            }
        }
    }

    fun requestLoopExit(threadId: Int) {
        user32.PostThreadMessage(threadId, HotkeyUser32.WM_QUIT, WinDef.WPARAM(0), WinDef.LPARAM(0))
    }
}

