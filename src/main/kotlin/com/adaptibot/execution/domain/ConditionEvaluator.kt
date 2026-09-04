package com.adaptibot.execution.domain

import com.adaptibot.script.value.Condition
import com.adaptibot.script.value.Matcher
import com.adaptibot.script.value.PixelColor
import com.adaptibot.infrastructure.ScreenCapture
import com.adaptibot.infrastructure.SharedScreenFrame
import com.adaptibot.vision.VisionFacade
import com.adaptibot.vision.VisionQuery

internal class ConditionEvaluator(
    private val visionFacade: VisionFacade,
) {

    /**
     * Evaluates [condition]. Captures taken while evaluating a compound condition are shared, so
     * an AND/OR over several matchers watching the same area costs a single screenshot. Nested
     * calls join the frame opened by the outermost one (or by an observer pass).
     */
    fun evaluate(condition: Condition): Boolean = SharedScreenFrame.shareCaptures {
        evaluateInFrame(condition)
    }

    private fun evaluateInFrame(condition: Condition): Boolean = when (condition) {
        is Condition.ElementExists -> evaluateMatcher(condition.matcher)
        is Condition.And           -> condition.conditions.all { evaluateInFrame(it) }
        is Condition.Or            -> condition.conditions.any { evaluateInFrame(it) }
        is Condition.Not           -> !evaluateInFrame(condition.condition)
    }

    private fun evaluateMatcher(matcher: Matcher): Boolean = when (matcher) {
        is Matcher.ImagePresent -> {
            val match = visionFacade.find(VisionQuery.ByImage(matcher.pattern, matcher.location))
            match != null && match.confidence >= matcher.pattern.matchThreshold
        }
        is Matcher.ColorAt -> {
            val pixel = ScreenCapture.getPixelColor(matcher.point.x, matcher.point.y)
            val actual = PixelColor(r = pixel.red, g = pixel.green, b = pixel.blue, a = pixel.alpha)
            actual.matches(matcher.expected, matcher.tolerance)
        }
        is Matcher.TextPresent -> {
            visionFacade.find(VisionQuery.ByText(matcher.text, matcher.location)) != null
        }
    }
}
