package com.adaptibot.ui.dialog

import com.adaptibot.model.Condition
import com.adaptibot.model.ElementIdentifier
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.stage.Window

/**
 * Recursive visual editor for [Condition] tree.
 * Displays AND / OR / NOT / ElementExists nodes, each with add/remove controls.
 */
class ConditionEditor(
    initial: Condition? = null,
    private val ownerWindow: Window? = null
) : ScrollPane() {

    private val rootNodeWrapper = VBox()

    private var rootNode: ConditionNode = initial?.let { ConditionNode(it, ownerWindow) }
        ?: ConditionNode(Condition.ElementExists(ElementIdentifier.ByCoordinate(
            com.adaptibot.model.Coordinate(0, 0))), ownerWindow)

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
    initial: Condition,
    private val ownerWindow: Window?
) : VBox(4.0) {

    private val typeCombo = ComboBox<String>().apply {
        styleClass.add("form-combo")
        items.setAll("ElementExists", "AND", "OR", "NOT")
        prefWidth = 130.0
    }

    private val childrenBox = VBox(4.0)
    private val identifierEditor: ElementIdentifierEditor

    private var currentType: String
    private var condChildren: MutableList<ConditionNode> = mutableListOf()

    init {
        padding = Insets(4.0, 0.0, 4.0, 12.0)
        styleClass.add("condition-node")

        identifierEditor = ElementIdentifierEditor(
            initial = if (initial is Condition.ElementExists) initial.identifier else null,
            ownerWindow = ownerWindow
        )

        currentType = when (initial) {
            is Condition.ElementExists -> "ElementExists"
            is Condition.And           -> "AND"
            is Condition.Or            -> "OR"
            is Condition.Not           -> "NOT"
        }
        typeCombo.value = currentType

        when (initial) {
            is Condition.And -> initial.conditions.forEach { condChildren.add(ConditionNode(it, ownerWindow)) }
            is Condition.Or  -> initial.conditions.forEach { condChildren.add(ConditionNode(it, ownerWindow)) }
            is Condition.Not -> condChildren.add(ConditionNode(initial.condition, ownerWindow))
            else -> {}
        }

        typeCombo.setOnAction { onTypeChanged() }
        rebuild()
    }

    fun getCondition(): Condition = when (typeCombo.value) {
        "AND" -> Condition.And(condChildren.map { it.getCondition() })
        "OR"  -> Condition.Or(condChildren.map { it.getCondition() })
        "NOT" -> Condition.Not(condChildren.firstOrNull()?.getCondition()
            ?: Condition.ElementExists(ElementIdentifier.ByCoordinate(com.adaptibot.model.Coordinate(0, 0))))
        else  -> Condition.ElementExists(identifierEditor.getIdentifier()
            ?: ElementIdentifier.ByCoordinate(com.adaptibot.model.Coordinate(0, 0)))
    }

    private fun onTypeChanged() {
        currentType = typeCombo.value ?: "ElementExists"
        rebuild()
    }

    private fun rebuild() {
        children.clear()
        styleClass.removeAll("condition-node-and", "condition-node-or", "condition-node-not", "condition-node-leaf")

        val typeLabel = Label(currentType).apply { styleClass.add("condition-op-label") }
        val header = HBox(8.0, typeLabel, typeCombo).apply { alignment = Pos.CENTER_LEFT }

        when (currentType) {
            "ElementExists" -> {
                styleClass.add("condition-node-leaf")
                children.addAll(header, identifierEditor)
            }
            "AND", "OR" -> {
                styleClass.add(if (currentType == "AND") "condition-node-and" else "condition-node-or")
                val addBtn = Button("＋ Add condition").apply {
                    styleClass.add("toolbar-btn")
                    style = "-fx-font-size: 10px; -fx-padding: 2 8 2 8;"
                    setOnAction {
                        condChildren.add(ConditionNode(
                            Condition.ElementExists(ElementIdentifier.ByCoordinate(com.adaptibot.model.Coordinate(0, 0))),
                            ownerWindow
                        ))
                        rebuildChildren()
                    }
                }
                rebuildChildren()
                children.addAll(header, childrenBox, addBtn)
            }
            "NOT" -> {
                styleClass.add("condition-node-not")
                if (condChildren.isEmpty()) {
                    condChildren.add(ConditionNode(
                        Condition.ElementExists(ElementIdentifier.ByCoordinate(com.adaptibot.model.Coordinate(0, 0))),
                        ownerWindow
                    ))
                }
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
            setOnAction {
                condChildren.remove(child)
                rebuildChildren()
            }
        }
        return HBox(6.0, child, removeBtn).apply {
            alignment = Pos.TOP_RIGHT
            HBox.setHgrow(child, Priority.ALWAYS)
        }
    }
}

