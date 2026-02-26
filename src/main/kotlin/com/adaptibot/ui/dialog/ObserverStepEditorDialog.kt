package com.adaptibot.ui.dialog

import com.adaptibot.model.ObserverStep
import com.adaptibot.model.StepId
import com.adaptibot.ui.view.ConditionBuilderPane
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.stage.Modality

class ObserverStepEditorDialog(private val existingBlock: ObserverStep? = null) : Dialog<ObserverStep>() {

    private val stepIdField = TextField()
    private val labelField = TextField()
    private val delayBeforeField = TextField()
    private val delayAfterField = TextField()
    private val conditionBuilder = ConditionBuilderPane()

    init {
        title = if (existingBlock == null) "Add New Observer" else "Edit Observer"
        headerText = "Configure the observer properties"

        initModality(Modality.APPLICATION_MODAL)
        isResizable = true
        width = 700.0
        height = 600.0

        dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

        val grid = GridPane().apply {
            hgap = 10.0
            vgap = 10.0
            padding = Insets(20.0, 20.0, 10.0, 20.0)
        }

        grid.add(Label("Step ID:"), 0, 0)
        grid.add(stepIdField, 1, 0)
        stepIdField.promptText = "unique-observer-id"

        grid.add(Label("Label (optional):"), 0, 1)
        grid.add(labelField, 1, 1)
        labelField.promptText = "Optional description"

        grid.add(Label("Delay Before (ms):"), 0, 2)
        grid.add(delayBeforeField, 1, 2)
        delayBeforeField.promptText = "0"
        delayBeforeField.text = "0"

        grid.add(Label("Delay After (ms):"), 0, 3)
        grid.add(delayAfterField, 1, 3)
        delayAfterField.promptText = "0"
        delayAfterField.text = "0"

        val conditionSection = VBox(10.0).apply {
            children.addAll(
                Label("Observer Condition (triggers when true):").apply {
                    style = "-fx-font-weight: bold; -fx-font-size: 14px;"
                },
                conditionBuilder
            )
            padding = Insets(10.0, 0.0, 10.0, 0.0)
        }

        val infoLabel = Label("Note: Add action steps for this observer after creation").apply {
            style = "-fx-text-fill: gray; -fx-font-style: italic;"
        }

        val contentBox = VBox(15.0).apply {
            children.addAll(grid, Separator(), conditionSection, Separator(), infoLabel)
            padding = Insets(10.0)
        }

        dialogPane.content = contentBox

        existingBlock?.let { loadBlockData(it) }

        setResultConverter { buttonType ->
            if (buttonType == ButtonType.OK) {
                buildObserverStep()
            } else {
                null
            }
        }

        val okButton = dialogPane.lookupButton(ButtonType.OK) as Button
        okButton.isDisable = true

        stepIdField.textProperty().addListener { _, _, _ ->
            updateOkButtonState(okButton)
        }

        conditionBuilder.conditionProperty.addListener { _, _, _ ->
            updateOkButtonState(okButton)
        }
    }

    private fun updateOkButtonState(okButton: Button) {
        okButton.isDisable = stepIdField.text.isNullOrBlank() || conditionBuilder.getCondition() == null
    }

    private fun loadBlockData(block: ObserverStep) {
        stepIdField.text = block.id.value
        labelField.text = block.label
        delayBeforeField.text = block.delayBefore.toString()
        delayAfterField.text = block.delayAfter.toString()
        conditionBuilder.setCondition(block.condition)
    }

    private fun buildObserverStep(): ObserverStep {
        val id = if (existingBlock != null) existingBlock.id else StepId(stepIdField.text)
        val condition = conditionBuilder.getCondition() ?: throw IllegalStateException("Condition cannot be null")

        return ObserverStep(
            id = id,
            label = labelField.text.takeIf { it.isNotBlank() },
            delayBefore = delayBeforeField.text.toLongOrNull() ?: 0,
            delayAfter = delayAfterField.text.toLongOrNull() ?: 0,
            condition = condition,
            actionSteps = existingBlock?.actionSteps ?: emptyList()
        )
    }
}
