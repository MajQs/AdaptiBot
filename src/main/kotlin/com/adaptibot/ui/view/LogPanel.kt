package com.adaptibot.ui.view

import com.adaptibot.ui.viewmodel.LogLevel
import com.adaptibot.ui.viewmodel.LogMessage
import com.adaptibot.ui.viewmodel.ScriptViewModel
import javafx.collections.ListChangeListener
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*

class LogPanel(private val viewModel: ScriptViewModel) : BorderPane() {

    private val listView = ListView<LogMessage>()

    init {
        styleClass.add("log-panel")

        val header = buildHeader()
        top = header

        listView.styleClass.add("log-list")
        listView.items = viewModel.logMessages
        listView.setCellFactory { LogCell() }
        listView.selectionModel.selectionMode = SelectionMode.SINGLE
        center = listView

        // Auto-scroll to bottom
        viewModel.logMessages.addListener(ListChangeListener {
            if (viewModel.logMessages.isNotEmpty()) {
                listView.scrollTo(viewModel.logMessages.size - 1)
            }
        })
    }

    private fun buildHeader(): HBox {
        val title = Label("EXECUTION LOG").apply { styleClass.add("panel-title") }
        val clearBtn = Button("Clear").apply {
            styleClass.add("toolbar-btn")
            style = "-fx-padding: 2 8 2 8; -fx-font-size: 11px;"
            setOnAction { viewModel.logMessages.clear() }
        }
        val spacer = Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }
        return HBox(8.0, title, spacer, clearBtn).apply {
            styleClass.add("panel-header")
            alignment = Pos.CENTER_LEFT
        }
    }
}

private class LogCell : ListCell<LogMessage>() {
    override fun updateItem(item: LogMessage?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
            text = null
        } else {
            val timeLabel = Label("[${item.time}]").apply {
                styleClass.addAll("log-entry", "log-time")
                minWidth = 70.0
            }
            val textClass = when (item.level) {
                LogLevel.INFO    -> "log-text-info"
                LogLevel.SUCCESS -> "log-text-success"
                LogLevel.ERROR   -> "log-text-error"
            }
            val msgLabel = Label(item.text).apply {
                styleClass.addAll("log-entry", textClass)
                isWrapText = false
            }
            graphic = HBox(6.0, timeLabel, msgLabel).apply {
                alignment = Pos.CENTER_LEFT
            }
            text = null
        }
    }
}

