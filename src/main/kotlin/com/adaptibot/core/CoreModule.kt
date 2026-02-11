package com.adaptibot.core

import com.adaptibot.core.domain.ActionExecutor
import com.adaptibot.core.domain.BlockExecutor
import com.adaptibot.core.domain.ExecutionController
import com.adaptibot.core.domain.ScriptOrchestrator
import com.adaptibot.core.domain.StepExecutionOrchestrator
import com.adaptibot.core.domain.actions.ActionExecutor as ActionExecutorImpl
import com.adaptibot.core.domain.actions.ConditionEvaluator
import com.adaptibot.core.domain.actions.ElementFinder
import com.adaptibot.core.domain.observer.ObserverManager

object CoreModule {
    fun getFacade(): CoreFacade {
        val elementFinder = ElementFinder()
        val conditionEvaluator = ConditionEvaluator(elementFinder)
        val observerManager = ObserverManager(conditionEvaluator, 1000)
        val actionExecutorImpl = ActionExecutorImpl()
        val actionExecutor = ActionExecutor(actionExecutorImpl, elementFinder)
        val blockExecutor = BlockExecutor(conditionEvaluator)
        val executionController = ExecutionController()
        val stepExecutionOrchestrator = StepExecutionOrchestrator(
            actionExecutor = actionExecutor,
            blockExecutor = blockExecutor,
            observerManager = observerManager,
            executionController = executionController
        )
        val scriptOrchestrator = ScriptOrchestrator(
            stepExecutionOrchestrator = stepExecutionOrchestrator,
            executionController = executionController
        )

        return CoreFacade(
            scriptOrchestrator = scriptOrchestrator
        )
    }
}
