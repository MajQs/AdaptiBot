package com.adaptibot.core.domain

/**
 * Value object representing metrics of step execution.
 * Encapsulates timing and result information.
 */
internal data class StepExecutionMetrics(
    val stepName: String,
    val startTime: Long,
    val success: Boolean,
    val error: String? = null
) {
    /**
     * Calculate the duration in milliseconds from start time to now.
     */
    fun duration(): Long = System.currentTimeMillis() - startTime
}

