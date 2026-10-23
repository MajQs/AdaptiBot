package com.adaptibot.ui.view

import com.adaptibot.ui.viewmodel.ScriptViewModel
import javafx.beans.binding.Bindings
import javafx.geometry.Pos
import javafx.scene.Node
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

    private val statusDot = Circle(5.0).apply { styleClass.add("status-dot-idle") }
    private val statusLabel = Label("Idle").apply { styleClass.add("status-text") }

    init {
        styleClass.add("toolbar-area")
        alignment = Pos.CENTER_LEFT
        spacing = 8.0

        // File operations
        val newBtn = Button("New").apply {
            styleClass.add("toolbar-btn")
            disableProperty().bind(viewModel.isRecordingProperty)
            setOnAction { onNew() }
        }
        val openBtn = Button("Open").apply {
            styleClass.add("toolbar-btn")
            disableProperty().bind(viewModel.isRecordingProperty)
            setOnAction { onOpen() }
        }
        val saveBtn = Button("Save").apply {
            styleClass.add("toolbar-btn")
            disableProperty().bind(viewModel.isRecordingProperty)
            setOnAction { onSave() }
        }

        val sep1 = buildSeparator()

        // Script name field
        val nameField = TextField().apply {
            styleClass.add("script-name-field")
            prefWidth = 220.0
            promptText = "Script name…"
            textProperty().bindBidirectional(viewModel.scriptNameProperty)
            disableProperty().bind(viewModel.isRecordingProperty)
        }

        val sep2 = buildSeparator()

        // Run / Stop
        runButton.apply {
            styleClass.add("toolbar-btn-primary")
            disableProperty().bind(viewModel.isRunningProperty.or(viewModel.isRecordingProperty).or(viewModel.runAvailableProperty.not()))
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
            disableProperty().bind(viewModel.isRecordingProperty)
            setOnAction { onSettings() }
        }

        val spacer = Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }

        // Status indicator
        viewModel.isRunningProperty.addListener { _, _, _ -> refreshStatus() }
        viewModel.isRecordingProperty.addListener { _, _, _ -> refreshStatus() }
        val statusBox = HBox(6.0, statusDot, statusLabel).apply { alignment = Pos.CENTER }

        children.addAll(newBtn, openBtn, saveBtn, sep1, nameField, sep2, runEntry(), stopButton, sep3, settingsBtn, spacer, statusBox)
    }

    /**
     * Disabled controls receive no mouse events, so the unavailable-platform tooltip sits on a wrapper.
     * Platform capabilities are fixed at startup, so a static tooltip suffices; supported platforms keep the bare button.
     */
    private fun runEntry(): Node =
        if (viewModel.runAvailableProperty.get()) runButton
        else StackPane(runButton).apply { Tooltip.install(this, Tooltip(RUN_UNAVAILABLE_MESSAGE)) }

    /** Recording takes precedence: it can only be active while no script runs. */
    private fun refreshStatus() {
        when {
            viewModel.isRecordingProperty.get() -> {
                statusLabel.text = stopHotkeyName?.let { "Recording… ($it)" } ?: "Recording…"
                statusLabel.styleClass.setAll("status-recording")
                statusDot.styleClass.setAll("status-dot-recording")
            }
            viewModel.isRunningProperty.get() -> {
                statusLabel.text = "Running…"
                statusLabel.styleClass.setAll("status-running")
                statusDot.styleClass.setAll("status-dot-running")
            }
            else -> {
                statusLabel.text = "Idle"
                statusLabel.styleClass.setAll("status-text")
                statusDot.styleClass.setAll("status-dot-idle")
            }
        }
    }

    private fun buildSeparator(): Region = Region().apply {
        styleClass.add("toolbar-separator")
        prefWidth = 1.0
        minHeight = 20.0
        maxHeight = 20.0
    }

    private companion object {
        const val RUN_UNAVAILABLE_MESSAGE = "Running mouse and keyboard actions is unavailable on this platform."
    }
}
