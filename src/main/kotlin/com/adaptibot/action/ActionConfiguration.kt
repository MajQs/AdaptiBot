package com.adaptibot.action

import com.adaptibot.action.domain.ActionHandler
import com.adaptibot.action.domain.KeyboardActionHandler
import com.adaptibot.action.domain.MouseActionHandler
import com.adaptibot.action.domain.SystemActionHandler
import com.adaptibot.model.Action
import com.adaptibot.vision.VisionConfiguration

object ActionConfiguration {

    @Suppress("UNCHECKED_CAST")
    fun getActionFacade(): ActionFacade =
        ActionFacade(
            actionHandlerPerType = mapOf(
                Action.Mouse to MouseActionHandler(VisionConfiguration.getVisionFacade()),
                Action.Keyboard to KeyboardActionHandler(),
                Action.System to SystemActionHandler()
            ) as Map<Class<out Action>, ActionHandler<in Action>>
        )
}