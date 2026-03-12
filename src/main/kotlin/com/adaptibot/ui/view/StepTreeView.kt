package com.adaptibot.ui.view

import com.adaptibot.model.*
import com.adaptibot.ui.dialog.StepType
import com.adaptibot.ui.viewmodel.ScriptViewModel
import javafx.collections.ListChangeListener
import javafx.scene.control.*

/** Which branch of a [ConditionalBlock] a new step should be added to. */
enum class ConditionalBranch { TRUE, ELSE, DEFAULT }

class StepTreeView(private val viewModel: ScriptViewModel) : TreeView<TreeNode>() {

    private var onEditStep: ((Step) -> Unit)? = null
    private var onAddStep: ((parentId: StepId?, afterStepId: StepId?, type: StepType, branch: ConditionalBranch) -> Unit)? = null

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
            ScriptTreeCell(viewModel, { onEditStep?.invoke(it) }) { parentId, afterStepId, type, branch ->
                onAddStep?.invoke(parentId, afterStepId, type, branch)
            }
        }
    }

    fun setOnEditStep(handler: (Step) -> Unit) { onEditStep = handler }

    fun setOnAddStep(handler: (parentId: StepId?, afterStepId: StepId?, type: StepType, branch: ConditionalBranch) -> Unit) {
        onAddStep = handler
    }

    private fun rebuildTree() {
        // Snapshot expanded state before destroying the old items.
        // Key for StepNode  → step id string  (e.g. "step_1")
        // Key for BranchNode → "<parentId>:<TRUE|ELSE>"  (e.g. "cond_1:TRUE")
        val expandedKeys = mutableSetOf<String>()
        collectExpandedKeys(root.children, expandedKeys)

        root.children.setAll(viewModel.steps.map { buildItem(it, expandedKeys) })
    }

    private fun collectExpandedKeys(
        items: Iterable<TreeItem<TreeNode>>,
        out: MutableSet<String>
    ) {
        for (item in items) {
            if (item.isExpanded) {
                when (val n = item.value) {
                    is TreeNode.StepNode   -> out.add(n.step.id.value)
                    is TreeNode.BranchNode -> out.add("${n.parentId.value}:${n.branch.name}")
                    null -> {}
                }
            }
            collectExpandedKeys(item.children, out)
        }
    }

    private fun buildItem(step: Step, expandedKeys: Set<String> = emptySet()): TreeItem<TreeNode> {
        // A node is expanded when:
        //  • it is brand-new (not in expandedKeys at all) → default true for blocks
        //  • it was previously expanded (key present in expandedKeys)
        // A node is collapsed when its key was previously collapsed (absent from expandedKeys
        // but the snapshot is non-empty, meaning it existed before and the user collapsed it).
        val isNew = expandedKeys.isEmpty()  // first build – expand everything

        val item = TreeItem<TreeNode>(TreeNode.StepNode(step))
        val stepKey = step.id.value
        item.isExpanded = when {
            isNew                         -> true
            step is BlockStep             -> stepKey in expandedKeys
            step is ObserverStep          -> stepKey in expandedKeys
            else                          -> false   // leaf – doesn't matter
        }

        when (step) {
            is ConditionalBlock -> {
                val trueKey  = "${step.id.value}:${ConditionalBranch.TRUE.name}"
                val elseKey  = "${step.id.value}:${ConditionalBranch.ELSE.name}"

                val trueHeader = TreeItem<TreeNode>(
                    TreeNode.BranchNode(step.id, ConditionalBranch.TRUE, step.steps.size)
                ).also { it.isExpanded = if (isNew) true else trueKey in expandedKeys }
                step.steps.forEach { trueHeader.children.add(buildItem(it, expandedKeys)) }

                val elseHeader = TreeItem<TreeNode>(
                    TreeNode.BranchNode(step.id, ConditionalBranch.ELSE, step.elseSteps.size)
                ).also { it.isExpanded = if (isNew) true else elseKey in expandedKeys }
                step.elseSteps.forEach { elseHeader.children.add(buildItem(it, expandedKeys)) }

                item.children.addAll(trueHeader, elseHeader)
            }
            is GroupBlock   -> step.steps.forEach { item.children.add(buildItem(it, expandedKeys)) }
            is ObserverStep -> step.steps.forEach { item.children.add(buildItem(it, expandedKeys)) }
            else -> {}
        }
        return item
    }
}

// ── Cell ──────────────────────────────────────────────────────────────────────

