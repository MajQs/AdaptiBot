package com.adaptibot.core.executor

import com.adaptibot.common.model.StepId

sealed class ExecutionResult {
    data class Success(val stepId: StepId, val durationMs: Long) : ExecutionResult()
    data class Failure(val stepId: StepId, val error: Throwable, val durationMs: Long) : ExecutionResult()
    data class FlowControl(val stepId: StepId, val jumpToStepId: StepId?) : ExecutionResult()
}

