package com.adaptibot.action.adapter

import com.adaptibot.action.adapter.winapi.VirtualKeyCodes
import com.adaptibot.infrastructure.InterruptibleSleep
import com.adaptibot.script.value.KeyboardKey

internal object KeyboardController {

    private const val TYPE_CHARACTER_HOLD_MS = 10L
    private const val TYPE_CHARACTER_GAP_MS = 20L
    private const val KEY_PRESS_GAP_MS = 20L
    private const val COMBINATION_HOLD_MS = 50L

    fun typeText(text: String) {
        text.forEach { char ->
            InputStateTracker.holdingCharacter(char) {
                InterruptibleSleep.sleep(TYPE_CHARACTER_HOLD_MS)
            }
            InterruptibleSleep.sleep(TYPE_CHARACTER_GAP_MS)
        }
    }

    fun pressKeys(keys: List<KeyboardKey>) {
        val virtualKeyCodes = keys.map { VirtualKeyCodes.getKeyCode(it) }

        InputStateTracker.holdingKeys(
            virtualKeyCodes = virtualKeyCodes,
            afterEachPress = { InterruptibleSleep.sleep(KEY_PRESS_GAP_MS) }
        ) {
            InterruptibleSleep.sleep(COMBINATION_HOLD_MS)
        }
    }
}

