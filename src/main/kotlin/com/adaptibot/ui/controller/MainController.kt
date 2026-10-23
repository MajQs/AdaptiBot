package com.adaptibot.ui.controller

import com.adaptibot.execution.ExecutionConfiguration
import com.adaptibot.hotkey.HotkeyConfiguration
import com.adaptibot.hotkey.HotkeyFacade
import com.adaptibot.infrastructure.PlatformCapabilities
import com.adaptibot.infrastructure.PlatformCapabilityDetector
import com.adaptibot.recorder.RecorderConfiguration
import com.adaptibot.recorder.RecorderFacade
import com.adaptibot.recorder.dto.RecordingDto
import com.adaptibot.recorder.dto.RecordingResultDto
import com.adaptibot.script.step.ContainerId
import com.adaptibot.script.step.StepId
import com.adaptibot.ui.adapter.RecordingToStepMapper
import com.adaptibot.ui.adapter.UiExecutionEventPublisher
import com.adaptibot.ui.dialog.*
import com.adaptibot.ui.view.*
import com.adaptibot.ui.viewmodel.LogMessage
import com.adaptibot.ui.viewmodel.ScriptViewModel
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Alert
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.SplitPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Region
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.net.URL
import java.nio.file.Path
import java.util.ResourceBundle

/** Where a finished recording is inserted; captured when the user picks the command, before the modal. */
private data class RecordingInsertionContext(val containerId: ContainerId, val afterStepId: StepId?)

class MainController : Initializable {

    @FXML
    private lateinit var rootPane: BorderPane

    private lateinit var viewModel: ScriptViewModel

    private var hotkeyFacade: HotkeyFacade? = null
    private var recorderFacade: RecorderFacade? = null

    /** Insertion location of the current recording session; set before the start modal is shown. */
    private var recordingInsertionContext: RecordingInsertionContext? = null
    private var stepTreeView: StepTreeView? = null
    private var isShuttingDown = false

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        // Build ViewModel with event publisher
        val eventPublisher = UiExecutionEventPublisher(null) // placeholder, set below
        val capabilities = PlatformCapabilityDetector.detect()
        val facade = ExecutionConfiguration.getFacade(eventPublisher, capabilities)
        viewModel = ScriptViewModel(facade, capabilities)
        recorderFacade = RecorderConfiguration.getFacade(capabilities)
        // Wire publisher back to viewModel
        eventPublisher.attachViewModel(viewModel)

        val stopHotkeyName = registerStopHotkey(capabilities)

        // Build UI
        val toolbar = ToolbarView(
            viewModel = viewModel,
            onNew = ::onNew,
            onOpen = ::onOpen,
            onSave = ::onSave,
            onSettings = ::onSettings,
            stopHotkeyName = stopHotkeyName
        )

        val scriptPanel = ScriptPanel(
            viewModel = viewModel,
            onEditStep = { step ->
                val updated = StepEditorDialogFactory.show(step, window())
                updated?.let { viewModel.updateStep(it) }
            },
            onAddStep = { containerId, afterStepId, type ->
                showAddStepFlow(containerId, afterStepId, type)
            }
        )

        stepTreeView = scriptPanel.treeView

        val logPanel = LogPanel(viewModel)

        val splitPane = SplitPane(scriptPanel, logPanel).apply {
            setDividerPositions(0.6)
            SplitPane.setResizableWithParent(scriptPanel, true)
            SplitPane.setResizableWithParent(logPanel, true)
        }

        rootPane.top    = toolbar
        rootPane.center = splitPane

        // Apply CSS
        rootPane.scene?.stylesheets?.add(
            javaClass.getResource("/css/adaptibot.css")?.toExternalForm() ?: ""
        )

        // Defer CSS until scene is available
        rootPane.sceneProperty().addListener { _, _, scene ->
            scene?.stylesheets?.add(
                javaClass.getResource("/css/adaptibot.css")?.toExternalForm() ?: ""
            )
        }

