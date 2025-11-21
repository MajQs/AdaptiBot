package com.adaptibot.common.config

import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val defaultScriptSettings: ScriptConfigDefaults = ScriptConfigDefaults(),
    val ui: UiConfig = UiConfig(),
    val logging: LoggingConfig = LoggingConfig()
)

@Serializable
data class ScriptConfigDefaults(
    val delayBefore: Long = 0,
    val delayAfter: Long = 0,
    val observerCheckDelay: Long = 1000,
    val imageMatchThreshold: Double = 0.7
)

@Serializable
data class UiConfig(
    val windowWidth: Double = 1200.0,
    val windowHeight: Double = 800.0,
    val editorLogsSplitRatio: Double = 0.6
)

@Serializable
data class LoggingConfig(
    val maxLogEntries: Int = 1000,
    val logToFile: Boolean = true
)

