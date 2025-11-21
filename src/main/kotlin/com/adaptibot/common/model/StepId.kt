package com.adaptibot.common.model

import kotlinx.serialization.Serializable

@Serializable
data class StepId(val value: String) {
    companion object {
        fun generate(): StepId = StepId("step_${System.nanoTime()}")
    }
}

