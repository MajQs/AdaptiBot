package com.adaptibot.core.dto

internal data class StepExecutionMetrics(
    val stepName: String,
    val startTime: Long,
    val success: Boolean,
    val error: String? = null
) {
    fun duration(): Long = System.currentTimeMillis() - startTime
}