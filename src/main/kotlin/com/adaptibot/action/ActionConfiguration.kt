package com.adaptibot.action

import com.adaptibot.action.domain.ActionExecutor
import com.adaptibot.engine.domain.actions.ElementFinder

object ActionConfiguration {

    fun getActionFacade(): ActionFacade =
        ActionFacade(
            actionExecutor = ActionExecutor(
                elementFinder = ElementFinder()
            )
        )
}