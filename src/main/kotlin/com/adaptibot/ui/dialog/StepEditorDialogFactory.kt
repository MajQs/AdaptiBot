package com.adaptibot.ui.dialog

import com.adaptibot.script.step.*
import com.adaptibot.script.value.*
import com.adaptibot.script.value.Target as ScriptTarget
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.stage.Window

/**
 * Creates and shows an edit dialog for any [Step] subtype.
 * Returns a modified (or new) step on OK, null on cancel.
 */
object StepEditorDialogFactory {

    fun show(step: Step, owner: Window?): Step? = when (step) {
        is ActionStep      -> ActionStepDialog(step, owner).showAndWait().orElse(null)
        is GroupStep       -> GroupStepDialog(step, owner).showAndWait().orElse(null)
        is ConditionalStep -> ConditionalStepDialog(step, owner).showAndWait().orElse(null)
        is ObserverStep    -> ObserverStepDialog(step, owner).showAndWait().orElse(null)
        is WhileStep       -> WhileStepDialog(step, owner).showAndWait().orElse(null)
        is ForStep         -> ForStepDialog(step, owner).showAndWait().orElse(null)
    }

    fun showNew(actionType: StepType, owner: Window?, defaultDelayBefore: Long = 0): Step? {
        return when (actionType) {
            StepType.MOUSE_CLICK   -> ActionStepDialog(ActionStep(action = Action.Mouse.Click(delayBefore = defaultDelayBefore)), owner).showAndWait().orElse(null)
            StepType.MOUSE_DRAG    -> ActionStepDialog(ActionStep(action = Action.Mouse.Drag(delayBefore = defaultDelayBefore, to = ScriptTarget.AtCoordinate(Coordinate(0, 0)))), owner).showAndWait().orElse(null)
            StepType.MOUSE_MOVE    -> ActionStepDialog(ActionStep(action = Action.Mouse.MoveTo(ScriptTarget.AtCoordinate(Coordinate(0, 0)), delayBefore = defaultDelayBefore)), owner).showAndWait().orElse(null)
            StepType.MOUSE_SCROLL  -> ActionStepDialog(ActionStep(action = Action.Mouse.Scroll(MouseScrollDirection.DOWN, 3, delayBefore = defaultDelayBefore)), owner).showAndWait().orElse(null)
            StepType.KEYBOARD_TYPE -> ActionStepDialog(ActionStep(action = Action.Keyboard.TypeText("", delayBefore = defaultDelayBefore)), owner).showAndWait().orElse(null)
            StepType.KEYBOARD_KEYS -> ActionStepDialog(ActionStep(action = Action.Keyboard.PressKeys(emptyList(), delayBefore = defaultDelayBefore)), owner).showAndWait().orElse(null)
            StepType.WAIT          -> ActionStepDialog(ActionStep(action = Action.System.Wait(500, delayBefore = defaultDelayBefore)), owner).showAndWait().orElse(null)
            StepType.GROUP         -> GroupStepDialog(GroupStep(), owner).showAndWait().orElse(null)
            StepType.CONDITIONAL   -> ConditionalStepDialog(ConditionalStep(condition = Condition.ElementExists(VisualMatcher.ImagePresent(ImagePattern("", 0.7)))), owner).showAndWait().orElse(null)
            StepType.OBSERVER      -> ObserverStepDialog(ObserverStep(condition = Condition.ElementExists(VisualMatcher.ImagePresent(ImagePattern("", 0.7)))), owner).showAndWait().orElse(null)
            StepType.WHILE         -> WhileStepDialog(WhileStep(condition = Condition.ElementExists(VisualMatcher.ImagePresent(ImagePattern("", 0.7)))), owner).showAndWait().orElse(null)
            StepType.FOR           -> ForStepDialog(ForStep(), owner).showAndWait().orElse(null)
        }
    }
}

enum class StepType(val label: String) {
    MOUSE_CLICK("Mouse Click"),
    MOUSE_DRAG("Mouse Drag"),
    MOUSE_MOVE("Mouse Move"),
    MOUSE_SCROLL("Mouse Scroll"),
    KEYBOARD_TYPE("Type Text"),
    KEYBOARD_KEYS("Press Keys"),
    WAIT("Wait"),
    GROUP("Group Block"),
    CONDITIONAL("Conditional Block"),
    OBSERVER("Observer Step"),
    WHILE("While Loop"),
    FOR("For Loop");
}

