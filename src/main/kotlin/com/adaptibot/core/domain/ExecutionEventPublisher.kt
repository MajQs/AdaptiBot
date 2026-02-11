package com.adaptibot.core.domain

/**
 * Interface for publishing execution events.
 * This allows the domain layer to be decoupled from the UI layer.
 */
interface ExecutionEventPublisher {

    /**
     * Log the start of script execution.
     */
    fun logExecutionStart(scriptName: String)

    /**
     * Log the end of script execution.
     */
    fun logExecutionStop()

    /**
     * Log successful step execution.
     */
    fun logStepSuccess(stepName: String, durationMs: Long)

    /**
     * Log failed step execution.
     */
    fun logStepFailure(stepName: String, durationMs: Long, error: String)
}

