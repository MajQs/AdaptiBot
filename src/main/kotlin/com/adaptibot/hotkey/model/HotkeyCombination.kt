package com.adaptibot.hotkey.model

data class HotkeyCombination(
    val modifiers: Set<HotkeyModifier>,
    val virtualKeyCode: Int,
    val keyName: String
) {

    internal val modifierMask: Int
        get() = modifiers.fold(MOD_NOREPEAT) { mask, modifier -> mask or modifier.mask }

    val displayName: String
        get() = (modifiers.sortedBy { it.ordinal }.map { it.toString() } + keyName).joinToString("+")

    override fun toString(): String = displayName

    companion object {

        private const val MOD_NOREPEAT = 0x4000
        private const val VK_F12 = 0x7B
        
        val STOP_EXECUTION = HotkeyCombination(
            modifiers = setOf(HotkeyModifier.CTRL, HotkeyModifier.SHIFT),
            virtualKeyCode = VK_F12,
            keyName = "F12"
        )
    }
}

