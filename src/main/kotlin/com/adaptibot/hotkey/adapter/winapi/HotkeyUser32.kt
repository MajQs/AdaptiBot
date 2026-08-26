package com.adaptibot.hotkey.adapter.winapi

import com.sun.jna.Native
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions

internal interface HotkeyUser32 : StdCallLibrary {

    fun RegisterHotKey(hWnd: WinDef.HWND?, id: Int, fsModifiers: Int, vk: Int): Boolean

    fun UnregisterHotKey(hWnd: WinDef.HWND?, id: Int): Boolean

    /** @return >0 for a regular message, 0 for WM_QUIT, -1 on error */
    fun GetMessage(lpMsg: WinUser.MSG, hWnd: WinDef.HWND?, wMsgFilterMin: Int, wMsgFilterMax: Int): Int

    fun PostThreadMessage(idThread: Int, uMsg: Int, wParam: WinDef.WPARAM?, lParam: WinDef.LPARAM?): Boolean

    companion object {

        const val WM_HOTKEY = 0x0312
        const val WM_QUIT = 0x0012

        val INSTANCE: HotkeyUser32 by lazy {
            Native.load("user32", HotkeyUser32::class.java, W32APIOptions.DEFAULT_OPTIONS)
        }
    }
}

