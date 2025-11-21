package com.adaptibot.core.executor

import com.adaptibot.common.model.Script
import com.adaptibot.common.model.Step
import com.adaptibot.common.model.StepId

data class ExecutionContext(
    val script: Script,
    val currentStepId: StepId? = null,
    val state: ExecutionState = ExecutionState.IDLE
)

