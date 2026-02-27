package com.adaptibot.execution.domain

import com.adaptibot.model.Script
import com.adaptibot.model.Step
import com.adaptibot.execution.dto.ExecutionContext
import com.adaptibot.execution.dto.ExecutionState
import org.slf4j.LoggerFactory

internal class ScriptExecutionState(
    private val eventPublisher: ExecutionEventPublisher
) {

    @Volatile
    private var currentContext: ExecutionContext = ExecutionContext.default()

    private val logger = LoggerFactory.getLogger(ScriptExecutionState::class.java)

    fun create(script: Script): ExecutionContext {
        if (currentContext.state != ExecutionState.IDLE) {
            logger.warn("Cannot start script - already running")
            throw SessionRunningException()
        }
        eventPublisher.logExecutionStart(script.name)
        currentContext = ExecutionContext.runFor(script)
        return currentContext
    }

    fun stop() {
        eventPublisher.logExecutionStop()
        currentContext = currentContext.copy(state = ExecutionState.STOPPED)
    }

    fun completeExecution() {
        currentContext = ExecutionContext.default()
    }

    fun getState(): ExecutionState = currentContext.state

    fun isRunning(): Boolean = currentContext.state == ExecutionState.RUNNING

    fun isStopped(): Boolean = currentContext.state == ExecutionState.STOPPED

    fun recordActiveStep(step: Step) {
        currentContext = currentContext.copy(activeStep = step)
    }

    private class SessionRunningException : RuntimeException("Session is already running")
}

