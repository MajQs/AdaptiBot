package com.adaptibot.action

import com.adaptibot.action.domain.ActionHandler
import com.adaptibot.action.domain.FlowActionHandler
import com.adaptibot.action.domain.KeyboardActionHandler
import com.adaptibot.action.domain.MouseActionHandler
import com.adaptibot.action.domain.SystemActionHandler
import com.adaptibot.common.model.Action
import com.adaptibot.vision.ElementFinder

object ActionConfiguration {

    @Suppress("UNCHECKED_CAST")
    fun getActionFacade(): ActionFacade =
        ActionFacade(
            actionHandlerPerType = mapOf(
                Action.Mouse to MouseActionHandler(ElementFinder()),
                Action.Keyboard to KeyboardActionHandler(),
                Action.System to SystemActionHandler(),
                Action.Flow to FlowActionHandler()
            ) as Map<Class<out Action>, ActionHandler<in Action>>
        )
}