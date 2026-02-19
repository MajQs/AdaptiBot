package com.adaptibot.engine

import com.adaptibot.engine.domain.*
import com.adaptibot.engine.domain.actions.ConditionEvaluator
import com.adaptibot.engine.domain.actions.ElementFinder
import com.adaptibot.engine.domain.observer.ObserverInterruptCoordinator
import com.adaptibot.engine.domain.observer.ObserverRegistry
import com.adaptibot.engine.domain.actions.ActionExecutor as ActionExecutorImpl
import com.adaptibot.ui.adapter.UiExecutionEventPublisher

object EngineConfiguration {
    fun getFacade(): EngineFacade {
        val elementFinder = ElementFinder()
        val conditionEvaluator = ConditionEvaluator(elementFinder)
        val eventPublisher = UiExecutionEventPublisher()
        val scriptExecutionState = ScriptExecutionState(eventPublisher)
        val observerRegistry = ObserverRegistry(conditionEvaluator, 1000)

        return EngineFacade(
            scriptRunner = ScriptRunner(
                scriptExecutionState = scriptExecutionState,
                scriptInterpreter = ScriptInterpreter(
                    actionStepHandler = ActionStepHandler(
                        actionExecutor = ActionExecutorImpl(),
                        elementFinder = elementFinder,
                        eventPublisher = eventPublisher
                    ),
                    blockStepResolver = BlockStepResolver(conditionEvaluator),
                    observerRegistry = observerRegistry,
                    scriptExecutionState = scriptExecutionState,
                    observerInterruptCoordinator = ObserverInterruptCoordinator()
                ),
            )
        )
    }
}
