package com.adaptibot.engine.domain

import com.adaptibot.action.ActionExecutionException
import com.adaptibot.action.ActionFacade
import com.adaptibot.common.model.ActionStep
import com.adaptibot.engine.dto.StepExecutionMetrics
import org.slf4j.LoggerFactory

internal class ActionStepHandler(
    private val actionFacade: ActionFacade,
    private val eventPublisher: ExecutionEventPublisher
) {
    private val logger = LoggerFactory.getLogger(ActionStepHandler::class.java)

    fun execute(step: ActionStep) {
        val stepName = extractStepName(step)
        val startTime = System.currentTimeMillis()

        try {
            actionFacade.execute(step.action)
            logExecutionResult(StepExecutionMetrics(stepName = stepName, startTime = startTime, success = true))
        } catch (e: ActionExecutionException) {
            logExecutionResult(StepExecutionMetrics(stepName = stepName, startTime = startTime, success = false, error = e.message))
        } catch (e: Exception) {
            logger.error("Unexpected error executing step: $stepName", e)
            logExecutionResult(StepExecutionMetrics(stepName = stepName, startTime = startTime, success = false, error = e.message ?: "Unexpected error"))
        }
    }


    private fun logExecutionResult(metrics: StepExecutionMetrics) {
        val duration = metrics.duration()
        if (metrics.success) {
            eventPublisher.logStepSuccess(metrics.stepName, duration)
        } else {
            val errorMessage = metrics.error ?: "Action failed"
            eventPublisher.logStepFailure(metrics.stepName, duration, errorMessage)
            logger.error("Action execution failed: ${metrics.stepName} – $errorMessage")
        }
    }

    private fun extractStepName(step: ActionStep): String =
        step.label ?: step.action::class.simpleName ?: "Action"
}
