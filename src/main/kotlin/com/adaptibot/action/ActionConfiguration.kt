package com.adaptibot.action

import com.adaptibot.action.adapter.robot.RobotInputController
import com.adaptibot.action.adapter.unsupported.UnsupportedKeyboardActionHandler
import com.adaptibot.action.adapter.unsupported.UnsupportedMouseActionHandler
import com.adaptibot.action.domain.ActionHandler
import com.adaptibot.action.domain.InputController
import com.adaptibot.action.domain.KeyboardActionHandler
import com.adaptibot.action.domain.MouseActionHandler
import com.adaptibot.action.domain.SystemActionHandler
import com.adaptibot.action.domain.TargetCoordinateResolver
import com.adaptibot.infrastructure.PlatformCapabilities
import com.adaptibot.infrastructure.PlatformCapabilityDetector
import com.adaptibot.script.value.Action
import com.adaptibot.vision.VisionConfiguration

object ActionConfiguration {

    fun getActionFacade(): ActionFacade = getActionFacade(PlatformCapabilityDetector.detect())

    @Suppress("UNCHECKED_CAST")
    internal fun getActionFacade(capabilities: PlatformCapabilities): ActionFacade {
        val inputController = if (capabilities.actionSupported) RobotInputController() else null
        val nativeActionHandlers = inputController?.let(::createRobotHandlers) ?: createUnsupportedHandlers()

        return ActionFacade(
            actionHandlerPerType = (nativeActionHandlers +
                (Action.System::class.java to SystemActionHandler())) as Map<Class<out Action>, ActionHandler<in Action>>,
            releaseAllInputs = inputController?.let { it::releaseAllInputs } ?: {}
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createRobotHandlers(
        inputController: InputController
    ): Map<Class<out Action>, ActionHandler<in Action>> = mapOf(
            Action.Mouse::class.java to MouseActionHandler(
                targetCoordinateResolver = TargetCoordinateResolver(
                    visionFacade = VisionConfiguration.visionFacade
                ),
                inputController = inputController
            ),
            Action.Keyboard::class.java to KeyboardActionHandler(inputController)
        ) as Map<Class<out Action>, ActionHandler<in Action>>

    @Suppress("UNCHECKED_CAST")
    private fun createUnsupportedHandlers(): Map<Class<out Action>, ActionHandler<in Action>> = mapOf(
        Action.Mouse::class.java to UnsupportedMouseActionHandler(),
        Action.Keyboard::class.java to UnsupportedKeyboardActionHandler()
    ) as Map<Class<out Action>, ActionHandler<in Action>>
}
