package com.adaptibot.core.domain

import com.adaptibot.common.model.Script
import com.adaptibot.common.model.Step
import com.adaptibot.core.dto.ExecutionContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class ExecutionSession {

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

    fun stop() {
        currentContext = currentContext.copy(state = ExecutionState.STOPPED)
        executionScope?.cancel()
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

    internal fun launchInScope(block: suspend CoroutineScope.() -> Unit) {
        executionScope?.launch(block = block)
    }
}