// ── Base helper ──────────────────────────────────────────────────────────────

private fun styledDialog(title: String, owner: Window?): Dialog<Unit> {
    return Dialog<Unit>().apply {
        this.title = title
        owner?.let { initOwner(it) }
        dialogPane.stylesheets.add(
            StepEditorDialogFactory::class.java.getResource("/css/adaptibot.css")?.toExternalForm() ?: ""
        )
        dialogPane.style = "-fx-background-color: #1e1e2e;"
        dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)
        (dialogPane.lookupButton(ButtonType.OK) as Button).defaultButtonProperty().set(true)
    }
}

private fun formGrid(): GridPane = GridPane().apply {
    hgap = 10.0; vgap = 10.0
    padding = Insets(16.0)
    prefWidth = 460.0
    columnConstraints.addAll(
        ColumnConstraints(120.0),
        ColumnConstraints().apply { hgrow = Priority.ALWAYS; isFillWidth = true }
    )
}

private fun labelField(): TextField = TextField().apply { styleClass.add("form-field") }
private fun longField(): TextField = TextField("0").apply { styleClass.add("form-field"); prefWidth = 100.0 }
private fun formLabel(text: String) = Label(text).apply { styleClass.add("form-label") }
private fun sectionTitle(text: String) = Label(text).apply { styleClass.add("form-section-title") }

// ── ActionStep Dialog ─────────────────────────────────────────────────────────

private class ActionStepDialog(
    private val original: ActionStep,
    owner: Window?
) : Dialog<ActionStep>() {

    init {
        title = "Edit Action Step"
        owner?.let { initOwner(it) }
        dialogPane.stylesheets.add(
            javaClass.getResource("/css/adaptibot.css")?.toExternalForm() ?: ""
        )
        dialogPane.style = "-fx-background-color: #1e1e2e;"

        val grid = formGrid()
        var row = 0

        // Label
        val labelField = labelField().apply { text = original.label ?: "" }
        grid.add(formLabel("Label *"), 0, row); grid.add(labelField, 1, row++);

        // Action-specific section
        grid.add(sectionTitle("ACTION"), 0, row, 2, 1); row++

        val actionContent = buildActionEditor(original.action, row, grid, owner)

        dialogPane.content = ScrollPane(grid).apply {
            isFitToWidth = true
            styleClass.add("scroll-pane")
            prefHeight = 500.0
        }

        dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)
        (dialogPane.lookupButton(ButtonType.OK) as Button).defaultButtonProperty().set(true)

        setResultConverter { bt ->
            if (bt == ButtonType.OK) {
                original.copy(
                    label = labelField.text.ifBlank { null }?.trim(),
                    action = actionContent.getAction()
                )
            } else null
        }
    }

    private fun buildActionEditor(action: Action, startRow: Int, grid: GridPane, owner: Window?): ActionEditor {
        return when (action) {
            is Action.Mouse.Click    -> MouseClickEditor(action, startRow, grid)
            is Action.Mouse.Drag     -> MouseDragEditor(action, startRow, grid)
            is Action.Mouse.MoveTo   -> MouseMoveEditor(action, startRow, grid)
            is Action.Mouse.Scroll   -> MouseScrollEditor(action, startRow, grid)
            is Action.Keyboard.TypeText  -> KeyboardTypeEditor(action, startRow, grid)
            is Action.Keyboard.PressKeys -> KeyboardKeysEditor(action, startRow, grid)
            is Action.System.Wait        -> WaitEditor(action, startRow, grid)
            is Action.System.LaunchApplication -> StaticActionEditor(action)
            is Action.System.CloseApplication  -> StaticActionEditor(action)
        }
    }
}

private interface ActionEditor { fun getAction(): Action }

private class StaticActionEditor(private val action: Action) : ActionEditor {
    override fun getAction() = action
}

