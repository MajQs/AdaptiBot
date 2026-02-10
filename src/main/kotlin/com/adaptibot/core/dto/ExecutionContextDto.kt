package com.adaptibot.core.dto

import com.adaptibot.common.model.StepId

data class ExecutionContextDto(
    val scriptName: String,
    val currentStepId: StepId?,
    val state: ExecutionStateDto,
    val iterationCount: Long,
    val interruptedStepId: StepId?,
    val isExecutingObserver: Boolean
)

