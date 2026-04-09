package com.adaptibot.execution.domain

import com.adaptibot.script.value.VisualMatcher

/**
 * Evaluates a [VisualMatcher] to a boolean result.
 *
 * The name is intentionally generic – new matcher types (e.g. OCR, colour check)
 * are added by implementing this interface, not by extending [VisualMatcher] alone.
 * [ConditionEvaluator] decides which strategy to use based on the matcher type.
 */
interface MatcherEvaluator {
    fun evaluate(matcher: VisualMatcher): Boolean
}

