package com.adaptibot.common.model

import kotlinx.serialization.Serializable

@Serializable
data class Script(
    val name: String,
    val description: String = "",
    val steps: List<Step>,
    val settings: ScriptSettings = ScriptSettings()
)

@Serializable
data class ScriptSettings(
    val defaultDelayBefore: Long = 0,
    val defaultDelayAfter: Long = 0,
    val observerCheckDelay: Long = 1000,
    val defaultImageMatchThreshold: Double = 0.7
)

