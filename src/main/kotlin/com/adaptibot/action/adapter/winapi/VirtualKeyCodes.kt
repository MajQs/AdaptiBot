package com.adaptibot.action.adapter.winapi

import com.adaptibot.script.value.KeyboardKey

internal object VirtualKeyCodes {

    fun getKeyCode(key: KeyboardKey): Int = when (key) {
        KeyboardKey.A -> 0x41;  KeyboardKey.B -> 0x42;  KeyboardKey.C -> 0x43;  KeyboardKey.D -> 0x44
        KeyboardKey.E -> 0x45;  KeyboardKey.F -> 0x46;  KeyboardKey.G -> 0x47;  KeyboardKey.H -> 0x48
        KeyboardKey.I -> 0x49;  KeyboardKey.J -> 0x4A;  KeyboardKey.K -> 0x4B;  KeyboardKey.L -> 0x4C
        KeyboardKey.M -> 0x4D;  KeyboardKey.N -> 0x4E;  KeyboardKey.O -> 0x4F;  KeyboardKey.P -> 0x50
        KeyboardKey.Q -> 0x51;  KeyboardKey.R -> 0x52;  KeyboardKey.S -> 0x53;  KeyboardKey.T -> 0x54
        KeyboardKey.U -> 0x55;  KeyboardKey.V -> 0x56;  KeyboardKey.W -> 0x57;  KeyboardKey.X -> 0x58
        KeyboardKey.Y -> 0x59;  KeyboardKey.Z -> 0x5A

        KeyboardKey.DIGIT_0 -> 0x30;  KeyboardKey.DIGIT_1 -> 0x31;  KeyboardKey.DIGIT_2 -> 0x32
        KeyboardKey.DIGIT_3 -> 0x33;  KeyboardKey.DIGIT_4 -> 0x34;  KeyboardKey.DIGIT_5 -> 0x35
        KeyboardKey.DIGIT_6 -> 0x36;  KeyboardKey.DIGIT_7 -> 0x37;  KeyboardKey.DIGIT_8 -> 0x38
        KeyboardKey.DIGIT_9 -> 0x39

        KeyboardKey.F1  -> 0x70;  KeyboardKey.F2  -> 0x71;  KeyboardKey.F3  -> 0x72;  KeyboardKey.F4  -> 0x73
        KeyboardKey.F5  -> 0x74;  KeyboardKey.F6  -> 0x75;  KeyboardKey.F7  -> 0x76;  KeyboardKey.F8  -> 0x77
        KeyboardKey.F9  -> 0x78;  KeyboardKey.F10 -> 0x79;  KeyboardKey.F11 -> 0x7A;  KeyboardKey.F12 -> 0x7B

        KeyboardKey.CTRL      -> 0x11
        KeyboardKey.SHIFT     -> 0x10
        KeyboardKey.ALT       -> 0x12
        KeyboardKey.WIN       -> 0x5B

        KeyboardKey.ENTER     -> 0x0D
        KeyboardKey.ESCAPE    -> 0x1B
        KeyboardKey.TAB       -> 0x09
        KeyboardKey.SPACE     -> 0x20
        KeyboardKey.BACKSPACE -> 0x08
        KeyboardKey.CAPS_LOCK -> 0x14
        KeyboardKey.PAUSE     -> 0x13

        KeyboardKey.HOME        -> 0x24
        KeyboardKey.END         -> 0x23
        KeyboardKey.PAGE_UP     -> 0x21
        KeyboardKey.PAGE_DOWN   -> 0x22

        KeyboardKey.ARROW_LEFT  -> 0x25
        KeyboardKey.ARROW_UP    -> 0x26
        KeyboardKey.ARROW_RIGHT -> 0x27
        KeyboardKey.ARROW_DOWN  -> 0x28

        KeyboardKey.INSERT -> 0x2D
        KeyboardKey.DELETE -> 0x2E
    }
}
