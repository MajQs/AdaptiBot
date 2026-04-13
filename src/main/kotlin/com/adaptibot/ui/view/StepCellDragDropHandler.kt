package com.adaptibot.ui.view

import com.adaptibot.script.step.ContainerId
import com.adaptibot.script.step.Step
import com.adaptibot.script.step.StepId
import com.adaptibot.script.step.containers
import com.adaptibot.ui.util.StepDragData
import com.adaptibot.ui.viewmodel.ScriptViewModel
import javafx.scene.control.TreeCell
import javafx.scene.input.*

/**
 * Handles drag-and-drop reordering for a single [TreeCell<TreeNode>].
 *
 * - Drag source : only [TreeNode.StepNode] cells (ContainerNode cannot be dragged).
 * - Drag target : accepts a step id and moves it to the target position.
 */
class StepCellDragDropHandler(
    private val cell: TreeCell<TreeNode>,
    private val viewModel: ScriptViewModel
) {

    fun install() {
        cell.setOnDragDetected { e ->
            val node = cell.item as? TreeNode.StepNode ?: return@setOnDragDetected
            val db = cell.startDragAndDrop(TransferMode.MOVE)
            val content = ClipboardContent()
            content[StepDragData.DATA_FORMAT] = node.step.id.value
            db.setContent(content)
            db.dragView = cell.snapshot(null, null)
            e.consume()
        }

        cell.setOnDragOver { e ->
            if (e.gestureSource != cell && e.dragboard.hasContent(StepDragData.DATA_FORMAT)) {
                e.acceptTransferModes(TransferMode.MOVE)
                cell.graphic?.let { g ->
                    if (!g.styleClass.contains("step-cell-drag-over"))
                        g.styleClass.add("step-cell-drag-over")
                }
            }
            e.consume()
        }

        cell.setOnDragExited {
            cell.graphic?.styleClass?.remove("step-cell-drag-over")
        }

        cell.setOnDragDropped { e ->
            val db = e.dragboard
            var success = false
            if (db.hasContent(StepDragData.DATA_FORMAT)) {
                val draggedId = StepId(db.getContent(StepDragData.DATA_FORMAT) as String)

                when (val targetNode = cell.item) {
                    is TreeNode.ContainerNode -> {
                        // Dropped onto a container header → append to that container
                        if (draggedId != targetNode.parentStepId) {
                            viewModel.moveStepToContainer(draggedId, targetNode.container.id)
                            success = true
                        }
                    }
                    is TreeNode.StepNode -> {
                        val targetStep = targetNode.step
                        if (draggedId != targetStep.id) {
                            val parentItem      = cell.treeItem?.parent
                            val targetContainerId = resolveParentContainerId(parentItem)
                            val siblings        = resolveSiblingList(parentItem, targetContainerId)
                            val targetIndex     = siblings.indexOfFirst { it.id == targetStep.id }
                            val insertAt        = if (targetIndex < 0) 0 else targetIndex
                            viewModel.moveStep(draggedId, targetContainerId, insertAt)
                            success = true
                        }
                    }
                    null -> {}
                }
            }
            e.isDropCompleted = success
            cell.graphic?.styleClass?.remove("step-cell-drag-over")
            e.consume()
        }

        cell.setOnDragDone { e ->
            cell.graphic?.styleClass?.remove("step-cell-drag-over")
            e.consume()
        }
    }

    private fun resolveParentContainerId(
        parentItem: javafx.scene.control.TreeItem<TreeNode>?
    ): ContainerId? {
        var item = parentItem
        while (item != null) {
            when (val v = item.value) {
                is TreeNode.ContainerNode -> return v.container.id
                is TreeNode.StepNode -> {
                    val containers = v.step.containers()
                    if (containers.size == 1) return containers.first().id
                    return null
                }
                null -> return null
            }
        }
        return null
    }

    private fun resolveSiblingList(
        parentItem: javafx.scene.control.TreeItem<TreeNode>?,
        parentContainerId: ContainerId?
    ): List<Step> {
        // If the immediate parent is a ContainerNode, use that container's steps
        val immediateParent = parentItem?.value
        if (immediateParent is TreeNode.ContainerNode) {
            return immediateParent.container.steps
        }
        if (parentContainerId == null) return viewModel.steps.toList()
        return viewModel.findContainer(parentContainerId)?.steps ?: emptyList()
    }
}