private class ScriptTreeCell(
    private val viewModel: ScriptViewModel,
    private val onEdit: (Step) -> Unit,
    private val onAddStep: (parentId: StepId?, afterStepId: StepId?, type: StepType, branch: ConditionalBranch) -> Unit
) : TreeCell<TreeNode>() {

    private val dragDropHandler = StepCellDragDropHandler(this, viewModel)

    /** Popup for "add after this step". */
    private val picker by lazy {
        StepTypePickerPopup { type ->
            val node = item as? TreeNode.StepNode ?: return@StepTypePickerPopup
            onAddStep(parentStepId(), node.step.id, type, ConditionalBranch.DEFAULT)
        }
    }

    /** Popup for "add inside" (GroupBlock / ObserverStep). */
    private val pickerInside by lazy {
        StepTypePickerPopup { type ->
            val node = item as? TreeNode.StepNode ?: return@StepTypePickerPopup
            onAddStep(node.step.id, null, type, ConditionalBranch.DEFAULT)
        }
    }

    /** Popup for "add to TRUE branch" – triggered from BranchNode or ConditionalBlock cell. */
    private val pickerInsideTrue by lazy {
        StepTypePickerPopup { type ->
            val parentId = resolveConditionalParentId() ?: return@StepTypePickerPopup
            onAddStep(parentId, null, type, ConditionalBranch.TRUE)
        }
    }

    /** Popup for "add to ELSE branch" – triggered from BranchNode or ConditionalBlock cell. */
    private val pickerInsideElse by lazy {
        StepTypePickerPopup { type ->
            val parentId = resolveConditionalParentId() ?: return@StepTypePickerPopup
            onAddStep(parentId, null, type, ConditionalBranch.ELSE)
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
            styleClass.removeAll("step-cell-active", "branch-node-cell")
            return
        }
        when (node) {
            is TreeNode.BranchNode -> {
                styleClass.add("branch-node-cell")
                styleClass.remove("step-cell-active")
                val popup = if (node.branch == ConditionalBranch.TRUE) pickerInsideTrue else pickerInsideElse
                graphic = BranchNodeGraphic.build(node) { anchorX, anchorY ->
                    popup.show(scene.window, anchorX, anchorY)
                }
                text = null
                contextMenu = buildBranchContextMenu(node)
            }
            is TreeNode.StepNode -> {
                styleClass.remove("branch-node-cell")
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
        val label = if (node.branch == ConditionalBranch.TRUE) "TRUE" else "ELSE"
        val popup = if (node.branch == ConditionalBranch.TRUE) pickerInsideTrue else pickerInsideElse

        val addItem = MenuItem("＋  Add step to $label branch")
        addItem.setOnAction {
            val bounds = graphic?.localToScreen(graphic!!.boundsInLocal)
            popup.show(scene.window, bounds?.minX ?: 0.0, bounds?.maxY?.plus(4) ?: 0.0)
        }
        menu.items.add(addItem)
        return menu
    }

    private fun buildStepContextMenu(step: Step): ContextMenu {
        val menu = ContextMenu()

        val editItem   = MenuItem("✏  Edit").also { it.setOnAction { onEdit(step) } }
        val deleteItem = MenuItem("🗑  Delete").also { it.setOnAction { viewModel.removeStep(step.id) } }

        when {
            step is ConditionalBlock -> {
                menu.items += MenuItem("＋  Add step to TRUE branch").also { mi ->
                    mi.setOnAction {
                        val b = graphic?.localToScreen(graphic!!.boundsInLocal)
                        pickerInsideTrue.show(scene.window, b?.minX ?: 0.0, b?.maxY?.plus(4) ?: 0.0)
                    }
                }
                menu.items += MenuItem("＋  Add step to ELSE branch").also { mi ->
                    mi.setOnAction {
                        val b = graphic?.localToScreen(graphic!!.boundsInLocal)
                        pickerInsideElse.show(scene.window, b?.minX ?: 0.0, b?.maxY?.plus(4) ?: 0.0)
                    }
                }
                menu.items += SeparatorMenuItem()
            }
            step is BlockStep || step is ObserverStep -> {
                menu.items += MenuItem("＋  Add step inside").also { mi ->
                    mi.setOnAction {
                        val b = graphic?.localToScreen(graphic!!.boundsInLocal)
                        pickerInside.show(scene.window, b?.minX ?: 0.0, b?.maxY?.plus(4) ?: 0.0)
                    }
                }
                menu.items += SeparatorMenuItem()
            }
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

    /**
     * Returns the [StepId] of the [ConditionalBlock] this cell belongs to.
     * Works both when the cell IS the ConditionalBlock and when it IS a BranchNode child.
     */
    private fun resolveConditionalParentId(): StepId? {
        return when (val n = item) {
            is TreeNode.BranchNode -> n.parentId
            is TreeNode.StepNode   -> (n.step as? ConditionalBlock)?.id
            else                   -> null
        }
    }

    /** Returns the [StepId] of the parent step (for "add after"). */
    private fun parentStepId(): StepId? {
        var parent = treeItem?.parent
        // skip BranchNode headers – go up until we find a StepNode or root
        while (parent != null) {
            val v = parent.value
            if (v is TreeNode.StepNode) return v.step.id
            if (v == null) return null  // reached invisible root
            parent = parent.parent
        }
        return null
    }
}
