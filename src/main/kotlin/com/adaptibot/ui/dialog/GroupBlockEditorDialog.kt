package com.adaptibot.ui.dialog

import com.adaptibot.common.model.Step
import com.adaptibot.common.model.StepId
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.stage.Modality
import java.util.*

class GroupBlockEditorDialog(private val existingGroup: Step.GroupBlock? = null) : Dialog<Step.GroupBlock>() {

    private val stepIdField = TextField()
    private val groupNameField = TextField()
    private val labelField = TextField()
    private val delayBeforeField = TextField()
    private val delayAfterField = TextField()

    init {
        title = if (existingGroup == null) "Add New Group Block" else "Edit Group Block"
        headerText = "Configure the group block properties"

        initModality(Modality.APPLICATION_MODAL)
        isResizable = false

        dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

        val grid = GridPane().apply {
            hgap = 10.0
            vgap = 10.0
            padding = Insets(20.0, 150.0, 10.0, 10.0)
        }

        grid.add(Label("Step ID:"), 0, 0)
        grid.add(stepIdField, 1, 0)
        stepIdField.promptText = "unique-group-id"

        grid.add(Label("Group Name:"), 0, 1)
        grid.add(groupNameField, 1, 1)
        groupNameField.promptText = "e.g., Login Process, Search Flow"

        grid.add(Label("Label (optional):"), 0, 2)
        grid.add(labelField, 1, 2)
        labelField.promptText = "Optional description"

        grid.add(Label("Delay Before (ms):"), 0, 3)
        grid.add(delayBeforeField, 1, 3)
        delayBeforeField.promptText = "0"
        delayBeforeField.text = "0"

        grid.add(Label("Delay After (ms):"), 0, 4)
        grid.add(delayAfterField, 1, 4)
        delayAfterField.promptText = "0"
        delayAfterField.text = "0"

        val infoLabel = Label("Note: Add steps to this group after creation").apply {
            style = "-fx-text-fill: gray; -fx-font-style: italic;"
        }

        val contentBox = VBox(15.0).apply {
            children.addAll(grid, Separator(), infoLabel)
            padding = Insets(10.0)
        }

        dialogPane.content = contentBox

        existingGroup?.let { loadGroupData(it) }

        setResultConverter { buttonType ->
            if (buttonType == ButtonType.OK) {
                buildGroupBlock()
            } else {
                null
            }
        }

        val okButton = dialogPane.lookupButton(ButtonType.OK) as Button
        okButton.isDisable = true

        stepIdField.textProperty().addListener { _, _, newValue ->
            okButton.isDisable = newValue.isNullOrBlank() || groupNameField.text.isNullOrBlank()
        }
        groupNameField.textProperty().addListener { _, _, newValue ->
            okButton.isDisable = stepIdField.text.isNullOrBlank() || newValue.isNullOrBlank()
        }
    }

    private fun loadGroupData(group: Step.GroupBlock) {
        stepIdField.text = group.id.value
        stepIdField.isDisable = true // Don't allow changing ID when editing
        groupNameField.text = group.name
        labelField.text = group.label ?: ""
        delayBeforeField.text = group.delayBefore.toString()
        delayAfterField.text = group.delayAfter.toString()
    }

    private fun buildGroupBlock(): Step.GroupBlock {
        val stepId = StepId(stepIdField.text)
        val groupName = groupNameField.text
        val label = labelField.text.takeIf { it.isNotBlank() }
        val delayBefore = delayBeforeField.text.toLongOrNull() ?: 0L
        val delayAfter = delayAfterField.text.toLongOrNull() ?: 0L

        return Step.GroupBlock(
            id = stepId,
            label = label,
            delayBefore = delayBefore,
            delayAfter = delayAfter,
            name = groupName,
            steps = existingGroup?.steps ?: emptyList()
        )
    }
}


