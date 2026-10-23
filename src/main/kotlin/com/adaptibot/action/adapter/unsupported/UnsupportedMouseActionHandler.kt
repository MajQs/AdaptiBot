package com.adaptibot.action.adapter.unsupported

import com.adaptibot.action.ActionExecutionException
import com.adaptibot.action.domain.ActionHandler
import com.adaptibot.script.value.Action.Mouse

internal class UnsupportedMouseActionHandler : ActionHandler<Mouse> {
    override fun handle(action: Mouse): Nothing = throw ActionExecutionException.UnsupportedPlatform(
        ActionExecutionException.NATIVE_ACTIONS_UNSUPPORTED_MESSAGE
    )
}
