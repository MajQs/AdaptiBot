package com.adaptibot.core.executor.actions

import com.adaptibot.common.model.Condition
import org.slf4j.LoggerFactory

class ConditionEvaluatorImpl(
    private val elementFinder: IElementFinder
) : IConditionEvaluator {
    
    private val logger = LoggerFactory.getLogger(ConditionEvaluatorImpl::class.java)
    
    override fun evaluate(condition: Condition): Boolean {
        return when (condition) {
            is Condition.ElementExists -> {
                elementFinder.find(condition.identifier) != null
            }
            is Condition.ElementNotExists -> {
                elementFinder.find(condition.identifier) == null
            }
            is Condition.And -> {
                condition.conditions.all { evaluate(it) }
            }
            is Condition.Or -> {
                condition.conditions.any { evaluate(it) }
            }
            is Condition.Not -> {
                !evaluate(condition.condition)
            }
        }
    }
}

