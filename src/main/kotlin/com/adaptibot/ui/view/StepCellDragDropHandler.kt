package com.adaptibot.ui.view

import com.adaptibot.script.BlockStep
import com.adaptibot.script.ConditionalBlock
import com.adaptibot.script.ConditionalBranch
import com.adaptibot.script.ObserverStep
import com.adaptibot.script.Step
import com.adaptibot.script.StepId
import com.adaptibot.ui.util.StepDragData
import com.adaptibot.ui.viewmodel.ScriptViewModel
import javafx.scene.control.TreeCell
import javafx.scene.input.*

/**
 * Handles drag-and-drop reordering for a single [TreeCell<TreeNode>].
 *
 * - Drag source : sets step id in clipboard (only [TreeNode.StepNode] cells)
 * - Drag target : accepts a step id and moves it to the target position.
 *   When dropped onto a [TreeNode.BranchNode] the step is appended to that branch.
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
                    is TreeNode.BranchNode -> {
                        // Dropped onto a branch header → append to that branch
                        if (draggedId != targetNode.parentId) {
                            viewModel.moveStepToBranch(
                                stepId   = draggedId,
                                parentId = targetNode.parentId,
                                branch   = targetNode.branch
                            )
                            success = true
                        }
                    }
                    is TreeNode.StepNode -> {
                        val targetStep = targetNode.step
                        if (draggedId != targetStep.id) {
                            val parentItem = cell.treeItem?.parent
                            // Resolve the actual parent step id, skipping BranchNode headers
                            val parentStepId: StepId? = resolveParentStepId(parentItem)

                            val siblings: List<Step> = resolveSiblingList(parentItem, parentStepId)
                            val targetIndex = siblings.indexOfFirst { it.id == targetStep.id }
                            val insertAt = if (targetIndex < 0) 0 else targetIndex

                            viewModel.moveStep(draggedId, parentStepId, insertAt)
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

    /** Walks up the tree skipping [TreeNode.BranchNode] headers to find the real parent step id. */
    private fun resolveParentStepId(parentItem: javafx.scene.control.TreeItem<TreeNode>?): StepId? {
        var item = parentItem
        while (item != null) {
            when (val v = item.value) {
                is TreeNode.StepNode -> return v.step.id
                null -> return null   // invisible root
                else -> item = item.parent  // BranchNode – keep walking up
            }
        }
        return null
    }

    private fun resolveSiblingList(
        parentItem: javafx.scene.control.TreeItem<TreeNode>?,
        parentStepId: StepId?
    ): List<Step> {
        if (parentStepId == null) return viewModel.steps.toList()

        // If the immediate parent is a BranchNode, use the correct branch list
        val immediateParent = parentItem?.value
        if (immediateParent is TreeNode.BranchNode) {
            val conditional = viewModel.findStep(immediateParent.parentId) as? ConditionalBlock
            return if (immediateParent.branch == ConditionalBranch.TRUE)
                conditional?.steps ?: emptyList()
            else
                conditional?.elseSteps ?: emptyList()
        }

        return when (val p = viewModel.findStep(parentStepId)) {
            is BlockStep    -> p.steps
            is ObserverStep -> p.steps
            else            -> emptyList()
        }
    }
}
