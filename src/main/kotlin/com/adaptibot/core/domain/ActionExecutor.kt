package com.adaptibot.core.domain

import com.adaptibot.common.model.Action
import com.adaptibot.common.model.ActionStep
import com.adaptibot.core.domain.actions.ActionExecutor as ActionExecutorImpl
import com.adaptibot.core.domain.actions.ElementFinder
import org.slf4j.LoggerFactory

internal class ActionExecutor(
    private val actionExecutorImpl: ActionExecutorImpl,
    private val elementFinder: ElementFinder,
    private val eventPublisher: ExecutionEventPublisher
) {
    private val logger = LoggerFactory.getLogger(ActionExecutor::class.java)

    fun execute(step: ActionStep): Boolean {

        val stepName = step.label ?: step.action::class.simpleName ?: "Action"
        val startTime = System.currentTimeMillis()

        return try {
            val coordinate = when (val action = step.action) {
                is Action.Mouse -> {
                    val target = when (action) {
                        is Action.Mouse.LeftClick -> action.target
                        is Action.Mouse.RightClick -> action.target
                        is Action.Mouse.DoubleClick -> action.target
                        is Action.Mouse.MoveTo -> action.target
                        else -> null
                    }
                    target?.let { elementFinder.find(it) }
                }
                else -> null
            }

            val success = actionExecutorImpl.execute(step.action, coordinate)
            val duration = System.currentTimeMillis() - startTime

            if (success) {
                eventPublisher.logStepSuccess(stepName, duration)
            } else {
                eventPublisher.logStepFailure(stepName, duration, "Action failed")
                logger.error("Action execution failed: ${step.label ?: step.id.value}")
            }

            success
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            eventPublisher.logStepFailure(stepName, duration, e.message ?: "Exception")
            logger.error("Exception executing action step: ${step.label ?: step.id.value}", e)
            false
        }
    }
}
