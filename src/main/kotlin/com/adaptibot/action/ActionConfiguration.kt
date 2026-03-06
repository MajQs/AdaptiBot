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
                Action.Mouse::class.java to MouseActionHandler(VisionConfiguration.getVisionFacade()),
                Action.Keyboard::class.java to KeyboardActionHandler(),
                Action.System::class.java to SystemActionHandler()
            ) as Map<Class<out Action>, ActionHandler<in Action>>
        )
}