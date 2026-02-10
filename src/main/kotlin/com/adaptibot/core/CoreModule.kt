package com.adaptibot.core

import com.adaptibot.core.domain.ScriptExecutor
import com.adaptibot.core.domain.StepExecutor
import com.adaptibot.core.domain.actions.ActionExecutor
import com.adaptibot.core.domain.actions.ConditionEvaluator
import com.adaptibot.core.domain.actions.ElementFinder
import com.adaptibot.core.domain.observer.ObserverManager

internal object CoreModule {

    fun create(): CoreFacade {
        val elementFinder = ElementFinder()
        val conditionEvaluator = ConditionEvaluator(elementFinder)
        val observerManager = ObserverManager(conditionEvaluator, 1000)

        return CoreFacade(
            scriptExecutor = ScriptExecutor(
                stepExecutor = StepExecutor(
                    actionExecutor = ActionExecutor(),
                    elementFinder = elementFinder,
                    conditionEvaluator = conditionEvaluator,
                    observerManager = observerManager,
                ),
                observerManager = observerManager,
            ),
        )
    }

}