private class MouseClickEditor(
    private val orig: Action.Mouse.Click, startRow: Int, grid: GridPane
) : ActionEditor {
    private val targetEditor = MouseTargetEditor(orig.target)
    private val buttonCombo = ComboBox<MouseButton>().apply {
        styleClass.add("form-combo"); items.setAll(MouseButton.entries); value = orig.button
    }
    private val typeCombo = ComboBox<MouseClickType>().apply {
        styleClass.add("form-combo"); items.setAll(MouseClickType.entries); value = orig.type
    }
    private val holdField = TextField(orig.holdDuration.toString()).apply { styleClass.add("form-field"); prefWidth = 100.0 }
    private val delayField = longField().apply { text = orig.delayBefore.toString() }

    init {
        var r = startRow
        grid.add(formLabel("Delay before (ms)"), 0, r); grid.add(delayField, 1, r++);
        grid.add(formLabel("Target"), 0, r); grid.add(targetEditor, 1, r++);
        grid.add(formLabel("Button"), 0, r); grid.add(buttonCombo, 1, r++);
        grid.add(formLabel("Click type"), 0, r); grid.add(typeCombo, 1, r++);
        grid.add(formLabel("Hold duration (ms)"), 0, r); grid.add(holdField, 1, r);
    }

    override fun getAction() = orig.copy(
        delayBefore = delayField.text.toLongOrNull() ?: 0L,
        target = targetEditor.getTarget(),
        button = buttonCombo.value ?: MouseButton.LEFT,
        type = typeCombo.value ?: MouseClickType.SINGLE,
        holdDuration = holdField.text.toLongOrNull() ?: 0L
    )
}

private class MouseDragEditor(
    private val orig: Action.Mouse.Drag, startRow: Int, grid: GridPane
) : ActionEditor {
    private val fromEditor = MouseTargetEditor(orig.from)
    private val toEditor   = MouseTargetEditor(orig.to)
    private val delayField = longField().apply { text = orig.delayBefore.toString() }

    init {
        var r = startRow
        grid.add(formLabel("Delay before (ms)"), 0, r); grid.add(delayField, 1, r++);
        grid.add(formLabel("From"), 0, r); grid.add(fromEditor, 1, r++);
        grid.add(formLabel("To"), 0, r); grid.add(toEditor, 1, r);
    }

    override fun getAction() = orig.copy(
        delayBefore = delayField.text.toLongOrNull() ?: 0L,
        from = fromEditor.getTarget(),
        to   = toEditor.getTarget() ?: ScriptTarget.AtCoordinate(Coordinate(0, 0))
    )
}

private class MouseMoveEditor(
    private val orig: Action.Mouse.MoveTo, startRow: Int, grid: GridPane
) : ActionEditor {
    private val editor = MouseTargetEditor(orig.target)
    private val delayField = longField().apply { text = orig.delayBefore.toString() }
    init {
        grid.add(formLabel("Delay before (ms)"), 0, startRow); grid.add(delayField, 1, startRow)
        grid.add(formLabel("Target"), 0, startRow + 1); grid.add(editor, 1, startRow + 1)
    }
    override fun getAction() = orig.copy(
        delayBefore = delayField.text.toLongOrNull() ?: 0L,
        target = editor.getTarget() ?: ScriptTarget.AtCoordinate(Coordinate(0, 0))
    )
}

private class MouseScrollEditor(
    private val orig: Action.Mouse.Scroll, startRow: Int, grid: GridPane
) : ActionEditor {
    private val dirCombo = ComboBox<MouseScrollDirection>().apply {
        styleClass.add("form-combo"); items.setAll(MouseScrollDirection.entries); value = orig.direction
    }
    private val amountField = TextField(orig.amount.toString()).apply { styleClass.add("form-field"); prefWidth = 100.0 }
    private val delayField = longField().apply { text = orig.delayBefore.toString() }
    init {
        grid.add(formLabel("Delay before (ms)"), 0, startRow); grid.add(delayField, 1, startRow)
        grid.add(formLabel("Direction"), 0, startRow + 1); grid.add(dirCombo, 1, startRow + 1)
        grid.add(formLabel("Amount"), 0, startRow + 2); grid.add(amountField, 1, startRow + 2)
    }
    override fun getAction() = orig.copy(
        delayBefore = delayField.text.toLongOrNull() ?: 0L,
        direction = dirCombo.value ?: MouseScrollDirection.DOWN,
        amount = amountField.text.toIntOrNull() ?: 1
    )
}

