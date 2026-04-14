package com.adaptibot.ui.dialog

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.stage.Window

/**
 * Dialog for selecting which type of step to add.
 * Returns a [StepType] on OK, null on cancel.
 */
class AddStepDialog(owner: Window?) : Dialog<StepType>() {

    init {
        title = "Add Step"
        owner?.let { initOwner(it) }
        dialogPane.stylesheets.add(
            javaClass.getResource("/css/adaptibot.css")?.toExternalForm() ?: ""
        )
        dialogPane.style = "-fx-background-color: #1e1e2e;"

        val content = VBox(12.0).apply { padding = Insets(16.0); prefWidth = 340.0 }

        val groups = mapOf(
            "🖱  Mouse" to listOf(StepType.MOUSE_CLICK, StepType.MOUSE_DRAG, StepType.MOUSE_MOVE, StepType.MOUSE_SCROLL),
            "⌨  Keyboard" to listOf(StepType.KEYBOARD_TYPE, StepType.KEYBOARD_KEYS),
            "⏱  System" to listOf(StepType.WAIT),
            "📦  Blocks" to listOf(StepType.GROUP, StepType.CONDITIONAL, StepType.OBSERVER, StepType.WHILE, StepType.FOR)
        )

        val toggleGroup = ToggleGroup()
        var selectedType: StepType? = null

        for ((groupName, types) in groups) {
            val groupLabel = Label(groupName).apply { styleClass.add("form-section-title") }
            val typeBox = VBox(4.0)
            for (type in types) {
                val radio = RadioButton(type.label).apply {
                    this.toggleGroup = toggleGroup
                    userData = type
                    styleClass.add("check-box")
                    setOnAction { selectedType = type }
                }
                typeBox.children.add(radio)
            }
            content.children.addAll(groupLabel, typeBox, Separator())
        }

        dialogPane.content = ScrollPane(content).apply {
            isFitToWidth = true; styleClass.add("scroll-pane"); prefHeight = 420.0
        }
        dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

        setResultConverter { bt ->
            if (bt == ButtonType.OK) selectedType else null
        }
    }
}

