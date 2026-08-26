package com.adaptibot.ui.dialog

import com.adaptibot.script.value.Condition
import com.adaptibot.script.value.ImagePattern
import com.adaptibot.script.value.Matcher
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*

/**
 * Recursive visual editor for [Condition] tree.
 * Displays AND / OR / NOT / ElementExists nodes, each with add/remove controls.
 */
class ConditionEditor(
    initial: Condition? = null
) : ScrollPane() {

    private val rootNodeWrapper = VBox()

    private var rootNode: ConditionNode = initial?.let { ConditionNode(it) }
        ?: ConditionNode(Condition.ElementExists(Matcher.ImagePresent(ImagePattern("", 0.7))))

    init {
        isFitToWidth = true
        styleClass.add("scroll-pane")
        content = rootNodeWrapper
        refreshRoot()
    }

    fun getCondition(): Condition = rootNode.getCondition()

    private fun refreshRoot() {
        rootNodeWrapper.children.setAll(rootNode)
    }
}

/**
 * A single node in the condition tree. Renders a compound node or leaf,
 * and supports adding / removing children.
 */
private class ConditionNode(
    initial: Condition
) : VBox(4.0) {

    private val typeCombo = ComboBox<String>().apply {
        styleClass.add("form-combo")
        items.setAll("ElementExists", "AND", "OR", "NOT")
        prefWidth = 130.0
    }

    private val childrenBox = VBox(4.0)
    private val matcherEditor: MatcherEditor

    private var currentType: String
    private var condChildren: MutableList<ConditionNode> = mutableListOf()

    init {
        padding = Insets(4.0, 0.0, 4.0, 12.0)
        styleClass.add("condition-node")

        matcherEditor = MatcherEditor(
            initial = if (initial is Condition.ElementExists) initial.matcher else null
        )

        currentType = when (initial) {
            is Condition.ElementExists -> "ElementExists"
            is Condition.And           -> "AND"
            is Condition.Or            -> "OR"
            is Condition.Not           -> "NOT"
        }
        typeCombo.value = currentType

        when (initial) {
            is Condition.And -> initial.conditions.forEach { condChildren.add(ConditionNode(it)) }
            is Condition.Or  -> initial.conditions.forEach { condChildren.add(ConditionNode(it)) }
            is Condition.Not -> condChildren.add(ConditionNode(initial.condition))
            else -> {}
        }

        typeCombo.setOnAction { onTypeChanged() }
        rebuild()
    }

    private fun defaultCondition() =
        Condition.ElementExists(Matcher.ImagePresent(ImagePattern("", 0.7)))

    fun getCondition(): Condition = when (typeCombo.value) {
        "AND" -> Condition.And(condChildren.map { it.getCondition() })
        "OR"  -> Condition.Or(condChildren.map { it.getCondition() })
        "NOT" -> Condition.Not(condChildren.firstOrNull()?.getCondition() ?: defaultCondition())
        else  -> Condition.ElementExists(
            matcherEditor.getMatcher() ?: Matcher.ImagePresent(ImagePattern("", 0.7))
        )
    }

    private fun onTypeChanged() {
        currentType = typeCombo.value ?: "ElementExists"
        rebuild()
    }

    private fun rebuild() {
        children.clear()
        styleClass.removeAll("condition-node-and", "condition-node-or", "condition-node-not", "condition-node-leaf")

        val typeLabel = Label(currentType).apply { styleClass.add("condition-op-label") }
        val header    = HBox(8.0, typeLabel, typeCombo).apply { alignment = Pos.CENTER_LEFT }

        when (currentType) {
            "ElementExists" -> {
                styleClass.add("condition-node-leaf")
                children.addAll(header, matcherEditor)
            }
            "AND", "OR" -> {
                styleClass.add(if (currentType == "AND") "condition-node-and" else "condition-node-or")
                val addBtn = Button("＋ Add condition").apply {
                    styleClass.add("toolbar-btn")
                    style = "-fx-font-size: 10px; -fx-padding: 2 8 2 8;"
                    setOnAction {
                        condChildren.add(ConditionNode(defaultCondition()))
                        rebuildChildren()
                    }
                }
                rebuildChildren()
                children.addAll(header, childrenBox, addBtn)
            }
            "NOT" -> {
                styleClass.add("condition-node-not")
                if (condChildren.isEmpty()) condChildren.add(ConditionNode(defaultCondition()))
                rebuildChildren()
                children.addAll(header, childrenBox)
            }
        }
    }

    private fun rebuildChildren() {
        childrenBox.children.setAll(condChildren.map { buildChildRow(it) })
    }

    private fun buildChildRow(child: ConditionNode): HBox {
        val removeBtn = Button("✕").apply {
            styleClass.add("toolbar-btn")
            style = "-fx-padding: 2 6 2 6; -fx-font-size: 10px;"
            setOnAction { condChildren.remove(child); rebuildChildren() }
        }
        return HBox(6.0, child, removeBtn).apply {
            alignment = Pos.TOP_RIGHT
            HBox.setHgrow(child, Priority.ALWAYS)
        }
    }
}