private class KeyboardTypeEditor(
    private val orig: Action.Keyboard.TypeText, startRow: Int, grid: GridPane
) : ActionEditor {
    private val textArea = TextArea(orig.text).apply {
        styleClass.add("form-field"); prefRowCount = 4
    }
    private val delayField = longField().apply { text = orig.delayBefore.toString() }
    init {
        grid.add(formLabel("Delay before (ms)"), 0, startRow); grid.add(delayField, 1, startRow)
        grid.add(formLabel("Text"), 0, startRow + 1); grid.add(textArea, 1, startRow + 1)
    }
    override fun getAction() = orig.copy(
        delayBefore = delayField.text.toLongOrNull() ?: 0L,
        text = textArea.text
    )
}

private class KeyboardKeysEditor(
    private val orig: Action.Keyboard.PressKeys, startRow: Int, grid: GridPane
) : ActionEditor {
    private val checkBoxes: List<CheckBox> = KeyboardKey.entries.map { key ->
        CheckBox(key.name).apply {
            isSelected = key in orig.keys
            styleClass.add("check-box")
        }
    }
    private val delayField = longField().apply { text = orig.delayBefore.toString() }
    init {
        val wrapPane = FlowPane(4.0, 4.0).apply {
            prefWrapLength = 320.0
            children.addAll(checkBoxes)
        }
        grid.add(formLabel("Delay before (ms)"), 0, startRow); grid.add(delayField, 1, startRow)
        grid.add(formLabel("Keys"), 0, startRow + 1)
        grid.add(wrapPane, 1, startRow + 1)
    }
    override fun getAction() = orig.copy(
        delayBefore = delayField.text.toLongOrNull() ?: 0L,
        keys = checkBoxes.filter { it.isSelected }.map { KeyboardKey.valueOf(it.text) }
    )
}

private class WaitEditor(
    private val orig: Action.System.Wait, startRow: Int, grid: GridPane
) : ActionEditor {
    private val msField = TextField(orig.milliseconds.toString()).apply { styleClass.add("form-field"); prefWidth = 130.0 }
    private val delayField = longField().apply { text = orig.delayBefore.toString() }
    init {
        grid.add(formLabel("Delay before (ms)"), 0, startRow); grid.add(delayField, 1, startRow)
        grid.add(formLabel("Wait (ms)"), 0, startRow + 1); grid.add(msField, 1, startRow + 1)
    }
    override fun getAction() = orig.copy(
        delayBefore = delayField.text.toLongOrNull() ?: 0L,
        milliseconds = msField.text.toLongOrNull() ?: 500L
    )
}

// ── GroupStep Dialog ──────────────────────────────────────────────────────────

private class GroupStepDialog(private val original: GroupStep, owner: Window?) : Dialog<GroupStep>() {
    init {
        title = "Edit Group Step"
        owner?.let { initOwner(it) }
        dialogPane.stylesheets.add(javaClass.getResource("/css/adaptibot.css")?.toExternalForm() ?: "")
        dialogPane.style = "-fx-background-color: #1e1e2e;"

        val grid = formGrid()
        val labelField = labelField().apply { text = original.label ?: "" }
        grid.add(formLabel("Label *"), 0, 0); grid.add(labelField, 1, 0)
        dialogPane.content = grid
        dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)
        setResultConverter { bt ->
            if (bt == ButtonType.OK) original.copy(
                label = labelField.text.ifBlank { null }?.trim()
            ) else null
        }
    }
}

// ── ConditionalStep Dialog ────────────────────────────────────────────────────

private class ConditionalStepDialog(private val original: ConditionalStep, owner: Window?) : Dialog<ConditionalStep>() {
    init {
        title = "Edit Conditional Block"
        owner?.let { initOwner(it) }
        dialogPane.stylesheets.add(javaClass.getResource("/css/adaptibot.css")?.toExternalForm() ?: "")
        dialogPane.style = "-fx-background-color: #1e1e2e;"

        val grid = formGrid()
        val labelField = labelField().apply { text = original.label ?: "" }
        grid.add(formLabel("Label *"), 0, 0); grid.add(labelField, 1, 0)
        grid.add(sectionTitle("CONDITION"), 0, 1, 2, 1)

        val condEditor = ConditionEditor(original.condition)
        grid.add(condEditor, 0, 2, 2, 1)

        val scrollPane = ScrollPane(grid).apply {
            isFitToWidth = true
            styleClass.add("scroll-pane")
            prefHeight = 520.0
        }
        dialogPane.content = scrollPane
        dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)
        setResultConverter { bt ->
            if (bt == ButtonType.OK) original.copy(
                label = labelField.text.ifBlank { null }?.trim(),
                condition = condEditor.getCondition()
            ) else null
        }
    }
}

