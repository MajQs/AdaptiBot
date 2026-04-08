package com.adaptibot.execution.domain

import com.adaptibot.script.Condition
import com.adaptibot.script.VisualMatcher
import com.adaptibot.vision.VisionFacade

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
        is VisualMatcher.ImagePresent -> {
            val match = visionFacade.getImageMatch(matcher.pattern)
            match != null && match.confidence >= matcher.pattern.matchThreshold
        }
        is VisualMatcher.ColorAt -> {
            val actual = visionFacade.getPixelColor(matcher.point)
            actual.matches(matcher.expected, matcher.tolerance)
        }
    }
}


