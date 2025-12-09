package com.adaptibot.ui.dialog

import com.adaptibot.common.model.Condition
import com.adaptibot.common.model.Step
import com.adaptibot.common.model.StepId
import com.adaptibot.ui.view.ConditionBuilderPane
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.stage.Modality

class ObserverBlockEditorDialog(private val existingBlock: Step.ObserverBlock? = null) : Dialog<Step.ObserverBlock>() {

    private val stepIdField = TextField()
    private val labelField = TextField()
    private val delayBeforeField = TextField()
    private val delayAfterField = TextField()
    private val conditionBuilder = ConditionBuilderPane()

    init {
        title = if (existingBlock == null) "Add New Observer Block" else "Edit Observer Block"
        headerText = "Configure the observer block properties"

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
                buildObserverBlock()
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

    private fun loadBlockData(block: Step.ObserverBlock) {
        stepIdField.text = block.id.value
        stepIdField.isDisable = true
        labelField.text = block.label ?: ""
        delayBeforeField.text = block.delayBefore.toString()
        delayAfterField.text = block.delayAfter.toString()
        conditionBuilder.setCondition(block.condition)
    }

    private fun buildObserverBlock(): Step.ObserverBlock? {
        val condition = conditionBuilder.getCondition() ?: return null
        
        val stepId = StepId(stepIdField.text)
        val label = labelField.text.takeIf { it.isNotBlank() }
        val delayBefore = delayBeforeField.text.toLongOrNull() ?: 0L
        val delayAfter = delayAfterField.text.toLongOrNull() ?: 0L

        return Step.ObserverBlock(
            id = stepId,
            label = label,
            delayBefore = delayBefore,
            delayAfter = delayAfter,
            condition = condition,
            actionSteps = existingBlock?.actionSteps ?: emptyList()
        )
    }
}


