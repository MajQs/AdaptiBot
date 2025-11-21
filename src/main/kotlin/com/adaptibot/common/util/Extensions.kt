package com.adaptibot.common.util

import org.slf4j.Logger

inline fun <T> Logger.logExecution(operation: String, block: () -> T): T {
    debug("Starting: $operation")
    val startTime = System.currentTimeMillis()
    return try {
        block().also {
            val duration = System.currentTimeMillis() - startTime
            debug("Completed: $operation (${duration}ms)")
        }
    } catch (e: Exception) {
        error("Failed: $operation", e)
        throw e
    }
}

fun Long.toReadableDuration(): String {
    return when {
        this < 1000 -> "${this}ms"
        this < 60000 -> String.format("%.2fs", this / 1000.0)
        else -> String.format("%.2fm", this / 60000.0)
    }
}

