package com.adaptibot.action

import com.adaptibot.action.domain.*
import com.adaptibot.script.Action
import com.adaptibot.vision.VisionConfiguration

object ActionConfiguration {

    @Suppress("UNCHECKED_CAST")
    fun getActionFacade(): ActionFacade = ActionFacade(
        actionHandlerPerType = mapOf(
            Action.Mouse::class.java to MouseActionHandler(
                targetCoordinateResolver = TargetCoordinateResolver(
                    visionFacade = VisionConfiguration.visionFacade
                )
            ),
            Action.Keyboard::class.java to KeyboardActionHandler(),
            Action.System::class.java to SystemActionHandler()
        ) as Map<Class<out Action>, ActionHandler<in Action>>
    )
}