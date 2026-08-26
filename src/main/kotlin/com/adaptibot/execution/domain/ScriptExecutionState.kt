package com.adaptibot.execution.domain

import com.adaptibot.script.Script
import com.adaptibot.script.step.Step
import com.adaptibot.execution.dto.ExecutionContext
import com.adaptibot.execution.dto.ExecutionStateDto
import org.slf4j.LoggerFactory

internal class ScriptExecutionState(
    private val eventPublisher: ExecutionEventPublisher
) {

    @Volatile
    private var currentContext: ExecutionContext = ExecutionContext.default()

    private val logger = LoggerFactory.getLogger(ScriptExecutionState::class.java)

    fun create(script: Script): ExecutionContext {
        if (currentContext.state != ExecutionStateDto.IDLE) {
            logger.warn("Cannot start script - already running")
            throw SessionRunningException()
        }
        eventPublisher.logExecutionStart(script.name)
        currentContext = ExecutionContext.runFor(script)
        return currentContext
    }

    fun stop(): Boolean {
        if (currentContext.state != ExecutionStateDto.RUNNING) {
            logger.debug("Stop requested but no session is running")
            return false
        }
        eventPublisher.logExecutionStop()
        currentContext = currentContext.copy(state = ExecutionStateDto.STOPPED)
        return true
    }

    fun completeExecution() {
        currentContext = ExecutionContext.default()
    }

    fun getState(): ExecutionStateDto = currentContext.state

    fun isRunning(): Boolean = currentContext.state == ExecutionStateDto.RUNNING

    fun isStopped(): Boolean = currentContext.state == ExecutionStateDto.STOPPED

    fun recordActiveStep(step: Step) {
        currentContext = currentContext.copy(activeStep = step)
    }

    private class SessionRunningException : RuntimeException("Session is already running")
}
