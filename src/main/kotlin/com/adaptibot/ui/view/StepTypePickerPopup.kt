package com.adaptibot.ui.view

import com.adaptibot.ui.dialog.StepType
import javafx.geometry.Insets
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Separator
import javafx.scene.layout.FlowPane
import javafx.scene.layout.VBox
import javafx.stage.Popup

/**
 * Lightweight non-modal popup that shows a grid of step-type buttons.
 * Calls [onTypeSelected] with the chosen [StepType] and auto-hides.
 */
class StepTypePickerPopup(
    private val onTypeSelected: (StepType) -> Unit
) : Popup() {

    init {
        isAutoHide = true
        isAutoFix  = true

        val container = VBox(6.0).apply {
            styleClass.add("step-type-picker")
            padding = Insets(10.0)
            prefWidth = 280.0
        }

        container.children.addAll(
            sectionLabel("🖱  Mouse"),
            typeRow(
                typeBtn(StepType.MOUSE_CLICK,  "Click",  "🖱"),
                typeBtn(StepType.MOUSE_DRAG,   "Drag",   "↔"),
                typeBtn(StepType.MOUSE_MOVE,   "Move",   "➡"),
                typeBtn(StepType.MOUSE_SCROLL, "Scroll", "↕")
            ),
            Separator(),
            sectionLabel("⌨  Keyboard"),
            typeRow(
                typeBtn(StepType.KEYBOARD_TYPE, "Type", "⌨"),
                typeBtn(StepType.KEYBOARD_KEYS, "Keys", "🔑")
            ),
            Separator(),
            sectionLabel("⚙  System"),
            typeRow(
                typeBtn(StepType.WAIT, "Wait", "⏱")
            ),
            Separator(),
            sectionLabel("📦  Blocks"),
            typeRow(
                typeBtn(StepType.GROUP,       "Group",  "📦"),
                typeBtn(StepType.CONDITIONAL, "IF/ELSE","❓"),
                typeBtn(StepType.OBSERVER,    "Observer","👁"),
                typeBtn(StepType.WHILE,       "While",  "🔁"),
                typeBtn(StepType.FOR,         "For",    "🔢")
            )
        )

        content.add(container)

        // Apply CSS when the scene is available – resolved via the owner node
        sceneProperty().addListener { _, _, scene ->
            scene?.stylesheets?.add(
                javaClass.getResource("/css/adaptibot.css")?.toExternalForm() ?: ""
            )
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun sectionLabel(text: String) = Label(text).apply {
        styleClass.add("picker-section-label")
    }

    private fun typeRow(vararg buttons: Button): FlowPane = FlowPane(6.0, 6.0).apply {
        children.addAll(*buttons)
    }

    private fun typeBtn(type: StepType, label: String, icon: String): Button =
        Button("$icon  $label").apply {
            styleClass.add("picker-type-btn")
            setOnAction {
                this@StepTypePickerPopup.hide()
                onTypeSelected(type)
            }
        }
}

