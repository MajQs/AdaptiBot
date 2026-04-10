package com.adaptibot.ui.view

import com.adaptibot.script.step.BlockStep
import com.adaptibot.script.step.ElseBlock
import com.adaptibot.script.step.IfBlock
import com.adaptibot.script.step.ObserverStep
import com.adaptibot.script.step.Step
import com.adaptibot.script.step.StepId
import com.adaptibot.ui.util.StepDragData
import com.adaptibot.ui.viewmodel.ScriptViewModel
import javafx.scene.control.TreeCell
import javafx.scene.input.*

/**
 * Handles drag-and-drop reordering for a single [TreeCell<TreeNode>].
 *
 * - Drag source : sets step id in clipboard (only [TreeNode.StepNode] cells)
 * - Drag target : accepts a step id and moves it to the target position.
 */
class StepCellDragDropHandler(
    private val cell: TreeCell<TreeNode>,
    private val viewModel: ScriptViewModel
) {

    fun install() {
        cell.setOnDragDetected { e ->
            val node = cell.item as? TreeNode.StepNode ?: return@setOnDragDetected
            // Branch containers (IfBlock/ElseBlock) are integral parts of ConditionalBlock
            // and must not be dragged independently.
            if (node.step is IfBlock || node.step is ElseBlock) return@setOnDragDetected
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

                val targetNode = cell.item as? TreeNode.StepNode
                if (targetNode != null && draggedId != targetNode.step.id) {
                    val targetStep = targetNode.step
                    val parentItem = cell.treeItem?.parent
                    val parentStepId: StepId? = resolveParentStepId(parentItem)
                    val siblings: List<Step> = resolveSiblingList(parentStepId)
                    val targetIndex = siblings.indexOfFirst { it.id == targetStep.id }
                    val insertAt = if (targetIndex < 0) 0 else targetIndex
                    viewModel.moveStep(draggedId, parentStepId, insertAt)
                    success = true
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

    private fun resolveParentStepId(parentItem: javafx.scene.control.TreeItem<TreeNode>?): StepId? {
        var item = parentItem
        while (item != null) {
            val v = item.value
            if (v is TreeNode.StepNode) return v.step.id
            if (v == null) return null  // invisible root
            item = item.parent
        }
        return null
    }

    private fun resolveSiblingList(parentStepId: StepId?): List<Step> {
        if (parentStepId == null) return viewModel.steps.toList()
        return when (val p = viewModel.findStep(parentStepId)) {
            is BlockStep    -> p.steps
            is ObserverStep -> p.steps
            else            -> emptyList()
        }
    }
}
