package com.adaptibot.ui.model

import com.adaptibot.ui.view.LogEntry
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ExecutionLogger {
    
    private const val MAX_LOG_ENTRIES = 1000
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    
    val logs: ObservableList<LogEntry> = FXCollections.observableArrayList()
    
    fun logStepStart(stepName: String) {
        addLog(LogEntry(
            timestamp = formatTimestamp(),
            stepName = stepName,
            status = "▶ Running",
            durationMs = 0,
            message = "Started"
        ))
    }
    
    fun logStepSuccess(stepName: String, durationMs: Long) {
        addLog(LogEntry(
            timestamp = formatTimestamp(),
            stepName = stepName,
            status = "✓ Success",
            durationMs = durationMs,
            message = "Completed"
        ))
    }
    
    fun logStepFailure(stepName: String, durationMs: Long, error: String) {
        addLog(LogEntry(
            timestamp = formatTimestamp(),
            stepName = stepName,
            status = "✗ Failed",
            durationMs = durationMs,
            message = error
        ))
    }
    
    fun logExecutionStart(scriptName: String) {
        addLog(LogEntry(
            timestamp = formatTimestamp(),
            stepName = "Script",
            status = "▶ Started",
            durationMs = 0,
            message = "Executing: $scriptName"
        ))
    }
    
    fun logExecutionStop() {
        addLog(LogEntry(
            timestamp = formatTimestamp(),
            stepName = "Script",
            status = "⏹ Stopped",
            durationMs = 0,
            message = "Execution stopped"
        ))
    }
    
    fun logExecutionPause() {
        addLog(LogEntry(
            timestamp = formatTimestamp(),
            stepName = "Script",
            status = "⏸ Paused",
            durationMs = 0,
            message = "Execution paused"
        ))
    }
    
    fun logExecutionResume() {
        addLog(LogEntry(
            timestamp = formatTimestamp(),
            stepName = "Script",
            status = "▶ Resumed",
            durationMs = 0,
            message = "Execution resumed"
        ))
    }
    
    fun clear() {
        Platform.runLater {
            logs.clear()
        }
    }
    
    private fun addLog(entry: LogEntry) {
        Platform.runLater {
            // Remove oldest entry if limit reached
            if (logs.size >= MAX_LOG_ENTRIES) {
                logs.removeAt(0)
            }
            logs.add(entry)
        }
    }
    
    private fun formatTimestamp(): String {
        return LocalDateTime.now().format(timeFormatter)
    }
}

