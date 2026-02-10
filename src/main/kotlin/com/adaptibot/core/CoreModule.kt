package com.adaptibot.core

import com.adaptibot.core.domain.ScriptOrchestrator
import com.adaptibot.core.domain.StepExecutionOrchestrator
import com.adaptibot.core.domain.StepExecutor
import com.adaptibot.core.domain.actions.ActionExecutor
import com.adaptibot.core.domain.actions.ConditionEvaluator
import com.adaptibot.core.domain.actions.ElementFinder
import com.adaptibot.core.domain.observer.ObserverManager

object CoreModule {
    fun getFacade(): CoreFacade {
        val elementFinder = ElementFinder()
        val conditionEvaluator = ConditionEvaluator(elementFinder)
        val observerManager = ObserverManager(conditionEvaluator, 1000)
        val stepExecutor = StepExecutor(
            actionExecutor = ActionExecutor(),
            elementFinder = elementFinder,
            conditionEvaluator = conditionEvaluator,
        )
        val stepExecutionOrchestrator = StepExecutionOrchestrator(
            stepExecutor = stepExecutor,
            observerManager = observerManager
        )
        val scriptOrchestrator = ScriptOrchestrator(
            stepExecutionOrchestrator = stepExecutionOrchestrator
        )

        return CoreFacade(
            scriptOrchestrator = scriptOrchestrator
        )
    }
}
