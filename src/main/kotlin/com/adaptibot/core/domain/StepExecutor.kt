package com.adaptibot.core.domain

import com.adaptibot.common.model.Step
import com.adaptibot.core.domain.actions.ActionExecutor
import com.adaptibot.core.domain.actions.ConditionEvaluator
import com.adaptibot.core.domain.actions.ElementFinder
import com.adaptibot.core.domain.observer.ObserverManager
import com.adaptibot.core.dto.ExecutionContext
import com.adaptibot.core.dto.ExecutionState
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

internal class StepExecutor(
    private val actionExecutor: ActionExecutor,
    private val elementFinder: ElementFinder,
    private val conditionEvaluator: ConditionEvaluator,
    private val observerManager: ObserverManager,
    ) {

    private val logger = LoggerFactory.getLogger(StepExecutor::class.java)

    suspend fun execute(step: Step, context: ExecutionContext, shouldStop: () -> Boolean): Boolean {
        if (shouldStop() || context.state == ExecutionState.PAUSED) {
            return false
        }

        if (step.delayBefore > 0) {
            delay(step.delayBefore)
        }

        val success = when (step) {
            is Step.ActionStep -> executeActionStep(step, shouldStop)
            is Step.ConditionalBlock -> executeConditionalBlock(step, context, shouldStop)
            is Step.ObserverBlock -> registerObserver(step)
            is Step.GroupBlock -> executeGroupBlock(step, context, shouldStop)
        }

        if (step.delayAfter > 0) {
            delay(step.delayAfter)
        }

        return success
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

    private suspend fun executeConditionalBlock(
        block: Step.ConditionalBlock,
        context: ExecutionContext,
        shouldStop: () -> Boolean
    ): Boolean {
        return try {
            val conditionMet = conditionEvaluator.evaluate(block.condition)

            val stepsToExecute = if (conditionMet) {
                block.thenSteps
            } else {
                block.elseSteps
            }

            for (step in stepsToExecute) {
                if (shouldStop() || context.state == ExecutionState.PAUSED) {
                    return false
                }
                execute(step, context, shouldStop)
            }

            true
        } catch (e: Exception) {
            logger.error("Exception executing conditional block: ${block.label ?: block.id.value}", e)
            false
        }
    }

    private fun registerObserver(block: Step.ObserverBlock): Boolean {
        return try {
            observerManager.registerObserver(block)
            true
        } catch (e: Exception) {
            logger.error("Exception registering observer: ${block.label ?: block.id.value}", e)
            false
        }
    }

    private suspend fun executeGroupBlock(
        block: Step.GroupBlock,
        context: ExecutionContext,
        shouldStop: () -> Boolean
    ): Boolean {
        return try {
            for (step in block.steps) {
                if (shouldStop() || context.state == ExecutionState.PAUSED) {
                    return false
                }
                execute(step, context, shouldStop)
            }
            true
        } catch (e: Exception) {
            logger.error("Exception executing group block: ${block.label ?: block.id.value}", e)
            false
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

