package com.adaptibot.ui.view

import javafx.scene.control.Label
import javafx.scene.control.TreeCell
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.input.ClipboardContent
import javafx.scene.input.DataFormat
import javafx.scene.input.TransferMode
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.util.Callback

class ScriptEditorPane : BorderPane() {
    
    val stepsTreeView: TreeView<com.adaptibot.ui.model.StepNode>
    val contextMenu: javafx.scene.control.ContextMenu
    
    private val dragDataFormat = DataFormat("application/x-adaptibot-step")
    private var draggedItem: TreeItem<com.adaptibot.ui.model.StepNode>? = null
    
    var onStepMoved: ((
        stepId: com.adaptibot.common.model.StepId,
        targetContainerId: com.adaptibot.common.model.StepId,
        targetContainerType: com.adaptibot.ui.model.ContainerType,
        targetIndex: Int,
        parentBlockId: com.adaptibot.common.model.StepId?
    ) -> Unit)? = null
    
    init {
        val header = Label("Script Editor")
        header.style = "-fx-font-weight: bold; -fx-padding: 10;"
        
        stepsTreeView = TreeView()
        stepsTreeView.isShowRoot = true
        
        stepsTreeView.cellFactory = Callback { DraggableTreeCell() }
        
        contextMenu = createContextMenu()
        stepsTreeView.contextMenu = contextMenu
        
        top = header
        center = stepsTreeView
        
        VBox.setVgrow(this, Priority.ALWAYS)
    }
    
    private inner class DraggableTreeCell : TreeCell<com.adaptibot.ui.model.StepNode>() {
        
        init {
            setOnDragDetected { event ->
                val item = treeItem
                if (item != null && item.value != null && item != stepsTreeView.root) {
                    draggedItem = item
                    
                    val dragboard = startDragAndDrop(TransferMode.MOVE)
                    val content = ClipboardContent()
                    content.put(dragDataFormat, item.value.step.id.value)
                    dragboard.setContent(content)
                    
                    event.consume()
                }
            }
            
            setOnDragOver { event ->
                if (event.gestureSource != this && event.dragboard.hasContent(dragDataFormat)) {
                    val targetItem = treeItem
                    if (targetItem != null && draggedItem != null && !isDescendant(targetItem, draggedItem!!)) {
                        event.acceptTransferModes(TransferMode.MOVE)
                        
                        style = "-fx-border-color: #2196F3; -fx-border-width: 0 0 2 0;"
                    }
                }
                event.consume()
            }
            
            setOnDragExited { event ->
                style = ""
                event.consume()
            }
            
            setOnDragDropped { event ->
                val db = event.dragboard
                var success = false
                
                if (db.hasContent(dragDataFormat)) {
                    val targetItem = treeItem
                    val sourceItem = draggedItem
                    
                    if (targetItem != null && sourceItem != null && targetItem != sourceItem) {
                        val dropInfo = calculateDropTarget(targetItem)
                        if (dropInfo != null) {
                            onStepMoved?.invoke(
                                sourceItem.value.step.id,
                                dropInfo.containerId,
                                dropInfo.containerType,
                                dropInfo.index,
                                dropInfo.parentBlockId
                            )
                            success = true
                        }
                    }
                }
                
                event.isDropCompleted = success
                event.consume()
            }
            
            setOnDragDone { event ->
                draggedItem = null
                style = ""
                event.consume()
            }
        }
        
        override fun updateItem(item: com.adaptibot.ui.model.StepNode?, empty: Boolean) {
            super.updateItem(item, empty)
            if (empty || item == null) {
                text = null
                style = ""
            } else {
                text = "${item.icon} ${item.displayText}"
                
                style = when (item.step) {
                    is com.adaptibot.common.model.Step.GroupBlock -> 
                        "-fx-font-weight: bold; -fx-text-fill: #2e7d32;"
                    is com.adaptibot.common.model.Step.ConditionalBlock -> 
                        "-fx-font-weight: bold; -fx-text-fill: #1565c0;"
                    is com.adaptibot.common.model.Step.ObserverBlock -> 
                        "-fx-font-weight: bold; -fx-text-fill: #c62828;"
                    else -> ""
                }
            }
        }
    }
    
    private data class DropTargetInfo(
        val containerId: com.adaptibot.common.model.StepId,
        val containerType: com.adaptibot.ui.model.ContainerType,
        val index: Int,
        val parentBlockId: com.adaptibot.common.model.StepId?
    )
    
    private fun calculateDropTarget(targetItem: TreeItem<com.adaptibot.ui.model.StepNode>): DropTargetInfo? {
        val targetNode = targetItem.value ?: return null
        
        return if (targetNode.isContainer()) {
            DropTargetInfo(
                containerId = targetNode.step.id,
                containerType = targetNode.containerType,
                index = targetItem.children.size,
                parentBlockId = targetNode.parentBlockId
            )
        } else {
            val parent = targetItem.parent
            if (parent != null && parent.value != null) {
                val parentNode = parent.value
                val indexInParent = parent.children.indexOf(targetItem)
                DropTargetInfo(
                    containerId = parentNode.step.id,
                    containerType = parentNode.containerType,
                    index = indexInParent + 1,
                    parentBlockId = parentNode.parentBlockId
                )
            } else {
                null
            }
        }
    }
    
    private fun isDescendant(potentialDescendant: TreeItem<*>, potentialAncestor: TreeItem<*>): Boolean {
        var current: TreeItem<*>? = potentialDescendant
        while (current != null) {
            if (current == potentialAncestor) return true
            current = current.parent
        }
        return false
    }
    
    private fun createContextMenu(): javafx.scene.control.ContextMenu {
        return javafx.scene.control.ContextMenu().apply {
            val addMenu = javafx.scene.control.Menu("Add")
            
            val actionMenu = javafx.scene.control.Menu("Action")
            actionMenu.items.addAll(
                javafx.scene.control.MenuItem("Move Mouse").apply { id = "add-action-move" },
                javafx.scene.control.MenuItem("Left Click").apply { id = "add-action-left-click" },
                javafx.scene.control.MenuItem("Right Click").apply { id = "add-action-right-click" },
                javafx.scene.control.MenuItem("Double Click").apply { id = "add-action-double-click" },
                javafx.scene.control.SeparatorMenuItem(),
                javafx.scene.control.MenuItem("Type Text").apply { id = "add-action-type" },
                javafx.scene.control.MenuItem("Press Key").apply { id = "add-action-press-key" },
                javafx.scene.control.SeparatorMenuItem(),
                javafx.scene.control.MenuItem("Wait").apply { id = "add-action-wait" },
                javafx.scene.control.MenuItem("Jump To").apply { id = "add-action-jump" }
            )
            
            val blockMenu = javafx.scene.control.Menu("Block")
            blockMenu.items.addAll(
                javafx.scene.control.MenuItem("Group").apply { id = "add-block-group" },
                javafx.scene.control.MenuItem("Conditional (If/Else)").apply { id = "add-block-conditional" },
                javafx.scene.control.MenuItem("Observer").apply { id = "add-block-observer" }
            )
            
            addMenu.items.addAll(actionMenu, blockMenu)
            
            items.addAll(
                addMenu,
                javafx.scene.control.SeparatorMenuItem(),
                javafx.scene.control.MenuItem("Edit").apply { id = "edit" },
                javafx.scene.control.MenuItem("Delete").apply { id = "delete" },
                javafx.scene.control.SeparatorMenuItem(),
                javafx.scene.control.MenuItem("Copy").apply { id = "copy" },
                javafx.scene.control.MenuItem("Paste").apply { id = "paste" }
            )
        }
    }
}

