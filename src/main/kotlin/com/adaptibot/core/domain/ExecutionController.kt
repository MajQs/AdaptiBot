package com.adaptibot.core.domain

import com.adaptibot.common.model.Script
import com.adaptibot.core.dto.ExecutionContext
import com.adaptibot.core.dto.ExecutionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class ExecutionController {

    @Volatile
    private var currentContext: ExecutionContext = ExecutionContext(
        script = Script("", steps = emptyList()),
        state = ExecutionState.IDLE
    )

    private var executionScope: CoroutineScope? = null

    fun getContext(): ExecutionContext = currentContext

    fun start(script: Script) {
        currentContext = ExecutionContext(
            script = script,
            state = ExecutionState.RUNNING
        )
        executionScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    }

    fun pause() {
        if (currentContext.state == ExecutionState.RUNNING) {
            currentContext = currentContext.copy(state = ExecutionState.PAUSED)
        }
    }

    fun resume() {
        if (currentContext.state == ExecutionState.PAUSED) {
            currentContext = currentContext.copy(state = ExecutionState.RUNNING)
        }
    }

    fun stop() {
        currentContext = currentContext.copy(state = ExecutionState.STOPPED)
        executionScope?.cancel()
    }

    fun finish() {
        if (currentContext.state != ExecutionState.STOPPED) {
            currentContext = currentContext.copy(state = ExecutionState.IDLE)
        }
    }

    fun getScope(): CoroutineScope? = executionScope

    fun setActiveStep(step: com.adaptibot.common.model.Step) {
        currentContext = currentContext.copy(activeStep = step)
    }
}
