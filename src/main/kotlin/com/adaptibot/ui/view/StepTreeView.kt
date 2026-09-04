package com.adaptibot.ui.view

import com.adaptibot.script.step.*
import com.adaptibot.ui.dialog.StepType
import com.adaptibot.ui.viewmodel.ScriptViewModel
import javafx.collections.ListChangeListener
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent

class StepTreeView(private val viewModel: ScriptViewModel) : TreeView<TreeNode>() {

    private var onEditStep: ((Step) -> Unit)? = null
    private var onAddStep: ((containerId: ContainerId, afterStepId: StepId?, type: StepType) -> Unit)? = null

    init {
        styleClass.add("step-tree-view")
        isShowRoot = false
        selectionModel.selectionMode = SelectionMode.SINGLE

        val rootItem = TreeItem<TreeNode>()
        this.root = rootItem

        rebuildTree()

        viewModel.steps.addListener(ListChangeListener { rebuildTree() })
        viewModel.activeStepIdProperty.addListener { _, _, _ -> refresh() }
        viewModel.armedObserverIdsProperty.addListener { _, _, _ -> refresh() }
        viewModel.handlingObserverIdProperty.addListener { _, _, _ -> refresh() }

        setCellFactory {
            ScriptTreeCell(viewModel, { onEditStep?.invoke(it) }) { containerId, afterStepId, type ->
                onAddStep?.invoke(containerId, afterStepId, type)
            }
        }
    }

    fun setOnEditStep(handler: (Step) -> Unit) { onEditStep = handler }

    fun setOnAddStep(handler: (containerId: ContainerId, afterStepId: StepId?, type: StepType) -> Unit) {
        onAddStep = handler
    }

    private fun rebuildTree() {
        val expandedKeys = mutableSetOf<String>()
        collectExpandedKeys(root.children, expandedKeys)
        root.children.setAll(viewModel.steps.map { buildItem(it, expandedKeys) })
    }

    private fun collectExpandedKeys(items: Iterable<TreeItem<TreeNode>>, out: MutableSet<String>) {
        for (item in items) {
            if (item.isExpanded) {
                when (val n = item.value) {
                    is TreeNode.StepNode      -> out.add(n.step.id.value)
                    is TreeNode.ContainerNode -> out.add(n.container.id.value)
                    null -> {}
                }
            }
            collectExpandedKeys(item.children, out)
        }
    }

    private fun buildItem(step: Step, expandedKeys: Set<String> = emptySet(), parentDisabled: Boolean = false): TreeItem<TreeNode> {
        val isNew = expandedKeys.isEmpty()
        val item = TreeItem<TreeNode>(TreeNode.StepNode(step, parentDisabled))
        val stepKey = step.id.value
        item.isExpanded = when {
            isNew -> true
            step is ConditionalStep || step is GroupStep || step is ObserverStep
                || step is WhileStep || step is ForStep -> stepKey in expandedKeys
            else -> false
        }

        val childDisabled = parentDisabled || !step.enabled
        when (step) {
            is ConditionalStep -> {
                item.children.add(buildContainerItem(step.trueContainer, step.id, "IF TRUE", expandedKeys, childDisabled))
                item.children.add(buildContainerItem(step.elseContainer, step.id, "IF ELSE", expandedKeys, childDisabled))
            }
            is GroupStep    -> step.container.steps.forEach { item.children.add(buildItem(it, expandedKeys, childDisabled)) }
            is ObserverStep -> step.container.steps.forEach { item.children.add(buildItem(it, expandedKeys, childDisabled)) }
            is WhileStep    -> step.container.steps.forEach { item.children.add(buildItem(it, expandedKeys, childDisabled)) }
            is ForStep      -> step.container.steps.forEach { item.children.add(buildItem(it, expandedKeys, childDisabled)) }
            is ActionStep   -> {}
        }
        return item
    }

    private fun buildContainerItem(
        container: StepContainer,
        parentStepId: StepId,
        label: String,
        expandedKeys: Set<String>,
        parentDisabled: Boolean = false
    ): TreeItem<TreeNode> {
        val isNew = expandedKeys.isEmpty()
        val item = TreeItem<TreeNode>(TreeNode.ContainerNode(container, parentStepId, label, parentDisabled))
        item.isExpanded = if (isNew) true else container.id.value in expandedKeys
        container.steps.forEach { item.children.add(buildItem(it, expandedKeys, parentDisabled)) }
        return item
    }
}

// ── Cell ──────────────────────────────────────────────────────────────────────

