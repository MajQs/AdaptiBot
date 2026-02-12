package com.adaptibot.core.domain

import com.adaptibot.common.model.Action
import com.adaptibot.common.model.ActionStep
import com.adaptibot.common.model.ElementIdentifier
import com.adaptibot.core.domain.actions.ElementFinder
import com.adaptibot.core.dto.StepExecutionMetrics
import org.slf4j.LoggerFactory

internal class ActionStepHandler(
    private val actionExecutor: com.adaptibot.core.domain.actions.ActionExecutor,
    private val elementFinder: ElementFinder,
    private val eventPublisher: ExecutionEventPublisher
) {
    private val logger = LoggerFactory.getLogger(ActionStepHandler::class.java)

    fun execute(step: ActionStep): Boolean {
        val stepName = extractStepName(step)
        val startTime = System.currentTimeMillis()

        return try {
            val coordinate = extractTargetFromAction(step.action)?.let { elementFinder.find(it) }
            val success = actionExecutor.execute(step.action, coordinate)

            val metrics = StepExecutionMetrics(
                stepName = stepName,
                startTime = startTime,
                success = success
            )

            logExecutionResult(metrics)
            success
        } catch (e: Exception) {
            val metrics = StepExecutionMetrics(
                stepName = stepName,
                startTime = startTime,
                success = false,
                error = e.message ?: "Exception"
            )

            logExecutionResult(metrics)
            false
        }
    }

    private fun logExecutionResult(metrics: StepExecutionMetrics) {
        val duration = metrics.duration()

        if (metrics.success) {
            eventPublisher.logStepSuccess(metrics.stepName, duration)
        } else {
            val errorMessage = metrics.error ?: "Action failed"
            eventPublisher.logStepFailure(metrics.stepName, duration, errorMessage)
            logger.error("Action execution failed: " + metrics.stepName)
        }
    }

    private fun extractStepName(step: ActionStep): String {
        return step.label ?: step.action::class.simpleName ?: "Action"
    }

    private fun extractTargetFromAction(action: Action): ElementIdentifier? {
        return when (action) {
            is Action.Mouse.LeftClick -> action.target
            is Action.Mouse.RightClick -> action.target
            is Action.Mouse.DoubleClick -> action.target
            is Action.Mouse.MoveTo -> action.target
            else -> null
        }
    }
}

