package com.adaptibot.core.executor.actions

import com.adaptibot.common.model.Condition

interface IConditionEvaluator {
    fun evaluate(condition: Condition): Boolean
}

