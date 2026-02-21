package com.adaptibot.engine.domain

import com.adaptibot.action.ActionFacade
import com.adaptibot.action.domain.ActionExecutor
import com.adaptibot.common.model.Action
import com.adaptibot.common.model.ActionStep
import com.adaptibot.common.model.ElementIdentifier
import com.adaptibot.engine.domain.actions.ElementFinder
import com.adaptibot.engine.dto.StepExecutionMetrics
import org.slf4j.LoggerFactory

internal class ActionStepHandler(
    private val actionFacade: ActionFacade,
    private val eventPublisher: ExecutionEventPublisher
) {
    private val logger = LoggerFactory.getLogger(ActionStepHandler::class.java)

    fun execute(step: ActionStep): Boolean {
        val stepName = extractStepName(step)
        val startTime = System.currentTimeMillis()

        return try {
            actionFacade.execute(step.action)
            val success = true

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

}

