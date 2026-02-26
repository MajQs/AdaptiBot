package com.adaptibot.vision

import com.adaptibot.model.Condition

class ConditionEvaluator(
    private val elementFinder: ElementLocator
) {

    fun evaluate(condition: Condition): Boolean {
        return when (condition) {
            is Condition.ElementExists -> elementFinder.find(condition.identifier) is ElementLookupResult.Found
            is Condition.And -> condition.conditions.all { evaluate(it) }
            is Condition.Or -> condition.conditions.any { evaluate(it) }
            is Condition.Not -> !evaluate(condition.condition)
        }
    }
}