package com.adaptibot.ui.view

import com.adaptibot.model.BlockStep
import com.adaptibot.model.ObserverStep
import com.adaptibot.model.Step
import com.adaptibot.model.StepId
import com.adaptibot.ui.util.StepDragData
import com.adaptibot.ui.viewmodel.ScriptViewModel
import javafx.scene.control.TreeCell
import javafx.scene.input.*

/**
 * Handles drag-and-drop reordering for a single [TreeCell<Step>].
 * - Drag source: sets step id in clipboard
 * - Drag target: accepts any step id, moves it to the target position
 */
class StepCellDragDropHandler(
    private val cell: TreeCell<Step>,
    private val viewModel: ScriptViewModel
) {

    fun install() {
        cell.setOnDragDetected { e ->
            val step = cell.item ?: return@setOnDragDetected
            val db = cell.startDragAndDrop(TransferMode.MOVE)
            val content = ClipboardContent()
            content[StepDragData.DATA_FORMAT] = step.id.value
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
                val targetItem = cell.treeItem
                val targetStep = cell.item

                if (targetStep != null && draggedId != targetStep.id) {
                    // Determine parent and index of target
                    val parentItem = targetItem?.parent
                    val parentStepId: StepId? = parentItem?.value?.id

                    val siblings: List<Step> = if (parentStepId == null) {
                        viewModel.steps
                    } else {
                        when (val p = parentItem.value) {
                            is BlockStep -> p.steps
                            is ObserverStep -> p.steps
                            else -> emptyList()
                        }
                    }

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
}

