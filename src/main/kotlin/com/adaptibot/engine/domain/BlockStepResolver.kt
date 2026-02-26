package com.adaptibot.engine.domain

import com.adaptibot.model.BlockStep
import com.adaptibot.model.ConditionalBlock
import com.adaptibot.model.GroupBlock
import com.adaptibot.model.Step
import com.adaptibot.vision.ConditionEvaluator
import org.slf4j.LoggerFactory

internal class BlockStepResolver(
    private val conditionEvaluator: ConditionEvaluator,
) {
    private val logger = LoggerFactory.getLogger(BlockStepResolver::class.java)

    fun resolveNestedSteps(block: BlockStep): List<Step> {
        return when (block) {
            is GroupBlock -> block.steps
            is ConditionalBlock -> {
                try {
                    if (conditionEvaluator.evaluate(block.condition)) {
                        block.thenSteps
                    } else {
                        block.elseSteps
                    }
                } catch (e: Exception) {
                    logger.error("Exception executing conditional block: ${block.label ?: block.id.value}", e)
                    emptyList()
                }
            }
        }
    }
}
