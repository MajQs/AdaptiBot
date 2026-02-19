package com.adaptibot.engine.dto

import com.adaptibot.common.model.Script
import com.adaptibot.common.model.Step

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