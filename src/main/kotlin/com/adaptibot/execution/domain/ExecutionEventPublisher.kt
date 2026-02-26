package com.adaptibot.execution.domain

interface ExecutionEventPublisher {

    fun logExecutionStart(scriptName: String)

    fun logExecutionStop()

    fun logStepSuccess(stepName: String, durationMs: Long)

    fun logStepFailure(stepName: String, durationMs: Long, error: String)
}

