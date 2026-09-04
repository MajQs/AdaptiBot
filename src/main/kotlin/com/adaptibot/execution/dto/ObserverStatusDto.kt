package com.adaptibot.execution.dto

/**
 * Runtime state of the observer mechanism, published whenever it changes so the UI can show which
 * observers are currently watching and which one is handling.
 */
data class ObserverStatusDto(
    val armed: List<ObservedStepDto>,
    val handling: ObservedStepDto?,
) {
    companion object {
        val NONE = ObserverStatusDto(armed = emptyList(), handling = null)
    }
}

data class ObservedStepDto(
    val stepId: String,
    val label: String,
)

