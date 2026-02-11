package com.adaptibot.ui.dialog

import com.adaptibot.common.model.*
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.stage.Modality
import java.util.*

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
            ActionType.KEYBOARD_PRESS_KEY,
            ActionType.WAIT,
            ActionType.JUMP_TO_LABEL
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
                (dynamicFields["target"] as TextField).text = elementIdentifierToString(action.target)
            }
            is Action.Mouse.LeftClick -> {
                (dynamicFields["target"] as TextField).text = elementIdentifierToString(action.target)
            }
            is Action.Mouse.RightClick -> {
                (dynamicFields["target"] as TextField).text = elementIdentifierToString(action.target)
            }
            is Action.Mouse.DoubleClick -> {
                (dynamicFields["target"] as TextField).text = elementIdentifierToString(action.target)
            }
            is Action.Keyboard.TypeText -> {
                (dynamicFields["text"] as TextField).text = action.text
            }
            is Action.Keyboard.PressKey -> {
                (dynamicFields["key"] as TextField).text = action.key
            }
            is Action.System.Wait -> {
                (dynamicFields["duration"] as TextField).text = action.milliseconds.toString()
            }
            is Action.Flow.JumpTo -> {
                (dynamicFields["label"] as TextField).text = action.targetStepId.value
            }
            else -> {}
        }
    }

    private fun buildActionStep(): ActionStep {
        val stepId = StepId(stepIdField.text)
        val label = labelField.text
        val actionType = actionTypeComboBox.value

        val action = when (actionType) {
            ActionType.MOUSE_MOVE -> Action.Mouse.MoveTo(stringToElementIdentifier((dynamicFields["target"] as TextField).text))
            ActionType.MOUSE_LEFT_CLICK -> Action.Mouse.LeftClick(stringToElementIdentifier((dynamicFields["target"] as TextField).text))
            ActionType.MOUSE_RIGHT_CLICK -> Action.Mouse.RightClick(stringToElementIdentifier((dynamicFields["target"] as TextField).text))
            ActionType.MOUSE_DOUBLE_CLICK -> Action.Mouse.DoubleClick(stringToElementIdentifier((dynamicFields["target"] as TextField).text))
            ActionType.KEYBOARD_TYPE -> Action.Keyboard.TypeText((dynamicFields["text"] as TextField).text)
            ActionType.KEYBOARD_PRESS_KEY -> Action.Keyboard.PressKey((dynamicFields["key"] as TextField).text)
            ActionType.WAIT -> Action.System.Wait((dynamicFields["duration"] as TextField).text.toLong())
            ActionType.JUMP_TO_LABEL -> Action.Flow.JumpTo(StepId((dynamicFields["label"] as TextField).text))
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
            ActionType.KEYBOARD_PRESS_KEY -> {
                addTextField("key", "Key to press (e.g., ENTER)")
            }
            ActionType.WAIT -> {
                addTextField("duration", "Duration (ms)")
            }
            ActionType.JUMP_TO_LABEL -> {
                addTextField("label", "Jump to Label")
            }
            else -> {
                // No parameters
            }
        }
    }

    private fun addTextField(id: String, prompt: String) {
        val textField = TextField().apply { promptText = prompt }
        dynamicFields[id] = textField
        parametersPane.children.add(VBox(5.0, Label(prompt), textField))
    }

    private fun elementIdentifierToString(identifier: ElementIdentifier): String {
        return when (identifier) {
            is ElementIdentifier.ByCoordinate -> "coord:${identifier.coordinate.x},${identifier.coordinate.y}"
            is ElementIdentifier.ByImage -> "image:${identifier.pattern.base64Data.take(20)}..." // Show first 20 chars
        }
    }

    private fun stringToElementIdentifier(text: String): ElementIdentifier {
        return when {
            text.startsWith("coord:") -> {
                val coords = text.substringAfter("coord:").split(",")
                ElementIdentifier.ByCoordinate(Coordinate(coords[0].toInt(), coords[1].toInt()))
            }
            text.startsWith("image:") -> {
                val imageData = text.substringAfter("image:")
                ElementIdentifier.ByImage(ImagePattern(imageData))
            }
            else -> {
                // Default: treat as coordinate string "x,y" for backward compatibility
                val coords = text.split(",")
                if (coords.size == 2) {
                    ElementIdentifier.ByCoordinate(Coordinate(coords[0].trim().toInt(), coords[1].trim().toInt()))
                } else {
                    // Fallback to image pattern
                    ElementIdentifier.ByImage(ImagePattern(text))
                }
            }
        }
    }

    enum class ActionType {
        MOUSE_MOVE,
        MOUSE_LEFT_CLICK,
        MOUSE_RIGHT_CLICK,
        MOUSE_DOUBLE_CLICK,
        KEYBOARD_TYPE,
        KEYBOARD_PRESS_KEY,
        WAIT,
        JUMP_TO_LABEL;

        companion object {
            fun fromAction(action: Action): ActionType {
                return when (action) {
                    is Action.Mouse.MoveTo -> MOUSE_MOVE
                    is Action.Mouse.LeftClick -> MOUSE_LEFT_CLICK
                    is Action.Mouse.RightClick -> MOUSE_RIGHT_CLICK
                    is Action.Mouse.DoubleClick -> MOUSE_DOUBLE_CLICK
                    is Action.Keyboard.TypeText -> KEYBOARD_TYPE
                    is Action.Keyboard.PressKey -> KEYBOARD_PRESS_KEY
                    is Action.System.Wait -> WAIT
                    is Action.Flow.JumpTo -> JUMP_TO_LABEL
                    else -> throw IllegalArgumentException("Unknown action type")
                }
            }
        }
    }
}
