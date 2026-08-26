package com.adaptibot.execution.domain

import com.adaptibot.action.ActionExecutionException
import com.adaptibot.action.ActionFacade
import com.adaptibot.script.step.ActionStep
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
            eventPublisher.logStepSuccess(stepName, startTime.duration())
        } catch (e: InterruptedException) {
            // Stop requested — do not swallow, let the interpreter unwind immediately
            Thread.currentThread().interrupt()
            logger.debug("Step interrupted: {}", stepName)
            throw e
        } catch (e: ActionExecutionException) {
            eventPublisher.logStepFailure(stepName, startTime.duration(), e.message ?: "Action execution failed")
        } catch (e: Exception) {
            logger.error("Unexpected error executing step: $stepName", e)
            eventPublisher.logStepFailure(stepName, startTime.duration(), e.message ?: "Unexpected error")
        }
    }

    /** Releases keys and mouse buttons still held down after an aborted action. */
    fun releaseHeldInputs() = actionFacade.releaseAllInputs()

    private fun extractStepName(step: ActionStep): String =
        step.label ?: step.action::class.simpleName ?: "Action"

    private fun Long.duration(): Long = System.currentTimeMillis() - this
}
