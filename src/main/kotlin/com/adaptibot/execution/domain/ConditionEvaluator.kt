package com.adaptibot.execution.domain

import com.adaptibot.model.Condition
import com.adaptibot.model.VisualMatcher
import com.adaptibot.vision.VisionFacade
import com.adaptibot.vision.dto.ElementLookupResult

internal class ConditionEvaluator(
    private val visionFacade: VisionFacade,
) {

    fun evaluate(condition: Condition): Boolean = when (condition) {
        is Condition.ElementExists -> evaluateMatcher(condition.matcher)
        is Condition.And           -> condition.conditions.all { evaluate(it) }
        is Condition.Or            -> condition.conditions.any { evaluate(it) }
        is Condition.Not           -> !evaluate(condition.condition)
    }

    private fun evaluateMatcher(matcher: VisualMatcher): Boolean = when (matcher) {
        is VisualMatcher.ImagePresent -> visionFacade.findImage(matcher.pattern) is ElementLookupResult.Found
    }
}