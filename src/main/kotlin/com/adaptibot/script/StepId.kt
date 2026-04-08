package com.adaptibot.script

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class StepId(val value: String) {
    companion object {
        fun generate(): StepId = StepId(UUID.randomUUID().toString())
    }
}

