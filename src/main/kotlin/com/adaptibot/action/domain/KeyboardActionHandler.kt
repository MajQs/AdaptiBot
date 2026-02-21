package com.adaptibot.action.domain

import com.adaptibot.action.adapter.KeyboardController
import com.adaptibot.common.model.Action.Keyboard
import com.adaptibot.common.model.Action.Keyboard.*

internal class KeyboardActionHandler : ActionHandler<Keyboard> {

    override fun handle(action: Keyboard) {
        try {
            when (action) {
                is TypeText -> {
                    KeyboardController.typeText(action.text)
                }

                is PressKey -> {
                    KeyboardController.pressKey(action.key)
                }

                is PressKeyCombination -> {
                    KeyboardController.pressKeyCombination(action.keys)
                }
            }
        } catch (e: Exception) {
        }
    }
}