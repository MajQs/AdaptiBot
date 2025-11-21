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
        
        rootPane.center = mainView
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
        // TODO: Open add step dialog
    }
    
    private fun handleDeleteStep() {
        // TODO: Delete selected step
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
        executionService.start()
        updateExecutionState()
    }
    
    private fun handlePause() {
        executionService.pause()
        updateExecutionState()
    }
    
    private fun handleStop() {
        executionService.stop()
        updateExecutionState()
    }
    
    private fun handleClearLogs() {
        mainView.logsPane.logsTableView.items.clear()
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
        mainView.scriptEditorPane.stepsTreeView.root.children.clear()
        // TODO: Populate tree view with script steps
    }
    
    private fun updateExecutionState() {
        val state = executionService.getState()
        val statusLabel = mainView.controlToolBar.items[4] as javafx.scene.control.Label
        statusLabel.text = "Status: ${state.name.lowercase().replaceFirstChar { it.uppercase() }}"
    }
}
