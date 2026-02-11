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
        val executionSession = ExecutionSession()
        val eventPublisher = UiExecutionEventPublisher()

        return CoreFacade(
            scriptExecutionService = ScriptExecutionService(
                stepSequenceExecutor = StepSequenceExecutor(
                    actionStepHandler = ActionStepHandler(
                        actionExecutor = ActionExecutorImpl(),
                        elementFinder = elementFinder,
                        eventPublisher = eventPublisher
                    ),
                    blockStepResolver = BlockStepResolver(conditionEvaluator),
                    observerRegistry = ObserverRegistry(conditionEvaluator, 1000),
                    executionSession = executionSession,
                    observerInterruptCoordinator = ObserverInterruptCoordinator()
                ),
                executionSession = executionSession,
                eventPublisher = eventPublisher
            )

        )
    }
}
