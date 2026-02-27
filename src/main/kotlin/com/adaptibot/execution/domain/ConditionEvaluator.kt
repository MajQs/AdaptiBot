package com.adaptibot.execution.domain

import com.adaptibot.model.Condition
import com.adaptibot.vision.VisionFacade
import com.adaptibot.vision.dto.ElementLookupResult

internal class ConditionEvaluator(
    private val visionFacade: VisionFacade
) {

    fun evaluate(condition: Condition): Boolean {
        return when (condition) {
            is Condition.ElementExists -> visionFacade.findElement(condition.identifier) is ElementLookupResult.Found //TODO vision use ElementIdentifier.ByCoordinate that have no sense for Condition
            is Condition.And -> condition.conditions.all { evaluate(it) }
            is Condition.Or -> condition.conditions.any { evaluate(it) }
            is Condition.Not -> !evaluate(condition.condition)
        }
    }
}