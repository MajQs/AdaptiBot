package com.adaptibot.action

import com.adaptibot.action.domain.*
import com.adaptibot.model.Action
import com.adaptibot.vision.VisionConfiguration

object ActionConfiguration {

    @Suppress("UNCHECKED_CAST")
    fun getActionFacade(): ActionFacade = ActionFacade(
        actionHandlerPerType = mapOf(
            Action.Mouse::class.java to MouseActionHandler(
                targetCoordinateResolver = TargetCoordinateResolver(
                    visionFacade = VisionConfiguration.getVisionFacade()
                )
            ),
            Action.Keyboard::class.java to KeyboardActionHandler(),
            Action.System::class.java to SystemActionHandler()
        ) as Map<Class<out Action>, ActionHandler<in Action>>
    )
}