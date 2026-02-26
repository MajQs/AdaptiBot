package com.adaptibot.ui.dialog

import com.adaptibot.model.Action
import com.adaptibot.model.ActionStep
import com.adaptibot.model.MouseClickType
import com.adaptibot.model.Coordinate
import com.adaptibot.model.ElementIdentifier
import com.adaptibot.model.ImagePattern
import com.adaptibot.model.MouseButton
import com.adaptibot.model.StepId
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.stage.Modality

class StepEditorDialog(private val existingStep: ActionStep? = null) : Dialog<ActionStep>() {

    private val stepIdField = TextField()
    private val labelField = TextField()
    private val actionTypeComboBox = ComboBox<ActionType>()
    private val parametersPane = VBox(10.0)

    // Dynamic fields based on action type
    private var dynamicFields = mutableMapOf<String, Control>()

    fun setInitialActionType(actionType: ActionType) {
        actionTypeComboBox.value = actionType
    }

    init {
        title = if (existingStep == null) "Add New Action Step" else "Edit Action Step"
        headerText = "Configure the action step properties"

        initModality(Modality.APPLICATION_MODAL)
        isResizable = true

        dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

        val grid = GridPane().apply {
            hgap = 10.0
            vgap = 10.0
            padding = Insets(20.0, 150.0, 10.0, 10.0)
        }

        // Basic fields
        grid.add(Label("Step ID:"), 0, 0)
        grid.add(stepIdField, 1, 0)
        stepIdField.promptText = "unique-step-id"

        grid.add(Label("Label:"), 0, 1)
        grid.add(labelField, 1, 1)
        labelField.promptText = "Step description"

        grid.add(Label("Action Type:"), 0, 2)
        grid.add(actionTypeComboBox, 1, 2)

        // Populate action types
        actionTypeComboBox.items.addAll(
            ActionType.MOUSE_MOVE,
            ActionType.MOUSE_LEFT_CLICK,
            ActionType.MOUSE_RIGHT_CLICK,
            ActionType.MOUSE_DOUBLE_CLICK,
            ActionType.KEYBOARD_TYPE,
            ActionType.WAIT
        )

        actionTypeComboBox.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            updateParameterFields(newValue)
        }

        val contentBox = VBox(15.0).apply {
            children.addAll(grid, Separator(), Label("Action Parameters:"), parametersPane)
            padding = Insets(10.0)
        }

        dialogPane.content = contentBox

        // Load existing step data
        existingStep?.let { loadStepData(it) }

        // Result converter
        setResultConverter { buttonType ->
            if (buttonType == ButtonType.OK) {
                buildActionStep()
            } else {
                null
            }
        }

        // Validation
        val okButton = dialogPane.lookupButton(ButtonType.OK) as Button
        okButton.isDisable = true

