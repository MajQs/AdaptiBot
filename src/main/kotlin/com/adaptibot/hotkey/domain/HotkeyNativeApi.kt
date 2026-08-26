package com.adaptibot.hotkey.domain

internal interface HotkeyNativeApi {

    fun registerHotKey(id: Int, modifierMask: Int, virtualKeyCode: Int): Boolean

    fun unregisterHotKey(id: Int): Boolean

    fun currentThreadId(): Int

    /** @return id of the triggered hotkey, or null when the message loop should terminate */
    fun waitForNextHotkey(): Int?

    fun requestLoopExit(threadId: Int)
}

