package com.adaptibot.action.adapter.winapi

import com.adaptibot.common.model.Key

object VirtualKeyCodes {

    fun getKeyCode(key: Key): Int = when (key) {
        Key.A -> 0x41;  Key.B -> 0x42;  Key.C -> 0x43;  Key.D -> 0x44
        Key.E -> 0x45;  Key.F -> 0x46;  Key.G -> 0x47;  Key.H -> 0x48
        Key.I -> 0x49;  Key.J -> 0x4A;  Key.K -> 0x4B;  Key.L -> 0x4C
        Key.M -> 0x4D;  Key.N -> 0x4E;  Key.O -> 0x4F;  Key.P -> 0x50
        Key.Q -> 0x51;  Key.R -> 0x52;  Key.S -> 0x53;  Key.T -> 0x54
        Key.U -> 0x55;  Key.V -> 0x56;  Key.W -> 0x57;  Key.X -> 0x58
        Key.Y -> 0x59;  Key.Z -> 0x5A

        Key.DIGIT_0 -> 0x30;  Key.DIGIT_1 -> 0x31;  Key.DIGIT_2 -> 0x32
        Key.DIGIT_3 -> 0x33;  Key.DIGIT_4 -> 0x34;  Key.DIGIT_5 -> 0x35
        Key.DIGIT_6 -> 0x36;  Key.DIGIT_7 -> 0x37;  Key.DIGIT_8 -> 0x38
        Key.DIGIT_9 -> 0x39

        Key.F1  -> 0x70;  Key.F2  -> 0x71;  Key.F3  -> 0x72;  Key.F4  -> 0x73
        Key.F5  -> 0x74;  Key.F6  -> 0x75;  Key.F7  -> 0x76;  Key.F8  -> 0x77
        Key.F9  -> 0x78;  Key.F10 -> 0x79;  Key.F11 -> 0x7A;  Key.F12 -> 0x7B

        Key.CTRL      -> 0x11
        Key.SHIFT     -> 0x10
        Key.ALT       -> 0x12
        Key.WIN       -> 0x5B

        Key.ENTER     -> 0x0D
        Key.ESCAPE    -> 0x1B
        Key.TAB       -> 0x09
        Key.SPACE     -> 0x20
        Key.BACKSPACE -> 0x08
        Key.CAPS_LOCK -> 0x14
        Key.PAUSE     -> 0x13

        Key.HOME        -> 0x24
        Key.END         -> 0x23
        Key.PAGE_UP     -> 0x21
        Key.PAGE_DOWN   -> 0x22

        Key.ARROW_LEFT  -> 0x25
        Key.ARROW_UP    -> 0x26
        Key.ARROW_RIGHT -> 0x27
        Key.ARROW_DOWN  -> 0x28

        Key.INSERT -> 0x2D
        Key.DELETE -> 0x2E
    }
}
