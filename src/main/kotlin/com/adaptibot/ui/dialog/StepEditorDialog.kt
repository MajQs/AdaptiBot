package com.adaptibot.ui.dialog

import com.adaptibot.common.model.*
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.stage.Modality
import java.util.*

class StepEditorDialog(private val existingStep: Step.ActionStep? = null) : Dialog<Step.ActionStep>() {

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

    private fun updateParameterFields(actionType: ActionType?) {
        parametersPane.children.clear()
        dynamicFields.clear()

        if (actionType == null) return

        val grid = GridPane().apply {
            hgap = 10.0
            vgap = 10.0
        }

        var row = 0

        when (actionType) {
            ActionType.MOUSE_MOVE -> {
                grid.add(Label("X Coordinate:"), 0, row)
                val xField = TextField().apply { promptText = "100" }
                grid.add(xField, 1, row)
                dynamicFields["x"] = xField
                row++

                grid.add(Label("Y Coordinate:"), 0, row)
                val yField = TextField().apply { promptText = "200" }
                grid.add(yField, 1, row)
                dynamicFields["y"] = yField
            }

            ActionType.MOUSE_LEFT_CLICK, ActionType.MOUSE_RIGHT_CLICK, ActionType.MOUSE_DOUBLE_CLICK -> {
                grid.add(Label("X Coordinate:"), 0, row)
                val xField = TextField().apply { promptText = "100 (optional)" }
                grid.add(xField, 1, row)
                dynamicFields["x"] = xField
                row++

                grid.add(Label("Y Coordinate:"), 0, row)
                val yField = TextField().apply { promptText = "200 (optional)" }
                grid.add(yField, 1, row)
                dynamicFields["y"] = yField
            }

            ActionType.KEYBOARD_TYPE -> {
                grid.add(Label("Text to Type:"), 0, row)
                val textField = TextField().apply { promptText = "Hello World" }
                grid.add(textField, 1, row)
                dynamicFields["text"] = textField
            }

            ActionType.KEYBOARD_PRESS_KEY -> {
                grid.add(Label("Key Name:"), 0, row)
                val keyField = TextField().apply { promptText = "ENTER, TAB, etc." }
                grid.add(keyField, 1, row)
                dynamicFields["key"] = keyField
            }

            ActionType.WAIT -> {
                grid.add(Label("Duration (ms):"), 0, row)
                val durationField = TextField().apply { promptText = "1000" }
                grid.add(durationField, 1, row)
                dynamicFields["duration"] = durationField
            }

            ActionType.JUMP_TO_LABEL -> {
                grid.add(Label("Target Label:"), 0, row)
                val labelField = TextField().apply { promptText = "target-step-id" }
                grid.add(labelField, 1, row)
                dynamicFields["targetLabel"] = labelField
            }
        }

        parametersPane.children.add(grid)
    }

    private fun loadStepData(step: Step.ActionStep) {
        stepIdField.text = step.id.value
        labelField.text = step.label ?: ""

        when (val action = step.action) {
            is Action.Mouse.MoveTo -> {
                actionTypeComboBox.value = ActionType.MOUSE_MOVE
                when (val target = action.target) {
                    is ElementIdentifier.ByCoordinate -> {
                        (dynamicFields["x"] as? TextField)?.text = target.coordinate.x.toString()
                        (dynamicFields["y"] as? TextField)?.text = target.coordinate.y.toString()
                    }
                    else -> {}
                }
            }

            is Action.Mouse.LeftClick -> {
                actionTypeComboBox.value = ActionType.MOUSE_LEFT_CLICK
                when (val target = action.target) {
                    is ElementIdentifier.ByCoordinate -> {
                        (dynamicFields["x"] as? TextField)?.text = target.coordinate.x.toString()
                        (dynamicFields["y"] as? TextField)?.text = target.coordinate.y.toString()
                    }
                    else -> {}
                }
            }

            is Action.Mouse.RightClick -> {
                actionTypeComboBox.value = ActionType.MOUSE_RIGHT_CLICK
                when (val target = action.target) {
                    is ElementIdentifier.ByCoordinate -> {
                        (dynamicFields["x"] as? TextField)?.text = target.coordinate.x.toString()
                        (dynamicFields["y"] as? TextField)?.text = target.coordinate.y.toString()
                    }
                    else -> {}
                }
            }
            
            is Action.Mouse.DoubleClick -> {
                actionTypeComboBox.value = ActionType.MOUSE_DOUBLE_CLICK
                when (val target = action.target) {
                    is ElementIdentifier.ByCoordinate -> {
                        (dynamicFields["x"] as? TextField)?.text = target.coordinate.x.toString()
                        (dynamicFields["y"] as? TextField)?.text = target.coordinate.y.toString()
                    }
                    else -> {}
                }
            }

            is Action.Keyboard.TypeText -> {
                actionTypeComboBox.value = ActionType.KEYBOARD_TYPE
                (dynamicFields["text"] as? TextField)?.text = action.text
            }

            is Action.Keyboard.PressKey -> {
                actionTypeComboBox.value = ActionType.KEYBOARD_PRESS_KEY
                (dynamicFields["key"] as? TextField)?.text = action.key
            }

            is Action.System.Wait -> {
                actionTypeComboBox.value = ActionType.WAIT
                (dynamicFields["duration"] as? TextField)?.text = action.milliseconds.toString()
            }

            is Action.Flow.JumpTo -> {
                actionTypeComboBox.value = ActionType.JUMP_TO_LABEL
                (dynamicFields["targetLabel"] as? TextField)?.text = action.targetStepId.value
            }

            else -> {}
        }
    }

