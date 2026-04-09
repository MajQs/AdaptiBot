package com.adaptibot.script

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ScriptId(val value: String = UUID.randomUUID().toString())

