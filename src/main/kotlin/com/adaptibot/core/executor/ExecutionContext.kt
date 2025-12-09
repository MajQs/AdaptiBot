package com.adaptibot.core.executor

import com.adaptibot.common.model.Script
import com.adaptibot.common.model.StepId

data class ExecutionContext(
    val script: Script,
    val currentStepId: StepId? = null,
    val state: ExecutionState = ExecutionState.IDLE,
    val iterationCount: Long = 0
)

enum class ExecutionState {
    IDLE,
    RUNNING,
    PAUSED,
    STOPPED
}