private class ScriptTreeCell(
    private val viewModel: ScriptViewModel,
    private val onEdit: (Step) -> Unit,
    private val onAddStep: (containerId: ContainerId, afterStepId: StepId?, type: StepType) -> Unit
) : TreeCell<TreeNode>() {

    private val dragDropHandler = StepCellDragDropHandler(this, viewModel)

    /** Add a step after the current step (not valid for ContainerNode). */
    private val picker by lazy {
        StepTypePickerPopup { type ->
            val node = item as? TreeNode.StepNode ?: return@StepTypePickerPopup
            onAddStep(parentContainerId(), node.step.id, type)
        }
    }

    /** Add a step inside a single-container step (GroupStep, ObserverStep, loop steps). */
    private val pickerInside by lazy {
        StepTypePickerPopup { type ->
            val node = item as? TreeNode.StepNode ?: return@StepTypePickerPopup
            val containerId = node.step.containers().firstOrNull()?.id ?: return@StepTypePickerPopup
            onAddStep(containerId, null, type)
        }
    }

    /** Add a step inside a ContainerNode (e.g. IF TRUE / IF ELSE). */
    private val pickerInsideContainer by lazy {
        StepTypePickerPopup { type ->
            val node = item as? TreeNode.ContainerNode ?: return@StepTypePickerPopup
            onAddStep(node.container.id, null, type)
        }
    }

    init {
        dragDropHandler.install()
        setOnMouseClicked { e ->
            val node = item
            if (e.clickCount == 2 && node is TreeNode.StepNode) onEdit(node.step)
        }
        setOnContextMenuRequested { e ->
            contextMenu?.show(this, e.screenX, e.screenY)
            e.consume()
        }
    }

    override fun updateItem(node: TreeNode?, empty: Boolean) {
        super.updateItem(node, empty)
        if (empty || node == null) {
            graphic = null; text = null; contextMenu = null
            styleClass.removeAll("step-cell-active")
            return
        }
        when (node) {
            is TreeNode.ContainerNode -> {
                graphic = StepCellGraphic.buildContainerHeader(
                    container      = node.container,
                    label          = node.label,
                    parentDisabled = node.parentDisabled,
                    onAddInside    = { ax, ay -> pickerInsideContainer.show(scene.window, ax, ay) }
                )
                text = null
                contextMenu = buildContainerContextMenu(node)
            }
            is TreeNode.StepNode -> {
                val activeId = viewModel.activeStepIdProperty.get()
                val isActive = activeId != null && node.step.id == activeId
                val isSingleContainer = node.step.containers().size == 1
                val isObserver = node.step is ObserverStep
                val stepKey = node.step.id.value
                graphic = StepCellGraphic.build(
                    step           = node.step,
                    isActive       = isActive,
                    isEnabled      = node.step.enabled,
                    parentDisabled = node.parentDisabled,
                    isWatching     = isObserver && stepKey in viewModel.armedObserverIdsProperty.get(),
                    isHandling     = isObserver && stepKey == viewModel.handlingObserverIdProperty.get(),
                    onAddAfter     = { ax, ay -> picker.show(scene.window, ax, ay) },
                    onAddInside    = if (isSingleContainer) ({ ax, ay -> pickerInside.show(scene.window, ax, ay) }) else null
                )
                text = null
                contextMenu = buildStepContextMenu(node.step)
            }
        }
    }

    // ── Context menus ─────────────────────────────────────────────────────────

    private fun buildContainerContextMenu(node: TreeNode.ContainerNode): ContextMenu {
        val menu = ContextMenu()
        menu.items += MenuItem("＋  Add step to ${node.label}").also { mi ->
            mi.setOnAction {
                val b = graphic?.localToScreen(graphic!!.boundsInLocal)
                pickerInsideContainer.show(scene.window, b?.minX ?: 0.0, b?.maxY?.plus(4) ?: 0.0)
            }
        }
        return menu
    }

    private fun buildStepContextMenu(step: Step): ContextMenu {
        val menu = ContextMenu()
        val editItem   = MenuItem("✏  Edit").also { it.setOnAction { onEdit(step) } }
        val duplicateItem = MenuItem("⧉  Duplicate").also { it.setOnAction { viewModel.duplicateStep(step.id) } }
        val deleteItem = MenuItem("🗑  Delete").also { it.setOnAction { viewModel.removeStep(step.id) } }

        val isSingleContainer = step.containers().size == 1
        if (isSingleContainer) {
            menu.items += MenuItem("＋  Add step inside").also { mi ->
                mi.setOnAction {
                    val b = graphic?.localToScreen(graphic!!.boundsInLocal)
                    pickerInside.show(scene.window, b?.minX ?: 0.0, b?.maxY?.plus(4) ?: 0.0)
                }
            }
            menu.items += SeparatorMenuItem()
        }

        val addAfterItem = MenuItem("＋  Add step after").also { mi ->
            mi.setOnAction {
                val b = graphic?.localToScreen(graphic!!.boundsInLocal)
                picker.show(scene.window, b?.minX ?: 0.0, b?.maxY?.plus(4) ?: 0.0)
            }
        }
        val toggleItem = if (step.enabled) {
            MenuItem("⏸  Disable step").also { it.setOnAction { viewModel.toggleStep(step.id) } }
        } else {
            MenuItem("▶  Enable step").also { it.setOnAction { viewModel.toggleStep(step.id) } }
        }
        menu.items.addAll(addAfterItem, SeparatorMenuItem(), editItem, duplicateItem, SeparatorMenuItem(), toggleItem, SeparatorMenuItem(), deleteItem)
        return menu
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Walks up the tree to find the [ContainerId] of the nearest ancestor container.
     * Falls back to [rootContainerId] if the step is at root level.
     */
    private fun parentContainerId(): ContainerId {
        var parent = treeItem?.parent
        while (parent != null) {
            when (val v = parent.value) {
                is TreeNode.ContainerNode -> return v.container.id
                is TreeNode.StepNode -> {
                    val containers = v.step.containers()
                    if (containers.size == 1) return containers.first().id
                    return viewModel.rootContainerId
                }
                null -> return viewModel.rootContainerId
            }
        }
        return viewModel.rootContainerId
    }
}
