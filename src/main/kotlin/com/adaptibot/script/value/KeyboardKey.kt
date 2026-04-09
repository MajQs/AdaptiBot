package com.adaptibot.script.value

import kotlinx.serialization.Serializable

@Serializable
enum class KeyboardKey {
    // Letters
    A, B, C, D, E, F, G, H, I, J, K, L, M,
    N, O, P, Q, R, S, T, U, V, W, X, Y, Z,

    // Digits
    DIGIT_0, DIGIT_1, DIGIT_2, DIGIT_3, DIGIT_4,
    DIGIT_5, DIGIT_6, DIGIT_7, DIGIT_8, DIGIT_9,

    // Function keys
    F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12,

    // Modifier keys
    CTRL, SHIFT, ALT, WIN,

    // Control keys
    ENTER, ESCAPE, TAB, SPACE, BACKSPACE, CAPS_LOCK, PAUSE,

    // Navigation keys
    HOME, END, PAGE_UP, PAGE_DOWN,
    ARROW_LEFT, ARROW_UP, ARROW_RIGHT, ARROW_DOWN,

    // Edit keys
    INSERT, DELETE
}

