package com.adaptibot.ui.viewmodel

import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class LogLevel { INFO, SUCCESS, WARNING, ERROR }

data class LogMessage(
    val level: LogLevel,
    val text: String,
    val time: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
) {
    companion object {
        fun info(text: String) = LogMessage(LogLevel.INFO, text)
        fun success(text: String) = LogMessage(LogLevel.SUCCESS, text)
        fun warning(text: String) = LogMessage(LogLevel.WARNING, text)
        fun error(text: String) = LogMessage(LogLevel.ERROR, text)
    }
}

