package com.adaptibot.action

import com.adaptibot.action.domain.ActionExecutor
import com.adaptibot.common.model.Action

class ActionFacade internal constructor(
    private val actionExecutor: ActionExecutor
) {

    fun execute(action: Action) {
        actionExecutor.execute(action)
    }
}