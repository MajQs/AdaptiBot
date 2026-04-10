package com.adaptibot.execution.domain

import com.adaptibot.script.step.ConditionalStep
import com.adaptibot.script.step.Step

internal class ConditionalStepHandler(
    private val conditionEvaluator: ConditionEvaluator
) {
    fun resolve(step: ConditionalStep): List<Step> =
        if (conditionEvaluator.evaluate(step.condition)) step.ifBlock.steps else step.elseBlock.steps
}
