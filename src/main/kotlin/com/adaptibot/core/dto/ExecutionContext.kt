package com.adaptibot.core.dto

import com.adaptibot.common.model.Script
import com.adaptibot.common.model.StepId

data class ExecutionContext(
    val script: Script,
    val currentStepId: StepId? = null,
    val state: ExecutionState = ExecutionState.IDLE,
    val interruptedStepId: StepId? = null,
    val isExecutingObserver: Boolean = false
)

enum class ExecutionState {
    IDLE,
    RUNNING,
    PAUSED,
    STOPPED
}