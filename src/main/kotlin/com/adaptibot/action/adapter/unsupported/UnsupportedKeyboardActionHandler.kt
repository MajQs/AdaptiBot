package com.adaptibot.action.adapter.unsupported

import com.adaptibot.action.ActionExecutionException
import com.adaptibot.action.domain.ActionHandler
import com.adaptibot.script.value.Action.Keyboard

internal class UnsupportedKeyboardActionHandler : ActionHandler<Keyboard> {
    override fun handle(action: Keyboard): Nothing = throw ActionExecutionException.UnsupportedPlatform(
        ActionExecutionException.NATIVE_ACTIONS_UNSUPPORTED_MESSAGE
    )
}
