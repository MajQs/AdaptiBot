package com.adaptibot.ui.view

import com.adaptibot.model.*
import com.adaptibot.ui.dialog.StepType
import com.adaptibot.ui.viewmodel.ScriptViewModel
import javafx.collections.ListChangeListener
import javafx.scene.control.*

class StepTreeView(private val viewModel: ScriptViewModel) : TreeView<Step>() {

    private var onEditStep: ((Step) -> Unit)? = null
    /**
     * Called when user picks a type from the inline picker.
     * [parentId] – id of the container to add into (null = root level).
     * [afterStepId] – id of the step after which to insert (null = append to end).
     */
    private var onAddStep: ((parentId: StepId?, afterStepId: StepId?, type: StepType) -> Unit)? = null

    init {
        styleClass.add("step-tree-view")
        isShowRoot = false
        selectionModel.selectionMode = SelectionMode.SINGLE

        val rootItem = TreeItem<Step>()
        this.root = rootItem

        rebuildTree()

        viewModel.steps.addListener(ListChangeListener { rebuildTree() })

        viewModel.activeStepIdProperty.addListener { _, _, _ -> refresh() }

        setCellFactory {
            StepTreeCell(viewModel, { onEditStep?.invoke(it) }) { parentId, afterStepId, type ->
                onAddStep?.invoke(parentId, afterStepId, type)
            }
        }
    }

    fun setOnEditStep(handler: (Step) -> Unit) { onEditStep = handler }

    /**
     * Legacy convenience – wraps old (parentId) → showAddStepFlow style.
     * Kept so ScriptPanel can wire simply; the type is forwarded.
     */
    fun setOnAddStep(handler: (parentId: StepId?, afterStepId: StepId?, type: StepType) -> Unit) {
        onAddStep = handler
    }

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
    private val onAddStep: (parentId: StepId?, afterStepId: StepId?, type: StepType) -> Unit
) : TreeCell<Step>() {

    private var dragDropHandler = StepCellDragDropHandler(this, viewModel)

    /** Reused popup – lazily created once per cell instance. */
    private val picker by lazy {
        StepTypePickerPopup { type ->
            val step = item ?: return@StepTypePickerPopup
            // "add after this step" → parent is step's parent, insert after this step
            onAddStep(parentStepId(), step.id, type)
        }
    }

    /** Popup for "add inside" (container steps). */
    private val pickerInside by lazy {
        StepTypePickerPopup { type ->
            val step = item ?: return@StepTypePickerPopup
            // "add inside this container" → parent is this step, append at end
            onAddStep(step.id, null, type)
        }
    }

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

            graphic = StepCellGraphic.build(
                step        = step,
                isActive    = isActive,
                onAddAfter  = { anchorX, anchorY ->
                    picker.show(scene.window, anchorX, anchorY)
                },
                onAddInside = { anchorX, anchorY ->
                    pickerInside.show(scene.window, anchorX, anchorY)
                }
            )
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

        // "Add inside" only for containers
        if (step is BlockStep || step is ObserverStep) {
            val addInsideItem = MenuItem("＋  Add step inside")
            addInsideItem.setOnAction {
                val bounds = graphic?.localToScreen(graphic!!.boundsInLocal)
                val x = bounds?.minX ?: 0.0
                val y = bounds?.maxY?.plus(4) ?: 0.0
                pickerInside.show(scene.window, x, y)
            }
            menu.items.addAll(addInsideItem, SeparatorMenuItem())
        }

        val addAfterItem = MenuItem("＋  Add step after")
        addAfterItem.setOnAction {
            val bounds = graphic?.localToScreen(graphic!!.boundsInLocal)
            val x = bounds?.minX ?: 0.0
            val y = bounds?.maxY?.plus(4) ?: 0.0
            picker.show(scene.window, x, y)
        }

        menu.items.addAll(addAfterItem, SeparatorMenuItem(), editItem, SeparatorMenuItem(), deleteItem)
        return menu
    }

    /** Returns parent block id or null (top-level). */
    private fun parentStepId(): StepId? = treeItem?.parent?.value?.id
}

