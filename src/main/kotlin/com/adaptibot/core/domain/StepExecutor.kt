package com.adaptibot.core.domain

import com.adaptibot.common.model.Step
import com.adaptibot.core.domain.actions.ActionExecutor
import com.adaptibot.core.domain.actions.ConditionEvaluator
import com.adaptibot.core.domain.actions.ElementFinder
import com.adaptibot.core.dto.ExecutionContext
import com.adaptibot.core.dto.ExecutionState
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

internal class StepExecutor(
    private val actionExecutor: ActionExecutor,
    private val elementFinder: ElementFinder,
    private val conditionEvaluator: ConditionEvaluator,
    ) {

    private val logger = LoggerFactory.getLogger(StepExecutor::class.java)

suspend fun execute(step: Step, context: ExecutionContext, shouldStop: () -> Boolean): Pair<List<Step>, List<Step.ObserverBlock>> {
    if (shouldStop() || context.state == ExecutionState.PAUSED) {
        return Pair(emptyList(), emptyList())
    }

    if (step.delayBefore > 0) {
        delay(step.delayBefore)
    }

    val result = when (step) {
        is Step.ActionStep -> {
            executeActionStep(step, shouldStop)
            Pair(emptyList(), emptyList())
        }
        is Step.ConditionalBlock -> executeConditionalBlock(step)
        is Step.ObserverBlock -> Pair(emptyList(), listOf(step))
        is Step.GroupBlock -> Pair(step.steps, emptyList())
    }

    if (step.delayAfter > 0) {
        delay(step.delayAfter)
    }

    return result
}

private fun executeActionStep(step: Step.ActionStep, shouldStop: () -> Boolean): Boolean {
    if (shouldStop()) return false

    val stepName = step.label ?: step.action::class.simpleName ?: "Action"
    val startTime = System.currentTimeMillis()

    return try {
        val coordinate = when (val action = step.action) {
            is com.adaptibot.common.model.Action.Mouse -> {
                val target = when (action) {
                    is com.adaptibot.common.model.Action.Mouse.LeftClick -> action.target
                    is com.adaptibot.common.model.Action.Mouse.RightClick -> action.target
                    is com.adaptibot.common.model.Action.Mouse.DoubleClick -> action.target
                    is com.adaptibot.common.model.Action.Mouse.MoveTo -> action.target
                    else -> null
                }
                target?.let { elementFinder.find(it) }
            }
            else -> null
        }

        val success = actionExecutor.execute(step.action, coordinate)
        val duration = System.currentTimeMillis() - startTime

        if (success) {
            com.adaptibot.ui.model.ExecutionLogger.logStepSuccess(stepName, duration)
        } else {
            com.adaptibot.ui.model.ExecutionLogger.logStepFailure(stepName, duration, "Action failed")
            logger.error("Action execution failed: ${step.label ?: step.id.value}")
        }

        handleFlowControl(step.action)
        success

    } catch (e: Exception) {
        val duration = System.currentTimeMillis() - startTime
        com.adaptibot.ui.model.ExecutionLogger.logStepFailure(stepName, duration, e.message ?: "Exception")
        logger.error("Exception executing action step: ${step.label ?: step.id.value}", e)
        false
    }
}

private fun executeConditionalBlock(
    block: Step.ConditionalBlock
): Pair<List<Step>, List<Step.ObserverBlock>> {
    return try {
        val conditionMet = conditionEvaluator.evaluate(block.condition)
        val steps = if (conditionMet) {
            block.thenSteps
        } else {
            block.elseSteps
        }
        Pair(steps, emptyList())
    } catch (e: Exception) {
        logger.error("Exception executing conditional block: ${block.label ?: block.id.value}", e)
        Pair(emptyList(), emptyList())
    }
}

    //TODO not for now, not sure if we want to support this in the future, need to think about how it would interact with the execution flow and observers
    private fun handleFlowControl(action: com.adaptibot.common.model.Action) {
        when (action) {
            is com.adaptibot.common.model.Action.Flow.Stop,
            is com.adaptibot.common.model.Action.Flow.JumpTo,
            is com.adaptibot.common.model.Action.Flow.Continue -> {
                //onFlowControl(action)
            }
            else -> {}
        }
    }
}

