package com.adaptibot.engine.domain.actions

import com.adaptibot.common.model.Condition
import com.adaptibot.vision.ElementFinder
import com.adaptibot.vision.ElementLookupResult
import org.slf4j.LoggerFactory

internal class ConditionEvaluator(
    private val elementFinder: ElementFinder
) {

    private val logger = LoggerFactory.getLogger(ConditionEvaluator::class.java)

    fun evaluate(condition: Condition): Boolean {
        return when (condition) {
            is Condition.ElementExists -> elementFinder.find(condition.identifier) is ElementLookupResult.Found
            is Condition.ElementNotExists -> elementFinder.find(condition.identifier) !is ElementLookupResult.Found
            is Condition.And -> condition.conditions.all { evaluate(it) }
            is Condition.Or -> condition.conditions.any { evaluate(it) }
            is Condition.Not -> !evaluate(condition.condition)
        }
    }
}
