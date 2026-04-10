package com.adaptibot.ui.view

import com.adaptibot.script.step.*
import com.adaptibot.ui.dialog.StepType
import com.adaptibot.ui.viewmodel.ScriptViewModel
import javafx.collections.ListChangeListener
import javafx.scene.control.*

class StepTreeView(private val viewModel: ScriptViewModel) : TreeView<TreeNode>() {

    private var onEditStep: ((Step) -> Unit)? = null
    private var onAddStep: ((parentId: StepId?, afterStepId: StepId?, type: StepType) -> Unit)? = null

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
            ScriptTreeCell(viewModel, { onEditStep?.invoke(it) }) { parentId, afterStepId, type ->
                onAddStep?.invoke(parentId, afterStepId, type)
            }
        }
    }

    fun setOnEditStep(handler: (Step) -> Unit) { onEditStep = handler }

    fun setOnAddStep(handler: (parentId: StepId?, afterStepId: StepId?, type: StepType) -> Unit) {
        onAddStep = handler
    }

    private fun rebuildTree() {
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
                val n = item.value
                if (n is TreeNode.StepNode) out.add(n.step.id.value)
            }
            collectExpandedKeys(item.children, out)
        }
    }

    private fun buildItem(step: Step, expandedKeys: Set<String> = emptySet()): TreeItem<TreeNode> {
        val isNew = expandedKeys.isEmpty()
        val item = TreeItem<TreeNode>(TreeNode.StepNode(step))
        val stepKey = step.id.value
        item.isExpanded = when {
            isNew                        -> true
            step is BlockStep            -> stepKey in expandedKeys
            step is ObserverStep         -> stepKey in expandedKeys
            step is ConditionalStep     -> stepKey in expandedKeys
            else                         -> false
        }

        when (step) {
            is ConditionalStep -> {
                // IfBlock and ElseBlock are first-class BlockStep nodes
                item.children.add(buildItem(step.ifBlock, expandedKeys))
                item.children.add(buildItem(step.elseBlock, expandedKeys))
            }
            is BlockStep    -> step.steps.forEach { item.children.add(buildItem(it, expandedKeys)) }
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
    private val onAddStep: (parentId: StepId?, afterStepId: StepId?, type: StepType) -> Unit
) : TreeCell<TreeNode>() {

    private val dragDropHandler = StepCellDragDropHandler(this, viewModel)

    /** Popup for "add after this step". */
    private val picker by lazy {
        StepTypePickerPopup { type ->
            val node = item as? TreeNode.StepNode ?: return@StepTypePickerPopup
            // Branch containers must not have siblings inserted after them
            if (node.step is IfBlock || node.step is ElseBlock) return@StepTypePickerPopup
            onAddStep(parentStepId(), node.step.id, type)
        }
    }

    /** Popup for "add inside" (BlockStep / ObserverStep / ConditionalBlock header). */
    private val pickerInside by lazy {
        StepTypePickerPopup { type ->
            val node = item as? TreeNode.StepNode ?: return@StepTypePickerPopup
            onAddStep(node.step.id, null, type)
        }
    }

    init {
        dragDropHandler.install()
        setOnMouseClicked { e ->
            val node = item
            if (e.clickCount == 2 && node is TreeNode.StepNode
                && node.step !is IfBlock && node.step !is ElseBlock) {
                onEdit(node.step)
            }
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

    private fun buildStepContextMenu(step: Step): ContextMenu {
        val menu = ContextMenu()

        // Branch containers: only "add inside" is allowed
        if (step is IfBlock || step is ElseBlock) {
            menu.items += MenuItem("＋  Add step inside").also { mi ->
                mi.setOnAction {
                    val b = graphic?.localToScreen(graphic!!.boundsInLocal)
                    pickerInside.show(scene.window, b?.minX ?: 0.0, b?.maxY?.plus(4) ?: 0.0)
                }
            }
            return menu
        }

        val editItem   = MenuItem("✏  Edit").also { it.setOnAction { onEdit(step) } }
        val deleteItem = MenuItem("🗑  Delete").also { it.setOnAction { viewModel.removeStep(step.id) } }

        // Any container step (BlockStep, ObserverStep, ConditionalBlock) gets an "add inside" entry
        val isContainer = step is BlockStep || step is ObserverStep || step is ConditionalStep
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

    /** Returns the [StepId] of the parent step (for "add after"). */
    private fun parentStepId(): StepId? {
        var parent = treeItem?.parent
        while (parent != null) {
            val v = parent.value
            if (v is TreeNode.StepNode) return v.step.id
            if (v == null) return null  // reached invisible root
            parent = parent.parent
        }
        return null
    }
}
