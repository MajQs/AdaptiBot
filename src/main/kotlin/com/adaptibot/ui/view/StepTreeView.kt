package com.adaptibot.ui.view

import com.adaptibot.script.step.*
import com.adaptibot.ui.dialog.StepType
import com.adaptibot.ui.viewmodel.ScriptViewModel
import javafx.collections.ListChangeListener
import javafx.scene.control.*

class StepTreeView(private val viewModel: ScriptViewModel) : TreeView<TreeNode>() {

    private var onEditStep: ((Step) -> Unit)? = null
    private var onAddStep: ((parentId: StepId?, branchId: BranchId?, afterStepId: StepId?, type: StepType) -> Unit)? = null

    init {
        styleClass.add("step-tree-view")
        isShowRoot = false
        selectionModel.selectionMode = SelectionMode.SINGLE

        val rootItem = TreeItem<TreeNode>()
        this.root = rootItem

        rebuildTree()

        viewModel.steps.addListener(ListChangeListener { rebuildTree() })
        viewModel.activeStepIdProperty.addListener { _, _, _ -> refresh() }

        setCellFactory {
            ScriptTreeCell(viewModel, { onEditStep?.invoke(it) }) { parentId, branchId, afterStepId, type ->
                onAddStep?.invoke(parentId, branchId, afterStepId, type)
            }
        }
    }

    fun setOnEditStep(handler: (Step) -> Unit) { onEditStep = handler }

    fun setOnAddStep(handler: (parentId: StepId?, branchId: BranchId?, afterStepId: StepId?, type: StepType) -> Unit) {
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
                    is TreeNode.StepNode   -> out.add(n.step.id.value)
                    is TreeNode.BranchNode -> out.add(n.branch.id.value)
                    null -> {}
                }
            }
            collectExpandedKeys(item.children, out)
        }
    }

    private fun buildItem(step: Step, expandedKeys: Set<String> = emptySet()): TreeItem<TreeNode> {
        val isNew = expandedKeys.isEmpty()
        val item = TreeItem<TreeNode>(TreeNode.StepNode(step))
        val stepKey = step.id.value
        item.isExpanded = when {
            isNew -> true
            step is ConditionalStep || step is GroupStep || step is ObserverStep
                || step is WhileStep || step is ForStep -> stepKey in expandedKeys
            else -> false
        }

        when (step) {
            is ConditionalStep -> {
                item.children.add(buildBranchItem(step.trueBranch, step.id, isTrueBranch = true, expandedKeys))
                item.children.add(buildBranchItem(step.elseBranch, step.id, isTrueBranch = false, expandedKeys))
            }
            is GroupStep    -> step.steps.forEach { item.children.add(buildItem(it, expandedKeys)) }
            is ObserverStep -> step.steps.forEach { item.children.add(buildItem(it, expandedKeys)) }
            is WhileStep    -> step.steps.forEach { item.children.add(buildItem(it, expandedKeys)) }
            is ForStep      -> step.steps.forEach { item.children.add(buildItem(it, expandedKeys)) }
            is ActionStep   -> {}
        }
        return item
    }

    private fun buildBranchItem(
        branch: Branch,
        parentStepId: StepId,
        isTrueBranch: Boolean,
        expandedKeys: Set<String>
    ): TreeItem<TreeNode> {
        val isNew = expandedKeys.isEmpty()
        val item = TreeItem<TreeNode>(TreeNode.BranchNode(branch, parentStepId, isTrueBranch))
        item.isExpanded = if (isNew) true else branch.id.value in expandedKeys
        branch.steps.forEach { item.children.add(buildItem(it, expandedKeys)) }
        return item
    }
}

// ── Cell ──────────────────────────────────────────────────────────────────────

private class ScriptTreeCell(
    private val viewModel: ScriptViewModel,
    private val onEdit: (Step) -> Unit,
    private val onAddStep: (parentId: StepId?, branchId: BranchId?, afterStepId: StepId?, type: StepType) -> Unit
) : TreeCell<TreeNode>() {

    private val dragDropHandler = StepCellDragDropHandler(this, viewModel)

    /** Add a step after the current step (not valid for BranchNode). */
    private val picker by lazy {
        StepTypePickerPopup { type ->
            val node = item as? TreeNode.StepNode ?: return@StepTypePickerPopup
            onAddStep(parentStepId(), null, node.step.id, type)
        }
    }

    /** Add a step inside a container step (GroupStep, ObserverStep, loop steps). */
    private val pickerInside by lazy {
        StepTypePickerPopup { type ->
            val node = item as? TreeNode.StepNode ?: return@StepTypePickerPopup
            onAddStep(node.step.id, null, null, type)
        }
    }

    /** Add a step inside a branch (BranchNode). */
    private val pickerInsideBranch by lazy {
        StepTypePickerPopup { type ->
            val node = item as? TreeNode.BranchNode ?: return@StepTypePickerPopup
            onAddStep(null, node.branch.id, null, type)
        }
    }

    init {
        dragDropHandler.install()
        setOnMouseClicked { e ->
            val node = item
            if (e.clickCount == 2 && node is TreeNode.StepNode) onEdit(node.step)
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
            is TreeNode.BranchNode -> {
                graphic = StepCellGraphic.buildBranchHeader(
                    branch      = node.branch,
                    isTrueBranch = node.isTrueBranch,
                    onAddInside  = { ax, ay -> pickerInsideBranch.show(scene.window, ax, ay) }
                )
                text = null
                contextMenu = buildBranchContextMenu(node)
            }
            is TreeNode.StepNode -> {
                val activeId = viewModel.activeStepIdProperty.get()
                val isActive = activeId != null && node.step.id == activeId
                graphic = StepCellGraphic.build(
                    step        = node.step,
                    isActive    = isActive,
                    onAddAfter  = { ax, ay -> picker.show(scene.window, ax, ay) },
                    onAddInside = { ax, ay -> pickerInside.show(scene.window, ax, ay) }
                )
                text = null
                contextMenu = buildStepContextMenu(node.step)
            }
        }
    }

    // ── Context menus ─────────────────────────────────────────────────────────

    private fun buildBranchContextMenu(node: TreeNode.BranchNode): ContextMenu {
        val menu = ContextMenu()
        val label = if (node.isTrueBranch) "IF TRUE" else "IF ELSE"
        menu.items += MenuItem("＋  Add step to $label branch").also { mi ->
            mi.setOnAction {
                val b = graphic?.localToScreen(graphic!!.boundsInLocal)
                pickerInsideBranch.show(scene.window, b?.minX ?: 0.0, b?.maxY?.plus(4) ?: 0.0)
            }
        }
        return menu
    }

    private fun buildStepContextMenu(step: Step): ContextMenu {
        val menu = ContextMenu()
        val editItem   = MenuItem("✏  Edit").also { it.setOnAction { onEdit(step) } }
        val deleteItem = MenuItem("🗑  Delete").also { it.setOnAction { viewModel.removeStep(step.id) } }

        val isContainer = step is GroupStep || step is ObserverStep
                || step is WhileStep || step is ForStep
        if (isContainer) {
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
        menu.items.addAll(addAfterItem, SeparatorMenuItem(), editItem, SeparatorMenuItem(), deleteItem)
        return menu
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parentStepId(): StepId? {
        var parent = treeItem?.parent
        while (parent != null) {
            when (val v = parent.value) {
                is TreeNode.StepNode -> return v.step.id
                null                 -> return null
                else                 -> parent = parent.parent  // BranchNode – walk up
            }
        }
        return null
    }
}