        viewModel.addLog(LogMessage.info(capabilitySummary(capabilities)))
        viewModel.addLog(LogMessage.info("AdaptiBot ready."))
    }

    /** One startup line: names every disabled feature, or confirms all are available. */
    private fun capabilitySummary(capabilities: PlatformCapabilities): String {
        val disabled = buildList {
            if (!capabilities.actionSupported) add("script execution of mouse/keyboard actions")
            if (!capabilities.recordingSupported) add("recording")
            if (!capabilities.globalHotkeySupported) add("global emergency-stop hotkey")
        }
        return if (disabled.isEmpty()) "Platform capabilities: all automation features available."
        else "Platform capabilities: editor only. Disabled features: ${disabled.joinToString(", ")}."
    }

    fun shutdown() {
        isShuttingDown = true
        hotkeyFacade?.close()
        hotkeyFacade = null
        recorderFacade?.close()
        recorderFacade = null
    }

    // ── File operations ────────────────────────────────────────────────────────

    private fun onNew() {
        if (viewModel.isDirtyProperty.get()) {
            val confirm = javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION,
                "Unsaved changes will be lost. Continue?",
                javafx.scene.control.ButtonType.YES,
                javafx.scene.control.ButtonType.CANCEL
            )
            confirm.initOwner(window())
            confirm.dialogPane.stylesheets.add(javaClass.getResource("/css/adaptibot.css")?.toExternalForm() ?: "")
            val result = confirm.showAndWait()
            if (result.orElse(javafx.scene.control.ButtonType.CANCEL) != javafx.scene.control.ButtonType.YES) return
        }
        viewModel.newScript()
    }

    private fun onOpen() {
        val file = openChooser().showOpenDialog(window()) ?: return
        try {
            viewModel.loadScript(file.toPath())
        } catch (e: Exception) {
            showError("Failed to open file", e.message ?: "Unknown error")
        }
    }

    private fun onSave() {
        val path = viewModel.currentFilePath ?: run {
            openChooser().showSaveDialog(window())?.toPath() ?: return
        }
        try {
            viewModel.saveScript(path)
        } catch (e: Exception) {
            showError("Failed to save file", e.message ?: "Unknown error")
        }
    }

    private fun onSettings() {
        val dialog = ScriptSettingsDialog(
            current = viewModel.getScriptSettings(),
            scriptName = viewModel.getScriptName(),
            scriptDescription = viewModel.getScriptDescription(),
            owner = window()
        )
        val result = dialog.showAndWait().orElse(null) ?: return
        viewModel.renameScript(result.name)
        viewModel.updateDescription(result.description)
        viewModel.isDirtyProperty.set(true)
    }

    // ── Step add flow ──────────────────────────────────────────────────────────

    private fun showAddStepFlow(containerId: ContainerId, afterStepId: StepId?, type: com.adaptibot.ui.dialog.StepType) {
        if (type == StepType.RECORDING) {
            startRecordingFlow(RecordingInsertionContext(containerId, afterStepId))
            return
        }
        val defaultDelayBefore = viewModel.getScriptSettings().defaultDelayBefore
        val newStep = StepEditorDialogFactory.showNew(type, window(), defaultDelayBefore) ?: return
        if (afterStepId != null) viewModel.addStepAfter(afterStepId, newStep)
        else viewModel.addStep(containerId, newStep)
    }

    // ── Recording flow ─────────────────────────────────────────────────────────

    /** Capture starts only after the modal has closed, so the click confirming it is never recorded. */
    private fun startRecordingFlow(context: RecordingInsertionContext) {
        if (!viewModel.canStartRecordingBinding.get()) return
        recordingInsertionContext = context
        if (!confirmRecordingStart()) {
            recordingInsertionContext = null
            return
        }
        val recorder = recorderFacade
        if (recorder == null || isShuttingDown || !viewModel.canStartRecordingBinding.get()) {
            recordingInsertionContext = null
            return
        }
        viewModel.isRecordingProperty.set(true)
        // The recorder completes on a background thread (or synchronously when capture cannot start).
        val started = recorder.start { result -> Platform.runLater { onRecordingFinished(result) } }
        if (!started) {
            viewModel.isRecordingProperty.set(false)
            recordingInsertionContext = null
            showRecordingFailure()
        }
    }

    private fun confirmRecordingStart(): Boolean {
        val startButton = ButtonType("Start recording", ButtonBar.ButtonData.OK_DONE)
        val cancelButton = ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
        val dialog = Alert(Alert.AlertType.CONFIRMATION, RECORDING_SCOPE_MESSAGE, startButton, cancelButton)
        dialog.title = "Recording"
        dialog.headerText = "Mouse click recording"
        dialog.initOwner(window())
        dialog.dialogPane.minHeight = Region.USE_PREF_SIZE
        dialog.dialogPane.stylesheets.add(javaClass.getResource("/css/adaptibot.css")?.toExternalForm() ?: "")
        return dialog.showAndWait().orElse(cancelButton) == startButton
    }

    /** Runs on the JavaFX thread once per session; always restores the normal UI first. */
    private fun onRecordingFinished(result: RecordingResultDto) {
        viewModel.isRecordingProperty.set(false)
        if (isShuttingDown) return
        when (result) {
            is RecordingResultDto.Completed -> insertRecording(result.recording)
            RecordingResultDto.Failed -> {
                recordingInsertionContext = null
                showRecordingFailure()
            }
        }
    }

    /** Inserts a completed recording at the captured location; the context is always consumed. */
    private fun insertRecording(recording: RecordingDto) {
        val context = recordingInsertionContext
        recordingInsertionContext = null
        if (recording.events.isEmpty()) {
            viewModel.addLog(LogMessage.info(RECORDING_EMPTY_MESSAGE))
            return
        }
        val groupStep = RecordingToStepMapper.toGroupStep(recording)
        val inserted = context != null &&
            viewModel.insertStep(context.containerId, context.afterStepId, groupStep)
        if (inserted) {
            stepTreeView?.revealStep(groupStep.id)
        } else {
            showRecordingFailure()
        }
    }

    /** The single user-facing recording failure: one ERROR log entry and one dialog. */
    private fun showRecordingFailure() {
        viewModel.addLog(LogMessage.error(RECORDING_FAILED_MESSAGE))
        showError("Recording", RECORDING_FAILED_MESSAGE)
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun window(): Stage? = rootPane.scene?.window as? Stage

    private fun openChooser() = FileChooser().apply {
        title = "AdaptiBot Script"
        extensionFilters.add(FileChooser.ExtensionFilter("AdaptiBot Script (*.json)", "*.json"))
    }

    private fun showError(title: String, message: String) {
        val alert = javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, message)
        alert.title = title
        alert.initOwner(window())
        alert.dialogPane.stylesheets.add(javaClass.getResource("/css/adaptibot.css")?.toExternalForm() ?: "")
        alert.showAndWait()
    }

    private fun registerStopHotkey(capabilities: PlatformCapabilities): String? {
        val hotkeys = HotkeyConfiguration.getFacade(capabilities)
        val recorder = checkNotNull(recorderFacade)
        hotkeyFacade = hotkeys

        val registered = hotkeys.registerStopHotkey {
            if (recorder.isRecording()) {
                recorder.stop()
            } else {
                Platform.runLater { viewModel.stopExecutionByHotkey(hotkeys.stopHotkeyName) }
            }
        }
        viewModel.isStopHotkeyAvailableProperty.set(registered)

        if (!registered) {
            // On unsupported platforms the startup capability summary already explains this.
            if (capabilities.globalHotkeySupported) {
                viewModel.addLog(
                    LogMessage.error(ScriptViewModel.RECORDING_HOTKEY_UNAVAILABLE_MESSAGE)
                )
            }
            return null
        }

        viewModel.addLog(LogMessage.info("Emergency stop shortcut: ${hotkeys.stopHotkeyName}"))
        return hotkeys.stopHotkeyName
    }

    private companion object {
        const val RECORDING_SCOPE_MESSAGE =
            "Only physical single left mouse clicks and their screen coordinates are recorded.\n" +
            "Clicks inside AdaptiBot are recorded as well.\n" +
            "Injected events, keyboard input, mouse movement, other mouse buttons, and gestures are ignored.\n" +
            "Press Ctrl+Shift+F12 to stop recording."
        const val RECORDING_EMPTY_MESSAGE = "Recording finished: no clicks were recorded."
        const val RECORDING_FAILED_MESSAGE = "Recording could not be completed. Please try again."
    }
}
