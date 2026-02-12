package com.adaptibot.core.dto

import com.adaptibot.common.model.Script
import com.adaptibot.common.model.Step
import com.adaptibot.core.dto.ExecutionState

internal data class ExecutionContext(
    val script: Script,
    val state: ExecutionState = ExecutionState.IDLE,
    val activeStep: Step? = null
)

