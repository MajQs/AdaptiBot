package com.adaptibot.ui.view

import com.adaptibot.common.model.StepId
import com.adaptibot.ui.model.ContainerType
import com.adaptibot.ui.model.StepNode
import javafx.scene.control.*
import javafx.scene.input.ClipboardContent
import javafx.scene.input.DataFormat
import javafx.scene.input.TransferMode
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.util.Callback

class ScriptEditorPane : BorderPane() {

    val stepsTreeView: TreeView<StepNode>
    val contextMenu: ContextMenu

    private val dragDataFormat = DataFormat("application/x-adaptibot-step")
    private var draggedItem: TreeItem<StepNode>? = null

    var onStepMoved: ((
        stepId: StepId,
        targetContainerId: StepId,
        targetContainerType: ContainerType,
        targetIndex: Int,
        parentBlockId: StepId?
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

    private inner class DraggableTreeCell : TreeCell<StepNode>() {

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
        }

        override fun updateItem(item: StepNode?, empty: Boolean) {
            super.updateItem(item, empty)
            if (empty || item == null) {
                text = null
                graphic = null
            } else {
                text = "${item.icon} ${item.displayText}"
            }
        }
    }

    private fun createContextMenu(): ContextMenu {
        val editItem = MenuItem("Edit")
        editItem.id = "edit"

        val deleteItem = MenuItem("Delete")
        deleteItem.id = "delete"

        val copyItem = MenuItem("Copy")
        copyItem.id = "copy"

        val pasteItem = MenuItem("Paste")
        pasteItem.id = "paste"

        val addMenu = Menu("Add")
        val addActionMenu = Menu("Action")
        addActionMenu.items.addAll(
            MenuItem("Move Mouse").apply { id = "add-action-move" },
            MenuItem("Left Click").apply { id = "add-action-left-click" },
            MenuItem("Right Click").apply { id = "add-action-right-click" },
            MenuItem("Double Click").apply { id = "add-action-double-click" },
            MenuItem("Type Text").apply { id = "add-action-type" },
            MenuItem("Press Key").apply { id = "add-action-press-key" },
            MenuItem("Wait").apply { id = "add-action-wait" },
            MenuItem("Jump to Label").apply { id = "add-action-jump" }
        )

        val addBlockMenu = Menu("Block")
        addBlockMenu.items.addAll(
            MenuItem("Group Block").apply { id = "add-block-group" },
            MenuItem("Conditional Block").apply { id = "add-block-conditional" },
            MenuItem("Observer Step").apply { id = "add-block-observer" }
        )
        addMenu.items.addAll(addActionMenu, addBlockMenu)

        return ContextMenu(addMenu, SeparatorMenuItem(), editItem, deleteItem, copyItem, pasteItem)
    }

    private fun isDescendant(parent: TreeItem<StepNode>, child: TreeItem<StepNode>): Boolean {
        var current = parent
        while (current.parent != null) {
            if (current.parent == child) {
                return true
            }
            current = current.parent
        }
        return false
    }

    private data class DropTargetInfo(
        val containerId: StepId,
        val containerType: ContainerType,
        val index: Int,
        val parentBlockId: StepId?
    )

    private fun calculateDropTarget(targetItem: TreeItem<StepNode>): DropTargetInfo? {
        val targetNode = targetItem.value ?: return null

        return if (targetNode.isContainer()) {
            DropTargetInfo(targetNode.step.id, targetNode.containerType, targetItem.children.size, targetNode.parentBlockId)
        } else {
            val parentItem = targetItem.parent ?: return null
            val parentNode = parentItem.value ?: return null
            val indexInParent = parentItem.children.indexOf(targetItem)
            DropTargetInfo(parentNode.step.id, parentNode.containerType, indexInParent, parentNode.parentBlockId)
        }
    }
}
