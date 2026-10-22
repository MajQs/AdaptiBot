package com.adaptibot.recorder.adapter.winapi

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.win32.BaseTSD
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions

internal interface InputCaptureUser32 : StdCallLibrary {

    fun SetWindowsHookExW(
        idHook: Int,
        callback: LowLevelMouseProc,
        module: WinDef.HINSTANCE?,
        threadId: Int
    ): WinUser.HHOOK?

    fun UnhookWindowsHookEx(hook: WinUser.HHOOK): Boolean

    fun CallNextHookEx(
        hook: WinUser.HHOOK?,
        code: Int,
        wParam: WinDef.WPARAM,
        lParam: WinDef.LPARAM
    ): WinDef.LRESULT

    /** @return >0 for a regular message, 0 for WM_QUIT, -1 on error */
    fun GetMessage(message: WinUser.MSG, window: WinDef.HWND?, filterMin: Int, filterMax: Int): Int

    fun PeekMessage(
        message: WinUser.MSG,
        window: WinDef.HWND?,
        filterMin: Int,
        filterMax: Int,
        removeMessage: Int
    ): Boolean

    fun PostThreadMessage(
        threadId: Int,
        message: Int,
        wParam: WinDef.WPARAM?,
        lParam: WinDef.LPARAM?
    ): Boolean

    fun interface LowLevelMouseProc : StdCallLibrary.StdCallCallback {
        fun callback(code: Int, wParam: WinDef.WPARAM, lParam: WinDef.LPARAM): WinDef.LRESULT
    }

    @Structure.FieldOrder("pt", "mouseData", "flags", "time", "dwExtraInfo")
    class MSLLHOOKSTRUCT(pointer: Pointer? = null) : Structure(pointer) {
        @JvmField var pt: WinDef.POINT = WinDef.POINT()
        @JvmField var mouseData: WinDef.DWORD = WinDef.DWORD(0)
        @JvmField var flags: WinDef.DWORD = WinDef.DWORD(0)
        @JvmField var time: WinDef.DWORD = WinDef.DWORD(0)
        @JvmField var dwExtraInfo: BaseTSD.ULONG_PTR = BaseTSD.ULONG_PTR(0)
    }

    companion object {
        const val WH_MOUSE_LL = 14
        const val WM_LBUTTONDOWN = 0x0201
        const val WM_QUIT = 0x0012
        const val PM_NOREMOVE = 0x0000
        const val LLMHF_INJECTED = 0x00000001
        const val LLMHF_LOWER_IL_INJECTED = 0x00000002

        val INSTANCE: InputCaptureUser32 by lazy {
            Native.load("user32", InputCaptureUser32::class.java, W32APIOptions.DEFAULT_OPTIONS)
        }
    }
}
