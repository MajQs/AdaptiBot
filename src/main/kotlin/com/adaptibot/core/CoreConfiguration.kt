package com.adaptibot.core

import com.adaptibot.core.domain.*
import com.adaptibot.core.domain.actions.ConditionEvaluator
import com.adaptibot.core.domain.actions.ElementFinder
import com.adaptibot.core.domain.observer.ObserverManager
import com.adaptibot.core.domain.actions.ActionExecutor as ActionExecutorImpl

object CoreConfiguration {
    fun getFacade(): CoreFacade {
        val elementFinder = ElementFinder()
        val conditionEvaluator = ConditionEvaluator(elementFinder)
        val executionController = ExecutionController()

        return CoreFacade(
            scriptOrchestrator = ScriptOrchestrator(
                stepExecutionOrchestrator = StepExecutionOrchestrator(
                    actionExecutor = ActionExecutor(
                        actionExecutorImpl = ActionExecutorImpl(),
                        elementFinder = elementFinder
                    ),
                    blockExecutor = BlockExecutor(conditionEvaluator),
                    observerManager = ObserverManager(conditionEvaluator, 1000),
                    executionController = executionController
                ),
                executionController = executionController
            )

        )
    }
}
