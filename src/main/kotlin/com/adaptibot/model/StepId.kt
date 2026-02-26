package com.adaptibot.model

import kotlinx.serialization.Serializable

@Serializable
data class StepId(val value: String) {
    companion object {
        fun generate(): StepId = StepId("step_${System.nanoTime()}")
    }
}

