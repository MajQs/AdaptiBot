package com.adaptibot.script.step

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ContainerId(val value: String = UUID.randomUUID().toString())

