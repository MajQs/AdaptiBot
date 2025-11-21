package com.adaptibot.automation.winapi

import com.sun.jna.Native
import com.sun.jna.Structure
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.win32.StdCallLibrary

interface User32 : StdCallLibrary {
    
    fun SetCursorPos(x: Int, y: Int): Boolean
    
    fun GetCursorPos(lpPoint: POINT): Boolean
    
    fun mouse_event(dwFlags: Int, dx: Int, dy: Int, dwData: Int, dwExtraInfo: Int)
    
    companion object {
        val INSTANCE: User32 = Native.load("user32", User32::class.java)
        
        // Mouse event flags
        const val MOUSEEVENTF_MOVE = 0x0001
        const val MOUSEEVENTF_LEFTDOWN = 0x0002
        const val MOUSEEVENTF_LEFTUP = 0x0004
        const val MOUSEEVENTF_RIGHTDOWN = 0x0008
        const val MOUSEEVENTF_RIGHTUP = 0x0010
        const val MOUSEEVENTF_MIDDLEDOWN = 0x0020
        const val MOUSEEVENTF_MIDDLEUP = 0x0040
        const val MOUSEEVENTF_WHEEL = 0x0800
        const val MOUSEEVENTF_ABSOLUTE = 0x8000
        
        // Wheel delta
        const val WHEEL_DELTA = 120
    }
    
    @Structure.FieldOrder("x", "y")
    class POINT : Structure() {
        @JvmField var x: Int = 0
        @JvmField var y: Int = 0
    }
}

