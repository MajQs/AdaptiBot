package com.adaptibot.execution

import com.adaptibot.action.ActionConfiguration
import com.adaptibot.execution.domain.*
import com.adaptibot.execution.domain.observer.ObserverInterruptCoordinator
import com.adaptibot.execution.domain.observer.ObserverRegistry
import com.adaptibot.ui.adapter.UiExecutionEventPublisher
import com.adaptibot.vision.VisionConfiguration

object ExecutionConfiguration {
    fun getFacade(): ExecutionFacade {
        val conditionEvaluator = VisionConfiguration.getConditionEvaluator()
        val eventPublisher = UiExecutionEventPublisher()
        val scriptExecutionState = ScriptExecutionState(eventPublisher)

        return ExecutionFacade(
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
