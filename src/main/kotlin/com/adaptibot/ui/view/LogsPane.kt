package com.adaptibot.ui.view

import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

class LogsPane : BorderPane() {
    
    val logsTableView: TableView<LogEntry>
    val clearButton: Button
    
    init {
        val header = HBox(10.0).apply {
            style = "-fx-padding: 10;"
            children.addAll(
                Label("Execution Logs").apply {
                    style = "-fx-font-weight: bold;"
                    HBox.setHgrow(this, Priority.ALWAYS)
                },
                Button("Clear").also { clearButton = it }
            )
        }
        
        logsTableView = createLogsTable()
        
        top = header
        center = logsTableView
        
        VBox.setVgrow(this, Priority.ALWAYS)
    }
    
    private fun createLogsTable(): TableView<LogEntry> {
        return TableView<LogEntry>().apply {
            placeholder = Label("No logs yet")
            
            val timestampCol = TableColumn<LogEntry, String>("Timestamp").apply {
                prefWidth = 120.0
                setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.timestamp) }
            }
            
            val stepCol = TableColumn<LogEntry, String>("Step").apply {
                prefWidth = 250.0
                setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.stepName) }
            }
            
            val statusCol = TableColumn<LogEntry, String>("Status").apply {
                prefWidth = 120.0
                setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.status) }
            }
            
            val durationCol = TableColumn<LogEntry, String>("Duration").apply {
                prefWidth = 100.0
                setCellValueFactory { 
                    val duration = if (it.value.durationMs > 0) {
                        "${it.value.durationMs}ms"
                    } else {
                        "-"
                    }
                    javafx.beans.property.SimpleStringProperty(duration)
                }
            }
            
            val messageCol = TableColumn<LogEntry, String>("Message").apply {
                prefWidth = 350.0
                setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.message) }
            }
            
            columns.addAll(timestampCol, stepCol, statusCol, durationCol, messageCol)
        }
    }
}

data class LogEntry(
    val timestamp: String,
    val stepName: String,
    val status: String,
    val durationMs: Long,
    val message: String
)

