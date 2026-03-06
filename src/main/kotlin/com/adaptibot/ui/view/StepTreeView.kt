package com.adaptibot.ui.view

import com.adaptibot.model.*
import com.adaptibot.ui.util.StepDragData
import com.adaptibot.ui.viewmodel.ScriptViewModel
import javafx.collections.ListChangeListener
import javafx.scene.control.*
import javafx.scene.input.*

class StepTreeView(private val viewModel: ScriptViewModel) : TreeView<Step>() {

    private var onEditStep: ((Step) -> Unit)? = null
    private var onAddStep: ((parentId: StepId?) -> Unit)? = null

    init {
        styleClass.add("step-tree-view")
        isShowRoot = false
        selectionModel.selectionMode = SelectionMode.SINGLE

        val rootItem = TreeItem<Step>()
        this.root = rootItem

        rebuildTree()

        viewModel.steps.addListener(ListChangeListener { rebuildTree() })

        viewModel.activeStepIdProperty.addListener { _, _, newId ->
            refresh()
        }

        setCellFactory { StepTreeCell(viewModel, { onEditStep?.invoke(it) }, { onAddStep?.invoke(it) }) }
    }

    fun setOnEditStep(handler: (Step) -> Unit) { onEditStep = handler }
    fun setOnAddStep(handler: (parentId: StepId?) -> Unit) { onAddStep = handler }

    private fun rebuildTree() {
        root.children.setAll(viewModel.steps.map { buildItem(it) })
    }

    private fun buildItem(step: Step): TreeItem<Step> {
        val item = TreeItem(step)
        item.isExpanded = true
        when (step) {
            is GroupBlock -> step.steps.forEach { item.children.add(buildItem(it)) }
            is ConditionalBlock -> {
                step.steps.forEach { item.children.add(buildItem(it)) }
                step.elseSteps.forEach { item.children.add(buildItem(it)) }
            }
            is ObserverStep -> step.steps.forEach { item.children.add(buildItem(it)) }
            else -> {}
        }
        return item
    }
}

private class StepTreeCell(
    private val viewModel: ScriptViewModel,
    private val onEdit: (Step) -> Unit,
    private val onAddStep: (parentId: StepId?) -> Unit
) : TreeCell<Step>() {

    private var dragDropHandler = StepCellDragDropHandler(this, viewModel)

    init {
        dragDropHandler.install()
        setOnMouseClicked { e ->
            if (e.clickCount == 2 && item != null) onEdit(item)
        }
    }

    override fun updateItem(step: Step?, empty: Boolean) {
        super.updateItem(step, empty)
        if (empty || step == null) {
            graphic = null
            text = null
            contextMenu = null
            styleClass.removeAll("step-cell-active")
        } else {
            val activeId = viewModel.activeStepIdProperty.get()
            val isActive = activeId != null && step.id == activeId

            graphic = StepCellGraphic.build(step, isActive)
            text = null
            contextMenu = buildContextMenu(step)
        }
    }

    private fun buildContextMenu(step: Step): ContextMenu {
        val menu = ContextMenu()

        val editItem = MenuItem("✏  Edit")
        editItem.setOnAction { onEdit(step) }

        val deleteItem = MenuItem("🗑  Delete")
        deleteItem.setOnAction { viewModel.removeStep(step.id) }

        menu.items.addAll(editItem, SeparatorMenuItem(), deleteItem)

        // "Add step" submenu only for block-like steps
        if (step is BlockStep || step is ObserverStep) {
            val addItem = MenuItem("＋  Add step inside")
            addItem.setOnAction { onAddStep(step.id) }
            menu.items.add(0, addItem)
            menu.items.add(1, SeparatorMenuItem())
        }

        val addAfterItem = MenuItem("＋  Add step after")
        addAfterItem.setOnAction { onAddStep(parentStepId()) }
        menu.items.add(menu.items.size - 1, addAfterItem)
        menu.items.add(menu.items.size - 1, SeparatorMenuItem())

        return menu
    }

    /** Returns parent block id or null (top-level). */
    private fun parentStepId(): StepId? = treeItem?.parent?.value?.id
}

