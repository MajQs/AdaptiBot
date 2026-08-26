package com.adaptibot.ui.view

import com.adaptibot.ui.viewmodel.ScriptViewModel
import javafx.beans.binding.Bindings
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.shape.Circle

class ToolbarView(
    private val viewModel: ScriptViewModel,
    private val onNew: () -> Unit,
    private val onOpen: () -> Unit,
    private val onSave: () -> Unit,
    private val onSettings: () -> Unit,
    private val stopHotkeyName: String? = null
) : HBox() {

    val runButton = Button("▶  Run")
    val stopButton = Button("⏹  Stop")

    init {
        styleClass.add("toolbar-area")
        alignment = Pos.CENTER_LEFT
        spacing = 8.0

        // File operations
        val newBtn = Button("New").apply {
            styleClass.add("toolbar-btn")
            setOnAction { onNew() }
        }
        val openBtn = Button("Open").apply {
            styleClass.add("toolbar-btn")
            setOnAction { onOpen() }
        }
        val saveBtn = Button("Save").apply {
            styleClass.add("toolbar-btn")
            setOnAction { onSave() }
        }

        val sep1 = buildSeparator()

        // Script name field
        val nameField = TextField().apply {
            styleClass.add("script-name-field")
            prefWidth = 220.0
            promptText = "Script name…"
            textProperty().bindBidirectional(viewModel.scriptNameProperty)
        }

        val sep2 = buildSeparator()

        // Run / Stop
        runButton.apply {
            styleClass.add("toolbar-btn-primary")
            disableProperty().bind(viewModel.isRunningProperty)
            setOnAction { viewModel.startExecution() }
        }
        stopButton.apply {
            styleClass.add("toolbar-btn-danger")
            disableProperty().bind(Bindings.not(viewModel.isRunningProperty))
            setOnAction { viewModel.stopExecution() }
            stopHotkeyName?.let { shortcut ->
                graphic = Label(shortcut).apply { styleClass.add("hotkey-hint") }
                contentDisplay = ContentDisplay.BOTTOM
                graphicTextGap = 0.0
                tooltip = Tooltip("Stop the script  ($shortcut)")
            }
        }

        val sep3 = buildSeparator()

        val settingsBtn = Button("⚙  Settings").apply {
            styleClass.add("toolbar-btn")
            setOnAction { onSettings() }
        }

        val spacer = Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }

        // Status indicator
        val statusDot = Circle(5.0).apply {
            styleClass.add("status-dot-idle")
            viewModel.isRunningProperty.addListener { _, _, running ->
                styleClass.setAll(if (running) "status-dot-running" else "status-dot-idle")
            }
        }
        val statusLabel = Label("Idle").apply {
            styleClass.add("status-text")
            viewModel.isRunningProperty.addListener { _, _, running ->
                text = if (running) "Running…" else "Idle"
                styleClass.setAll(if (running) "status-running" else "status-text")
            }
        }
        val statusBox = HBox(6.0, statusDot, statusLabel).apply { alignment = Pos.CENTER }

        children.addAll(newBtn, openBtn, saveBtn, sep1, nameField, sep2, runButton, stopButton, sep3, settingsBtn, spacer, statusBox)
    }

    private fun buildSeparator(): Region = Region().apply {
        styleClass.add("toolbar-separator")
        prefWidth = 1.0
        minHeight = 20.0
        maxHeight = 20.0
    }
}

