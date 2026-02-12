package com.adaptibot.core.domain

import com.adaptibot.common.model.Script
import com.adaptibot.common.model.Step
import com.adaptibot.core.dto.ExecutionContext
import com.adaptibot.core.dto.ExecutionState

internal class ExecutionSession {

    @Volatile
    private var currentContext: ExecutionContext = ExecutionContext(
        script = Script("", steps = emptyList()),
        state = ExecutionState.IDLE
    )


    fun getContext(): ExecutionContext = currentContext

    fun start(script: Script) {
        currentContext = ExecutionContext(
            script = script,
            state = ExecutionState.RUNNING
        )
    }

    fun stop() {
        currentContext = currentContext.copy(state = ExecutionState.STOPPED)
    }

    fun completeExecution() {
        if (currentContext.state != ExecutionState.STOPPED) {
            currentContext = currentContext.copy(state = ExecutionState.IDLE)
        }
    }

    fun isRunning(): Boolean = currentContext.state == ExecutionState.RUNNING

    fun isStopped(): Boolean = currentContext.state == ExecutionState.STOPPED

    fun isIdle(): Boolean = currentContext.state == ExecutionState.IDLE

    fun recordActiveStep(step: Step) {
        currentContext = currentContext.copy(activeStep = step)
    }

}

