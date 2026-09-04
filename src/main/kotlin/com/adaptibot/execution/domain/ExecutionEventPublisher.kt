package com.adaptibot.execution.domain

import com.adaptibot.execution.dto.ObserverStatusDto

interface ExecutionEventPublisher {

    fun logExecutionStart(scriptName: String)

    fun logExecutionStop()

    fun logStepSuccess(stepName: String, durationMs: Long)

    fun logStepFailure(stepName: String, durationMs: Long, error: String)

    /** Published whenever the set of watching observers or the handling observer changes. */
    fun observerStatusChanged(status: ObserverStatusDto)
}

