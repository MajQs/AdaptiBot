package com.adaptibot.ui.view

import com.adaptibot.model.Step
import com.adaptibot.model.StepId
import com.adaptibot.ui.dialog.StepType
import com.adaptibot.ui.viewmodel.ScriptViewModel
import javafx.collections.ListChangeListener
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*

class ScriptPanel(
    private val viewModel: ScriptViewModel,
    private val onEditStep: (Step) -> Unit,
    /**
     * Called when the user picks a type to add.
     * [parentId]     – container to add into (null = root).
     * [afterStepId]  – insert after this step (null = append at end).
     * [type]         – the chosen [StepType].
     * [branch]       – which branch to add into (relevant for [ConditionalBlock]).
     */
    private val onAddStep: (parentId: StepId?, afterStepId: StepId?, type: StepType, branch: ConditionalBranch) -> Unit
) : BorderPane() {

    val treeView = StepTreeView(viewModel)

    /** Shown when the script is empty instead of the tree. */
    private val emptyState = buildEmptyState()

    init {
        styleClass.add("script-panel")
        top = buildHeader()

        treeView.setOnEditStep(onEditStep)
        treeView.setOnAddStep(onAddStep)

        val scroll = ScrollPane(treeView).apply {
            isFitToWidth = true
            isFitToHeight = true
            styleClass.add("scroll-pane")
        }

        // Switch between empty-state and tree based on step count
        center = if (viewModel.steps.isEmpty()) emptyState else scroll
        viewModel.steps.addListener(ListChangeListener {
            center = if (viewModel.steps.isEmpty()) emptyState else scroll
        })
    }

    private fun buildHeader(): HBox {
        val title = Label("SCRIPT STEPS").apply { styleClass.add("panel-title") }
        val spacer = Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }
        return HBox(8.0, title, spacer).apply {
            styleClass.add("panel-header")
            alignment = Pos.CENTER_LEFT
        }
    }

    private fun buildEmptyState(): VBox {
        val icon   = Label("📜").apply { style = "-fx-font-size: 36px;" }
        val title  = Label("No steps yet").apply { styleClass.add("panel-title") }
        val hint   = Label("Choose a step type below to get started").apply {
            styleClass.add("step-detail-text")
        }

        // Inline picker rendered directly in the empty state
        val picker = StepTypePickerPopup { type -> onAddStep(null, null, type, ConditionalBranch.DEFAULT) }

        val addFirstBtn = Button("＋  Add first step").apply {
            styleClass.add("toolbar-btn-primary")
            setOnAction { e ->
                val bounds = localToScreen(boundsInLocal)
                picker.show(scene.window, bounds.minX, bounds.maxY + 6)
                e.consume()
            }
        }

        return VBox(12.0, icon, title, hint, addFirstBtn).apply {
            styleClass.add("empty-state")
            alignment = Pos.CENTER
        }
    }
}


