package com.adaptibot.automation.winapi

object VirtualKeyCodes {
    // Letter keys
    const val VK_A = 0x41
    const val VK_Z = 0x5A
    
    // Number keys
    const val VK_0 = 0x30
    const val VK_9 = 0x39
    
    // Function keys
    const val VK_F1 = 0x70
    const val VK_F12 = 0x7B
    
    // Control keys
    const val VK_BACK = 0x08
    const val VK_TAB = 0x09
    const val VK_RETURN = 0x0D
    const val VK_SHIFT = 0x10
    const val VK_CONTROL = 0x11
    const val VK_MENU = 0x12  // ALT key
    const val VK_PAUSE = 0x13
    const val VK_CAPITAL = 0x14  // CAPS LOCK
    const val VK_ESCAPE = 0x1B
    const val VK_SPACE = 0x20
    
    // Navigation keys
    const val VK_PRIOR = 0x21  // PAGE UP
    const val VK_NEXT = 0x22   // PAGE DOWN
    const val VK_END = 0x23
    const val VK_HOME = 0x24
    const val VK_LEFT = 0x25
    const val VK_UP = 0x26
    const val VK_RIGHT = 0x27
    const val VK_DOWN = 0x28
    
    // Edit keys
    const val VK_INSERT = 0x2D
    const val VK_DELETE = 0x2E
    
    // Windows keys
    const val VK_LWIN = 0x5B
    const val VK_RWIN = 0x5C
    
    // Numpad keys
    const val VK_NUMPAD0 = 0x60
    const val VK_NUMPAD9 = 0x69
    
    // Key mapping
    val keyMap = mapOf(
        "backspace" to VK_BACK,
        "tab" to VK_TAB,
        "enter" to VK_RETURN,
        "shift" to VK_SHIFT,
        "ctrl" to VK_CONTROL,
        "control" to VK_CONTROL,
        "alt" to VK_MENU,
        "pause" to VK_PAUSE,
        "capslock" to VK_CAPITAL,
        "escape" to VK_ESCAPE,
        "esc" to VK_ESCAPE,
        "space" to VK_SPACE,
        "pageup" to VK_PRIOR,
        "pagedown" to VK_NEXT,
        "end" to VK_END,
        "home" to VK_HOME,
        "left" to VK_LEFT,
        "up" to VK_UP,
        "right" to VK_RIGHT,
        "down" to VK_DOWN,
        "insert" to VK_INSERT,
        "delete" to VK_DELETE,
        "win" to VK_LWIN,
        "windows" to VK_LWIN
    )
    
    fun getKeyCode(keyName: String): Int? {
        val normalized = keyName.lowercase().trim()
        
        // Check special keys first
        keyMap[normalized]?.let { return it }
        
        // Function keys (F1-F12)
        if (normalized.startsWith("f") && normalized.length >= 2) {
            val num = normalized.substring(1).toIntOrNull()
            if (num != null && num in 1..12) {
                return VK_F1 + (num - 1)
            }
        }
        
        // Single letter (A-Z)
        if (normalized.length == 1) {
            val char = normalized[0]
            if (char in 'a'..'z') {
                return VK_A + (char - 'a')
            }
            if (char in '0'..'9') {
                return VK_0 + (char - '0')
            }
        }
        
        return null
    }
}

