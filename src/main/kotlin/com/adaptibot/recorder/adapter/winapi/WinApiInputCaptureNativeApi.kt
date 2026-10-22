package com.adaptibot.recorder.adapter.winapi

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser

internal class WinApiInputCaptureNativeApi(
    private val user32: InputCaptureUser32 = InputCaptureUser32.INSTANCE,
    private val kernel32: Kernel32 = Kernel32.INSTANCE
) {
    fun installMouseHook(callback: InputCaptureUser32.LowLevelMouseProc): WinUser.HHOOK? =
        user32.SetWindowsHookExW(InputCaptureUser32.WH_MOUSE_LL, callback, null, 0)

    fun removeMouseHook(hook: WinUser.HHOOK): Boolean = user32.UnhookWindowsHookEx(hook)

    fun callNextHook(
        hook: WinUser.HHOOK?,
        code: Int,
        wParam: WinDef.WPARAM,
        lParam: WinDef.LPARAM
    ): WinDef.LRESULT = user32.CallNextHookEx(hook, code, wParam, lParam)

    fun readMouseHookData(lParam: WinDef.LPARAM): InputCaptureUser32.MSLLHOOKSTRUCT =
        InputCaptureUser32.MSLLHOOKSTRUCT(Pointer(lParam.toLong())).apply { read() }

    fun currentThreadId(): Int = kernel32.GetCurrentThreadId()

    /** Ensures PostThreadMessage has a queue to target before start reports success. */
    fun createMessageQueue() {
        user32.PeekMessage(WinUser.MSG(), null, 0, 0, InputCaptureUser32.PM_NOREMOVE)
    }

    /** @return true for a regular message, false for WM_QUIT; throws when GetMessage fails. */
    fun waitForNextMessage(): Boolean {
        val result = user32.GetMessage(WinUser.MSG(), null, 0, 0)
        if (result == -1) throw IllegalStateException("GetMessage failed")
        return result != 0
    }

    fun requestLoopExit(threadId: Int): Boolean =
        user32.PostThreadMessage(
            threadId,
            InputCaptureUser32.WM_QUIT,
            WinDef.WPARAM(0),
            WinDef.LPARAM(0)
        )
}
