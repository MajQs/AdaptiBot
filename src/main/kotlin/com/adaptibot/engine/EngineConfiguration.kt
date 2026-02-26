package com.adaptibot.engine

import com.adaptibot.action.ActionConfiguration
import com.adaptibot.engine.domain.*
import com.adaptibot.engine.domain.observer.ObserverInterruptCoordinator
import com.adaptibot.engine.domain.observer.ObserverRegistry
import com.adaptibot.ui.adapter.UiExecutionEventPublisher
import com.adaptibot.vision.VisionConfiguration

object EngineConfiguration {
    fun getFacade(): EngineFacade {
        val conditionEvaluator = VisionConfiguration.getConditionEvaluator()
        val eventPublisher = UiExecutionEventPublisher()
        val scriptExecutionState = ScriptExecutionState(eventPublisher)

        return EngineFacade(
            scriptRunner = ScriptRunner(
                scriptExecutionState = scriptExecutionState,
                scriptInterpreter = ScriptInterpreter(
                    actionStepHandler = ActionStepHandler(
                        actionFacade = ActionConfiguration.getActionFacade(),
                        eventPublisher = eventPublisher
                    ),
                    blockStepResolver = BlockStepResolver(conditionEvaluator),
                    observerRegistry = ObserverRegistry(conditionEvaluator, 1000),
                    scriptExecutionState = scriptExecutionState,
                    observerInterruptCoordinator = ObserverInterruptCoordinator()
                ),
            )
        )
    }
}
