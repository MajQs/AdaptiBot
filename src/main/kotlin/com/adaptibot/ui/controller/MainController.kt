package com.adaptibot.ui.controller

import com.adaptibot.ui.dialog.StepEditorDialog
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
            get(1).items[1].setOnAction { handleAddGroupBlock() }
            get(1).items[2].setOnAction { handleAddConditionalBlock() }
            get(1).items[3].setOnAction { handleAddObserverBlock() }
            get(1).items[4].setOnAction { handleDeleteStep() }
            get(1).items[5].setOnAction { handleCopyStep() }
            get(1).items[6].setOnAction { handlePasteStep() }
            get(1).items[8].setOnAction { handleSettings() }
            
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
        
        // Context menu handlers
        setupContextMenuHandlers()
    }
    
    private fun setupContextMenuHandlers() {
        val contextMenu = mainView.scriptEditorPane.contextMenu
        
        contextMenu.items.forEach { item ->
            when (item) {
                is javafx.scene.control.Menu -> {
                    item.items.forEach { subItem ->
                        when (subItem) {
                            is javafx.scene.control.Menu -> {
                                subItem.items.forEach { actionItem ->
                                    when (actionItem.id) {
                                        "add-action-move" -> actionItem.setOnAction { handleAddActionToContainer(StepEditorDialog.ActionType.MOUSE_MOVE) }
                                        "add-action-left-click" -> actionItem.setOnAction { handleAddActionToContainer(StepEditorDialog.ActionType.MOUSE_LEFT_CLICK) }
                                        "add-action-right-click" -> actionItem.setOnAction { handleAddActionToContainer(StepEditorDialog.ActionType.MOUSE_RIGHT_CLICK) }
                                        "add-action-double-click" -> actionItem.setOnAction { handleAddActionToContainer(StepEditorDialog.ActionType.MOUSE_DOUBLE_CLICK) }
                                        "add-action-type" -> actionItem.setOnAction { handleAddActionToContainer(StepEditorDialog.ActionType.KEYBOARD_TYPE) }
                                        "add-action-press-key" -> actionItem.setOnAction { handleAddActionToContainer(StepEditorDialog.ActionType.KEYBOARD_PRESS_KEY) }
                                        "add-action-wait" -> actionItem.setOnAction { handleAddActionToContainer(StepEditorDialog.ActionType.WAIT) }
                                        "add-action-jump" -> actionItem.setOnAction { handleAddActionToContainer(StepEditorDialog.ActionType.JUMP_TO_LABEL) }
                                        "add-block-group" -> actionItem.setOnAction { handleAddBlockToContainer("group") }
                                        "add-block-conditional" -> actionItem.setOnAction { handleAddBlockToContainer("conditional") }
                                        "add-block-observer" -> actionItem.setOnAction { handleAddBlockToContainer("observer") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            when (item.id) {
                "edit" -> item.setOnAction { 
                    val selected = mainView.scriptEditorPane.stepsTreeView.selectionModel.selectedItem
                    if (selected?.value != null) handleEditStep(selected.value)
                }
                "delete" -> item.setOnAction { handleDeleteStep() }
                "copy" -> item.setOnAction { handleCopyStep() }
                "paste" -> item.setOnAction { handlePasteStep() }
            }
        }
        
        contextMenu.setOnShowing {
            val selected = mainView.scriptEditorPane.stepsTreeView.selectionModel.selectedItem
            val isContainerSelected = selected?.value?.isContainer() == true
            
            val addMenu = contextMenu.items.find { it is javafx.scene.control.Menu && it.text == "Add" }
            addMenu?.isVisible = isContainerSelected
            contextMenu.items.find { it.id == "paste" }?.isDisable = (clipboardStep == null)
        }
        
        mainView.scriptEditorPane.onStepMoved = { stepId, targetContainerId, targetContainerType, targetIndex, parentBlockId ->
            scriptService.moveStep(stepId, targetContainerId, targetContainerType, targetIndex, parentBlockId)
            updateUI()
        }
    }
    
    private fun handleAddActionToContainer(actionType: com.adaptibot.ui.dialog.StepEditorDialog.ActionType) {
        val selectedItem = mainView.scriptEditorPane.stepsTreeView.selectionModel.selectedItem
        val selectedNode = selectedItem?.value
        
        if (selectedNode != null && selectedNode.isContainer()) {
            val dialog = com.adaptibot.ui.dialog.StepEditorDialog()
            dialog.setInitialActionType(actionType)
            val result = dialog.showAndWait()
            
            result.ifPresent { newStep ->
                scriptService.addStepToContainer(
                    selectedNode.step.id,
                    selectedNode.containerType,
                    newStep,
                    selectedNode.parentBlockId
                )
                updateUI()
            }
        }
    }
    
    private fun handleAddBlockToContainer(blockType: String) {
        val selectedItem = mainView.scriptEditorPane.stepsTreeView.selectionModel.selectedItem
        val selectedNode = selectedItem?.value
        
        if (selectedNode != null && selectedNode.isContainer()) {
            val result = when (blockType) {
                "group" -> {
                    val dialog = com.adaptibot.ui.dialog.GroupBlockEditorDialog()
                    dialog.showAndWait()
                }
                "conditional" -> {
                    val dialog = com.adaptibot.ui.dialog.ConditionalBlockEditorDialog()
                    dialog.showAndWait()
                }
                "observer" -> {
                    val dialog = com.adaptibot.ui.dialog.ObserverBlockEditorDialog()
                    dialog.showAndWait()
                }
                else -> return
            }
            
            result.ifPresent { newBlock ->
                scriptService.addStepToContainer(
                    selectedNode.step.id,
                    selectedNode.containerType,
                    newBlock,
                    selectedNode.parentBlockId
                )
                updateUI()
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
    
    private fun handleAddGroupBlock() {
        val dialog = com.adaptibot.ui.dialog.GroupBlockEditorDialog()
        val result = dialog.showAndWait()
        
        result.ifPresent { newGroup ->
            scriptService.addStep(newGroup)
            updateUI()
        }
    }
    
    private fun handleAddConditionalBlock() {
        val dialog = com.adaptibot.ui.dialog.ConditionalBlockEditorDialog()
        val result = dialog.showAndWait()
        
        result.ifPresent { newBlock ->
            scriptService.addStep(newBlock)
            updateUI()
            showInfo("Block Added", "Conditional block added successfully")
        }
    }
    
    private fun handleAddObserverBlock() {
        val dialog = com.adaptibot.ui.dialog.ObserverBlockEditorDialog()
        val result = dialog.showAndWait()
        
        result.ifPresent { newBlock ->
            scriptService.addStep(newBlock)
            updateUI()
            showInfo("Block Added", "Observer block added successfully")
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
    
    private var clipboardStep: com.adaptibot.common.model.Step? = null
    
    private fun handleCopyStep() {
        val selectedItem = mainView.scriptEditorPane.stepsTreeView.selectionModel.selectedItem
        if (selectedItem != null && selectedItem.value != null) {
            val stepNode = selectedItem.value
            clipboardStep = scriptService.copyStep(stepNode.step.id)
            if (clipboardStep != null) {
                showInfo("Copied", "Step copied to clipboard")
            }
        }
    }
    
    private fun handlePasteStep() {
        if (clipboardStep == null) {
            showAlert("Nothing to Paste", "No step in clipboard. Copy a step first.")
            return
        }
        
        val selectedItem = mainView.scriptEditorPane.stepsTreeView.selectionModel.selectedItem
        val targetGroupId = if (selectedItem != null && selectedItem.value.step is com.adaptibot.common.model.Step.GroupBlock) {
            selectedItem.value.step.id
        } else {
            null
        }
        
        scriptService.pasteStep(clipboardStep!!, targetGroupId)
        updateUI()
        showInfo("Pasted", "Step pasted successfully")
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
        when (val step = stepNode.step) {
            is com.adaptibot.common.model.Step.ActionStep -> {
                val dialog = com.adaptibot.ui.dialog.StepEditorDialog(step)
                val result = dialog.showAndWait()
                
                result.ifPresent { updatedStep ->
                    scriptService.updateStep(step.id, updatedStep)
                    updateUI()
                }
            }
            is com.adaptibot.common.model.Step.GroupBlock -> {
                val dialog = com.adaptibot.ui.dialog.GroupBlockEditorDialog(step)
                val result = dialog.showAndWait()
                
                result.ifPresent { updatedGroup ->
                    scriptService.updateStep(step.id, updatedGroup)
                    updateUI()
                }
            }
            is com.adaptibot.common.model.Step.ConditionalBlock -> {
                val dialog = com.adaptibot.ui.dialog.ConditionalBlockEditorDialog(step)
                val result = dialog.showAndWait()
                
                result.ifPresent { updatedBlock ->
                    scriptService.updateStep(step.id, updatedBlock)
                    updateUI()
                }
            }
            is com.adaptibot.common.model.Step.ObserverBlock -> {
                val dialog = com.adaptibot.ui.dialog.ObserverBlockEditorDialog(step)
                val result = dialog.showAndWait()
                
                result.ifPresent { updatedBlock ->
                    scriptService.updateStep(step.id, updatedBlock)
                    updateUI()
                }
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
        val alert = javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
    
    private fun showInfo(title: String, message: String) {
        val alert = javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
}