    private fun buildActionStep(): Step.ActionStep {
        val stepId = StepId(stepIdField.text)
        val label = labelField.text
        val actionType = actionTypeComboBox.value

        val action = when (actionType) {
            ActionType.MOUSE_MOVE -> {
                val x = (dynamicFields["x"] as TextField).text.toIntOrNull() ?: 0
                val y = (dynamicFields["y"] as TextField).text.toIntOrNull() ?: 0
                Action.Mouse.MoveTo(ElementIdentifier.ByCoordinate(Coordinate(x, y)))
            }

            ActionType.MOUSE_LEFT_CLICK -> {
                val x = (dynamicFields["x"] as TextField).text.toIntOrNull() ?: 0
                val y = (dynamicFields["y"] as TextField).text.toIntOrNull() ?: 0
                Action.Mouse.LeftClick(ElementIdentifier.ByCoordinate(Coordinate(x, y)))
            }

            ActionType.MOUSE_RIGHT_CLICK -> {
                val x = (dynamicFields["x"] as TextField).text.toIntOrNull() ?: 0
                val y = (dynamicFields["y"] as TextField).text.toIntOrNull() ?: 0
                Action.Mouse.RightClick(ElementIdentifier.ByCoordinate(Coordinate(x, y)))
            }

            ActionType.MOUSE_DOUBLE_CLICK -> {
                val x = (dynamicFields["x"] as TextField).text.toIntOrNull() ?: 0
                val y = (dynamicFields["y"] as TextField).text.toIntOrNull() ?: 0
                Action.Mouse.DoubleClick(ElementIdentifier.ByCoordinate(Coordinate(x, y)))
            }

            ActionType.KEYBOARD_TYPE -> {
                val text = (dynamicFields["text"] as TextField).text
                Action.Keyboard.TypeText(text)
            }

            ActionType.KEYBOARD_PRESS_KEY -> {
                val key = (dynamicFields["key"] as TextField).text
                Action.Keyboard.PressKey(key)
            }

            ActionType.WAIT -> {
                val duration = (dynamicFields["duration"] as TextField).text.toLongOrNull() ?: 1000L
                Action.System.Wait(duration)
            }

            ActionType.JUMP_TO_LABEL -> {
                val targetLabel = (dynamicFields["targetLabel"] as TextField).text
                Action.Flow.JumpTo(StepId(targetLabel))
            }

            else -> Action.System.Wait(1000L) // Default fallback
        }

        return Step.ActionStep(
            id = stepId,
            label = label,
            action = action
        )
    }

    enum class ActionType {
        MOUSE_MOVE,
        MOUSE_LEFT_CLICK,
        MOUSE_RIGHT_CLICK,
        MOUSE_DOUBLE_CLICK,
        KEYBOARD_TYPE,
        KEYBOARD_PRESS_KEY,
        WAIT,
        JUMP_TO_LABEL
    }
}

