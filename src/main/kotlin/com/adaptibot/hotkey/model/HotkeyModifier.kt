package com.adaptibot.hotkey.model

/**
 * Keyboard modifier of a global shortcut, mapped to the WinAPI `MOD_*` mask.
 */
enum class HotkeyModifier(internal val mask: Int, private val displayName: String) {
    ALT(0x0001, "Alt"),
    CTRL(0x0002, "Ctrl"),
    SHIFT(0x0004, "Shift"),
    WIN(0x0008, "Win");

    override fun toString(): String = displayName
}

