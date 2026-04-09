package com.adaptibot.action.domain

import com.adaptibot.action.ActionExecutionException
import com.adaptibot.action.adapter.KeyboardController
import com.adaptibot.script.value.Action.Keyboard

internal class KeyboardActionHandler : ActionHandler<Keyboard> {

    override fun handle(action: Keyboard) {
        when (action) {
            is Keyboard.TypeText -> KeyboardController.typeText(action.text)
            is Keyboard.PressKeys -> {
                if (action.keys.isEmpty()) throw ActionExecutionException.EmptyKeyList()
                KeyboardController.pressKeys(action.keys)
            }
        }
    }
}