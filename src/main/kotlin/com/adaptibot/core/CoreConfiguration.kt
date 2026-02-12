package com.adaptibot.core

import com.adaptibot.core.domain.*
import com.adaptibot.core.domain.actions.ConditionEvaluator
import com.adaptibot.core.domain.actions.ElementFinder
import com.adaptibot.core.domain.observer.ObserverInterruptCoordinator
import com.adaptibot.core.domain.observer.ObserverRegistry
import com.adaptibot.core.domain.actions.ActionExecutor as ActionExecutorImpl
import com.adaptibot.ui.adapter.UiExecutionEventPublisher

object CoreConfiguration {
    fun getFacade(): CoreFacade {
        val elementFinder = ElementFinder()
        val conditionEvaluator = ConditionEvaluator(elementFinder)
        val eventPublisher = UiExecutionEventPublisher()
        val executionSession = ExecutionSession(eventPublisher)
        val observerRegistry = ObserverRegistry(conditionEvaluator, 1000)

        return CoreFacade(
            scriptExecutionService = ScriptExecutionService(
                executionSession = executionSession,
                observerRegistry = observerRegistry,
                stepSequenceExecutor = StepSequenceExecutor(
                    actionStepHandler = ActionStepHandler(
                        actionExecutor = ActionExecutorImpl(),
                        elementFinder = elementFinder,
                        eventPublisher = eventPublisher
                    ),
                    blockStepResolver = BlockStepResolver(conditionEvaluator),
                    observerRegistry = observerRegistry,
                    executionSession = executionSession,
                    observerInterruptCoordinator = ObserverInterruptCoordinator()
                ),
            )
        )
    }
}
