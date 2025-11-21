package com.adaptibot.ui.controller

import com.adaptibot.ui.service.ScriptService
import com.adaptibot.ui.service.ExecutionService
import com.adaptibot.ui.view.MainView
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.layout.BorderPane
import java.net.URL
import java.util.*

class MainController : Initializable {
    
    @FXML
    private lateinit var rootPane: BorderPane
    
    private lateinit var mainView: MainView
    private lateinit var scriptService: ScriptService
    private lateinit var executionService: ExecutionService
    
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        scriptService = ScriptService()
        executionService = ExecutionService()
        
        mainView = MainView()
        setupMenuHandlers()
        setupControlHandlers()
        setupLogsBinding()
        
        rootPane.center = mainView
    }
    
    private fun setupLogsBinding() {
        // Bind ExecutionLogger to LogsPane TableView
        mainView.logsPane.logsTableView.items = com.adaptibot.ui.model.ExecutionLogger.logs
    }
    
    private fun setupMenuHandlers() {
        with(mainView.menuBar.menus) {
            // File menu
            get(0).items[0].setOnAction { handleNewScript() }
            get(0).items[1].setOnAction { handleOpenScript() }
            get(0).items[2].setOnAction { handleSaveScript() }
            get(0).items[3].setOnAction { handleSaveScriptAs() }
            get(0).items[5].setOnAction { handleExit() }
            
            // Edit menu
            get(1).items[0].setOnAction { handleAddStep() }
            get(1).items[1].setOnAction { handleDeleteStep() }
            get(1).items[2].setOnAction { handleCopyStep() }
            get(1).items[3].setOnAction { handlePasteStep() }
            get(1).items[5].setOnAction { handleSettings() }
            
            // Run menu
            get(2).items[0].setOnAction { handleStart() }
            get(2).items[1].setOnAction { handlePause() }
            get(2).items[2].setOnAction { handleStop() }
            
            // Help menu
            get(3).items[0].setOnAction { handleDocumentation() }
            get(3).items[1].setOnAction { handleExamples() }
            get(3).items[3].setOnAction { handleAbout() }
        }
    }
    
    private fun setupControlHandlers() {
        with(mainView.controlToolBar.items) {
            (get(0) as javafx.scene.control.Button).setOnAction { handleStart() }
            (get(1) as javafx.scene.control.Button).setOnAction { handlePause() }
            (get(2) as javafx.scene.control.Button).setOnAction { handleStop() }
        }
        
        mainView.logsPane.clearButton.setOnAction { handleClearLogs() }
        
        // Double-click to edit step
        mainView.scriptEditorPane.stepsTreeView.setOnMouseClicked { event ->
            if (event.clickCount == 2) {
                val selectedItem = mainView.scriptEditorPane.stepsTreeView.selectionModel.selectedItem
                if (selectedItem != null && selectedItem.value != null) {
                    handleEditStep(selectedItem.value)
                }
            }
        }
    }
    
    private fun handleNewScript() {
        scriptService.createNewScript()
        updateUI()
    }
    
    private fun handleOpenScript() {
        scriptService.openScript()
        updateUI()
    }
    
    private fun handleSaveScript() {
        scriptService.saveScript()
    }
    
    private fun handleSaveScriptAs() {
        scriptService.saveScriptAs()
    }
    
    private fun handleExit() {
        javafx.application.Platform.exit()
    }
    
    private fun handleAddStep() {
        val dialog = com.adaptibot.ui.dialog.StepEditorDialog()
        val result = dialog.showAndWait()
        
        result.ifPresent { newStep ->
            scriptService.addStep(newStep)
            updateUI()
        }
    }
    
    private fun handleDeleteStep() {
        val selectedItem = mainView.scriptEditorPane.stepsTreeView.selectionModel.selectedItem
        if (selectedItem != null && selectedItem.value != null) {
            val stepNode = selectedItem.value
            scriptService.deleteStep(stepNode.step.id)
            updateUI()
        }
    }
    
    private fun handleCopyStep() {
        // TODO: Copy selected step
    }
    
    private fun handlePasteStep() {
        // TODO: Paste step from clipboard
    }
    
    private fun handleSettings() {
        // TODO: Open settings dialog
    }
    
    private fun handleStart() {
        val script = scriptService.getCurrentScript()
        if (script.steps.isEmpty()) {
            showAlert("No Steps", "The script has no steps to execute.")
            return
        }
        
        executionService.start(script)
        updateExecutionState()
        updateControlsState()
    }
    
    private fun handleEditStep(stepNode: com.adaptibot.ui.model.StepNode) {
        if (stepNode.step is com.adaptibot.common.model.Step.ActionStep) {
            val dialog = com.adaptibot.ui.dialog.StepEditorDialog(stepNode.step)
            val result = dialog.showAndWait()
            
            result.ifPresent { updatedStep ->
                scriptService.updateStep(stepNode.step.id, updatedStep)
                updateUI()
            }
        }
    }
    
    private fun handlePause() {
        executionService.pause()
        updateExecutionState()
        updateControlsState()
    }
    
    private fun handleStop() {
        executionService.stop()
        updateExecutionState()
        updateControlsState()
    }
    
    private fun handleClearLogs() {
        com.adaptibot.ui.model.ExecutionLogger.clear()
    }
    
    private fun handleDocumentation() {
        // TODO: Open documentation
    }
    
    private fun handleExamples() {
        // TODO: Load example scripts
    }
    
    private fun handleAbout() {
        // TODO: Show about dialog
    }
    
    private fun updateUI() {
        val script = scriptService.getCurrentScript()
        
        // Build and set tree
        val root = com.adaptibot.ui.view.ScriptTreeBuilder.buildTree(script.steps)
        mainView.scriptEditorPane.stepsTreeView.root = root
        
        updateControlsState()
    }
    
    private fun updateExecutionState() {
        val state = executionService.getState()
        val statusLabel = mainView.controlToolBar.items[4] as javafx.scene.control.Label
        statusLabel.text = "Status: ${state.name.lowercase().replaceFirstChar { it.uppercase() }}"
    }
    
    private fun updateControlsState() {
        val hasSteps = scriptService.getCurrentScript().steps.isNotEmpty()
        val isRunning = executionService.isRunning()
        val isPaused = executionService.isPaused()
        
        val startBtn = mainView.controlToolBar.items[0] as javafx.scene.control.Button
        val pauseBtn = mainView.controlToolBar.items[1] as javafx.scene.control.Button
        val stopBtn = mainView.controlToolBar.items[2] as javafx.scene.control.Button
        
        startBtn.isDisable = !hasSteps || isRunning
        pauseBtn.isDisable = !isRunning || isPaused
        stopBtn.isDisable = !isRunning && !isPaused
    }
    
    private fun showAlert(title: String, message: String) {
        val alert = javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
}
