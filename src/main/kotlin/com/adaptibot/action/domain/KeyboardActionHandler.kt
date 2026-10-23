package com.adaptibot.action.domain

import com.adaptibot.action.ActionExecutionException
import com.adaptibot.script.value.Action.Keyboard

internal class KeyboardActionHandler(
    private val inputController: InputController,
) : ActionHandler<Keyboard> {

    override fun handle(action: Keyboard) {
        when (action) {
            is Keyboard.TypeText -> inputController.typeText(action.text)
            is Keyboard.PressKeys -> {
                if (action.keys.isEmpty()) throw ActionExecutionException.EmptyKeyList()
                inputController.pressKeys(action.keys)
            }
        }
    }
}
