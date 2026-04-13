package com.adaptibot.ui.controller

import com.adaptibot.execution.ExecutionConfiguration
import com.adaptibot.script.step.BranchId
import com.adaptibot.script.step.StepId
import com.adaptibot.ui.adapter.UiExecutionEventPublisher
import com.adaptibot.ui.dialog.*
import com.adaptibot.ui.view.*
import com.adaptibot.ui.viewmodel.LogMessage
import com.adaptibot.ui.viewmodel.ScriptViewModel
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.SplitPane
import javafx.scene.layout.BorderPane
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.net.URL
import java.nio.file.Path
import java.util.ResourceBundle

class MainController : Initializable {

    @FXML
    private lateinit var rootPane: BorderPane

    private lateinit var viewModel: ScriptViewModel

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        // Build ViewModel with event publisher
        val eventPublisher = UiExecutionEventPublisher(null) // placeholder, set below
        val facade = ExecutionConfiguration.getFacade(eventPublisher)
        viewModel = ScriptViewModel(facade)
        // Wire publisher back to viewModel
        eventPublisher.attachViewModel(viewModel)

        // Build UI
        val toolbar = ToolbarView(
            viewModel = viewModel,
            onNew = ::onNew,
            onOpen = ::onOpen,
            onSave = ::onSave,
            onSettings = ::onSettings
        )

        val scriptPanel = ScriptPanel(
            viewModel = viewModel,
            onEditStep = { step ->
                val updated = StepEditorDialogFactory.show(step, window())
                updated?.let { viewModel.updateStep(it) }
            },
            onAddStep = { parentId, branchId, afterStepId, type ->
                showAddStepFlow(parentId, branchId, afterStepId, type)
            }
        )

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

        viewModel.addLog(LogMessage.info("AdaptiBot ready."))
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

    private fun showAddStepFlow(parentId: StepId?, branchId: BranchId?, afterStepId: StepId?, type: com.adaptibot.ui.dialog.StepType) {
        val newStep = StepEditorDialogFactory.showNew(type, window()) ?: return
        when {
            branchId    != null -> viewModel.addStepToBranch(branchId, newStep)
            parentId    != null -> viewModel.addStepToParent(parentId, newStep)
            afterStepId != null -> viewModel.addStepAfter(afterStepId, newStep)
            else                -> viewModel.addStep(newStep)
        }
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
}

