package com.adaptibot.execution.dto

import com.adaptibot.script.Script
import com.adaptibot.script.Step

internal data class ExecutionContext(
    val script: Script = Script("", steps = emptyList()),
    val state: ExecutionState = ExecutionState.IDLE,
    val activeStep: Step? = null
) {
    companion object {
        @JvmStatic
        fun default() = ExecutionContext()
        fun runFor(script: Script) = ExecutionContext(script = script, state = ExecutionState.RUNNING)
    }
}

internal enum class ExecutionState {
    IDLE,
    RUNNING,
    STOPPED
}