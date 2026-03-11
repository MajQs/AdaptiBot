package com.adaptibot.ui.view

import com.adaptibot.model.*
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox

object StepCellGraphic {

    /**
     * @param onAddAfter called when the inline [+] button is clicked;
     *                   receives the screen X/Y of the button for popup anchoring.
     */
    fun build(step: Step, isActive: Boolean, onAddAfter: ((anchorX: Double, anchorY: Double) -> Unit)? = null): HBox {
        val badge = badge(step)
        val labelText = Label(step.label ?: defaultLabel(step)).apply {
            styleClass.add("step-label-text")
        }
        val detail = Label(detail(step)).apply {
            styleClass.add("step-detail-text")
        }
        val textBox = VBox(2.0, labelText, detail).apply {
            alignment = Pos.CENTER_LEFT
        }

        val dragHandle = Label("⠿").apply {
            styleClass.addAll("step-detail-text")
            style = "-fx-padding: 0 6 0 0; -fx-cursor: open-hand;"
        }

        val spacer = Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }

        val addBtn = Button("＋").apply {
            styleClass.add("inline-add-btn")
            isVisible  = false   // shown only on row hover
            isFocusTraversable = false
            setOnAction { e ->
                val bounds = localToScreen(boundsInLocal)
                onAddAfter?.invoke(bounds.minX, bounds.maxY + 4)
                e.consume()
            }
        }

        val box = HBox(4.0, dragHandle, badge, textBox, spacer, addBtn).apply {
            alignment = Pos.CENTER_LEFT
            styleClass.add("step-cell-box")
            if (isActive) styleClass.add("step-cell-active")

            // show/hide the [+] button on hover
            setOnMouseEntered { addBtn.isVisible = true }
            setOnMouseExited  { addBtn.isVisible = false }
        }
        return box
    }

    private fun badge(step: Step): Label = when (step) {
        is ActionStep -> Label(actionBadgeText(step.action)).apply {
            styleClass.addAll("step-badge", "step-badge-action")
        }
        is GroupBlock -> Label("GROUP").apply {
            styleClass.addAll("step-badge", "step-badge-group")
        }
        is ConditionalBlock -> Label("IF").apply {
            styleClass.addAll("step-badge", "step-badge-cond")
        }
        is ObserverStep -> Label("OBS").apply {
            styleClass.addAll("step-badge", "step-badge-observer")
        }
    }

    private fun actionBadgeText(action: Action): String = when (action) {
        is Action.Mouse.Click     -> "CLICK"
        is Action.Mouse.Drag      -> "DRAG"
        is Action.Mouse.MoveTo    -> "MOVE"
        is Action.Mouse.Scroll    -> "SCROLL"
        is Action.Keyboard.TypeText  -> "TYPE"
        is Action.Keyboard.PressKeys -> "KEYS"
        is Action.System.Wait        -> "WAIT"
        is Action.System.LaunchApplication -> "LAUNCH"
        is Action.System.CloseApplication  -> "CLOSE"
    }

    private fun defaultLabel(step: Step): String = when (step) {
        is ActionStep -> actionBadgeText(step.action).lowercase().replaceFirstChar { it.uppercase() } + " step"
        is GroupBlock -> "Group"
        is ConditionalBlock -> "Conditional"
        is ObserverStep -> "Observer"
    }

    private fun detail(step: Step): String = when (step) {
        is ActionStep -> actionDetail(step.action)
        is GroupBlock -> "${step.steps.size} step(s)"
        is ConditionalBlock -> "${step.steps.size} step(s)${if (step.elseSteps.isNotEmpty()) " / ${step.elseSteps.size} else" else ""}"
        is ObserverStep -> "${step.steps.size} step(s) on trigger"
    }

    private fun actionDetail(action: Action): String = when (action) {
        is Action.Mouse.Click -> buildString {
            append(action.button.name.lowercase())
            append(" ${action.type.name.lowercase()}")
            if (action.target != null) append(" @ ${identifierShort(action.target)}")
        }
        is Action.Mouse.Drag -> "from ${identifierShort(action.from)} → ${identifierShort(action.to)}"
        is Action.Mouse.MoveTo -> "→ ${identifierShort(action.target)}"
        is Action.Mouse.Scroll -> "${action.direction.name.lowercase()} ×${action.amount}"
        is Action.Keyboard.TypeText -> "\"${action.text.take(30)}${if (action.text.length > 30) "…" else ""}\""
        is Action.Keyboard.PressKeys -> action.keys.joinToString("+") { it.name }
        is Action.System.Wait -> "${action.milliseconds} ms"
        is Action.System.LaunchApplication -> action.path
        is Action.System.CloseApplication -> action.processName
    }

    private fun identifierShort(id: ElementIdentifier?): String = when (id) {
        is ElementIdentifier.ByCoordinate -> "(${id.coordinate.x}, ${id.coordinate.y})"
        is ElementIdentifier.ByImage -> "[image]"
        null -> "?"
    }
}