// ── ObserverStep Dialog ───────────────────────────────────────────────────────

private class ObserverStepDialog(private val original: ObserverStep, owner: Window?) : Dialog<ObserverStep>() {
    init {
        title = "Edit Observer Step"
        owner?.let { initOwner(it) }
        dialogPane.stylesheets.add(javaClass.getResource("/css/adaptibot.css")?.toExternalForm() ?: "")
        dialogPane.style = "-fx-background-color: #1e1e2e;"

        val grid = formGrid()
        val labelField = labelField().apply { text = original.label ?: "" }
        grid.add(formLabel("Label *"), 0, 0); grid.add(labelField, 1, 0)
        grid.add(sectionTitle("TRIGGER CONDITION"), 0, 1, 2, 1)

        val condEditor = ConditionEditor(original.condition)
        grid.add(condEditor, 0, 2, 2, 1)

        val scrollPane = ScrollPane(grid).apply {
            isFitToWidth = true; styleClass.add("scroll-pane"); prefHeight = 520.0
        }
        dialogPane.content = scrollPane
        dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)
        setResultConverter { bt ->
            if (bt == ButtonType.OK) original.copy(
                label = labelField.text.ifBlank { null }?.trim(),
                condition = condEditor.getCondition()
            ) else null
        }
    }
}

// ── WhileStep Dialog ──────────────────────────────────────────────────────────

private class WhileStepDialog(private val original: WhileStep, owner: Window?) : Dialog<WhileStep>() {
    init {
        title = "Edit While Loop"
        owner?.let { initOwner(it) }
        dialogPane.stylesheets.add(javaClass.getResource("/css/adaptibot.css")?.toExternalForm() ?: "")
        dialogPane.style = "-fx-background-color: #1e1e2e;"

        val grid = formGrid()
        val labelField = labelField().apply { text = original.label ?: "" }
        grid.add(formLabel("Label *"), 0, 0); grid.add(labelField, 1, 0)
        grid.add(sectionTitle("LOOP CONDITION"), 0, 1, 2, 1)

        val condEditor = ConditionEditor(original.condition)
        grid.add(condEditor, 0, 2, 2, 1)

        val scrollPane = ScrollPane(grid).apply {
            isFitToWidth = true
            styleClass.add("scroll-pane")
            prefHeight = 520.0
        }
        dialogPane.content = scrollPane
        dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)
        setResultConverter { bt ->
            if (bt == ButtonType.OK) original.copy(
                label = labelField.text.ifBlank { null }?.trim(),
                condition = condEditor.getCondition()
            ) else null
        }
    }
}

// ── ForStep Dialog ────────────────────────────────────────────────────────────

private class ForStepDialog(private val original: ForStep, owner: Window?) : Dialog<ForStep>() {
    init {
        title = "Edit For Loop"
        owner?.let { initOwner(it) }
        dialogPane.stylesheets.add(javaClass.getResource("/css/adaptibot.css")?.toExternalForm() ?: "")
        dialogPane.style = "-fx-background-color: #1e1e2e;"

        val grid = formGrid()
        val labelField = labelField().apply { text = original.label ?: "" }
        val iterationsField = TextField(original.iterations.toString()).apply {
            styleClass.add("form-field"); prefWidth = 100.0
        }

        grid.add(formLabel("Label *"), 0, 0); grid.add(labelField, 1, 0)
        grid.add(sectionTitle("LOOP SETTINGS"), 0, 1, 2, 1)
        grid.add(formLabel("Iterations"), 0, 2); grid.add(iterationsField, 1, 2)

        dialogPane.content = grid
        dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)
        setResultConverter { bt ->
            if (bt == ButtonType.OK) original.copy(
                label = labelField.text.ifBlank { null }?.trim(),
                iterations = iterationsField.text.toIntOrNull()?.coerceAtLeast(1) ?: 1
            ) else null
        }
    }
}

