package com.adaptibot.script.step

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class StepId(val value: String = UUID.randomUUID().toString())