        stepIdField.textProperty().addListener { _, _, newValue ->
            okButton.isDisable = newValue.isNullOrBlank() || labelField.text.isNullOrBlank() || actionTypeComboBox.value == null
        }
        labelField.textProperty().addListener { _, _, newValue ->
            okButton.isDisable = stepIdField.text.isNullOrBlank() || newValue.isNullOrBlank() || actionTypeComboBox.value == null
        }
        actionTypeComboBox.valueProperty().addListener { _, _, newValue ->
            okButton.isDisable = stepIdField.text.isNullOrBlank() || labelField.text.isNullOrBlank() || newValue == null
        }
    }

    private fun loadStepData(step: ActionStep) {
        stepIdField.text = step.id.value
        stepIdField.isDisable = true // Don't allow changing ID when editing
        labelField.text = step.label ?: ""

        val action = step.action
        val actionType = ActionType.fromAction(action)
        actionTypeComboBox.value = actionType

        updateParameterFields(actionType)

        when (action) {
            is Action.Mouse.MoveTo -> {
                (dynamicFields["target"] as TextField).text = getElementIdentifierString(action.target)
            }
            is Action.Mouse.Click -> {
                action.target?.let {
                    (dynamicFields["target"] as TextField).text = getElementIdentifierString(it)
                }
            }
            is Action.Keyboard.TypeText -> {
                (dynamicFields["text"] as TextField).text = action.text
            }
            is Action.System.Wait -> {
                (dynamicFields["duration"] as TextField).text = action.milliseconds.toString()
            }
            else -> {}
        }
    }

    private fun buildActionStep(): ActionStep {
        val stepId = StepId(stepIdField.text)
        val label = labelField.text
        val actionType = actionTypeComboBox.value

        val action = when (actionType) {
            ActionType.MOUSE_MOVE -> Action.Mouse.MoveTo(parseElementIdentifier((dynamicFields["target"] as TextField).text))
            ActionType.MOUSE_LEFT_CLICK -> Action.Mouse.Click(
                target = parseElementIdentifier((dynamicFields["target"] as TextField).text),
                button = MouseButton.LEFT
            )
            ActionType.MOUSE_RIGHT_CLICK -> Action.Mouse.Click(
                target = parseElementIdentifier((dynamicFields["target"] as TextField).text),
                button = MouseButton.RIGHT
            )
            ActionType.MOUSE_DOUBLE_CLICK -> Action.Mouse.Click(
                target = parseElementIdentifier((dynamicFields["target"] as TextField).text),
                type = MouseClickType.DOUBLE
            )
            ActionType.KEYBOARD_TYPE -> Action.Keyboard.TypeText((dynamicFields["text"] as TextField).text)
            ActionType.WAIT -> Action.System.Wait((dynamicFields["duration"] as TextField).text.toLong())
            else -> throw IllegalStateException("Unsupported action type")
        }

        return ActionStep(
            id = existingStep?.id ?: stepId,
            label = label,
            action = action
        )
    }

    private fun updateParameterFields(actionType: ActionType?) {
        parametersPane.children.clear()
        dynamicFields.clear()

        when (actionType) {
            ActionType.MOUSE_MOVE, ActionType.MOUSE_LEFT_CLICK, ActionType.MOUSE_RIGHT_CLICK, ActionType.MOUSE_DOUBLE_CLICK -> {
                addTextField("target", "Target (e.g., image.png)")
            }
            ActionType.KEYBOARD_TYPE -> {
                addTextField("text", "Text to type")
            }
            ActionType.WAIT -> {
                addTextField("duration", "Duration (ms)")
            }
            else -> {
                // No parameters
            }
        }
    }

    private fun getElementIdentifierString(identifier: ElementIdentifier): String {
        return when (identifier) {
            is ElementIdentifier.ByCoordinate -> "${identifier.coordinate.x},${identifier.coordinate.y}"
            is ElementIdentifier.ByImage -> "[Image Pattern]" // Simplified representation for UI
        }
    }

    private fun parseElementIdentifier(text: String): ElementIdentifier {
        // Try to parse as coordinate (x,y)
        if (text.contains(',')) {
            val parts = text.split(',')
            if (parts.size == 2) {
                val x = parts[0].trim().toIntOrNull()
                val y = parts[1].trim().toIntOrNull()
                if (x != null && y != null) {
                    return ElementIdentifier.ByCoordinate(Coordinate(x, y))
                }
            }
        }
        // For image patterns, use placeholder base64
        // In real implementation, this should open file picker
        return ElementIdentifier.ByImage(ImagePattern(base64Data = "", matchThreshold = 0.7))
    }

    private fun addTextField(id: String, prompt: String) {
        val textField = TextField().apply { promptText = prompt }
        dynamicFields[id] = textField
        parametersPane.children.add(VBox(5.0, Label(prompt), textField))
    }

    enum class ActionType {
        MOUSE_MOVE,
        MOUSE_LEFT_CLICK,
        MOUSE_RIGHT_CLICK,
        MOUSE_DOUBLE_CLICK,
        KEYBOARD_TYPE,
        WAIT;

        companion object {
            fun fromAction(action: Action): ActionType {
                return when (action) {
                    is Action.Mouse.MoveTo -> MOUSE_MOVE
                    is Action.Mouse.Click -> when {
                        action.type == MouseClickType.DOUBLE -> MOUSE_DOUBLE_CLICK
                        action.button == MouseButton.RIGHT -> MOUSE_RIGHT_CLICK
                        else -> MOUSE_LEFT_CLICK
                    }
                    is Action.Keyboard.TypeText -> KEYBOARD_TYPE
                    is Action.System.Wait -> WAIT
                    else -> throw IllegalArgumentException("Unknown action type")
                }
            }
        }
    }
}
