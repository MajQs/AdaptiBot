package com.adaptibot.ui.dialog

import com.adaptibot.script.ScriptSettings
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.stage.Window

class ScriptSettingsDialog(
    private val current: ScriptSettings,
    private val scriptName: String,
    private val scriptDescription: String,
    owner: Window?
) : Dialog<ScriptSettingsDialog.Result>() {

    data class Result(
        val name: String,
        val description: String,
        val settings: ScriptSettings
    )

    init {
        title = "Script Settings"
        owner?.let { initOwner(it) }
        dialogPane.stylesheets.add(
            javaClass.getResource("/css/adaptibot.css")?.toExternalForm() ?: ""
        )
        dialogPane.style = "-fx-background-color: #1e1e2e;"

        val grid = GridPane().apply {
            hgap = 10.0; vgap = 10.0; padding = Insets(16.0); prefWidth = 420.0
            columnConstraints.addAll(
                ColumnConstraints(160.0),
                ColumnConstraints().apply { hgrow = Priority.ALWAYS; isFillWidth = true }
            )
        }

        fun lbl(t: String) = Label(t).apply { styleClass.add("form-label") }
        fun field(v: String = "") = TextField(v).apply { styleClass.add("form-field") }
        fun longField(v: Long) = TextField(v.toString()).apply { styleClass.add("form-field"); prefWidth = 120.0 }
        fun doubleField(v: Double) = TextField(v.toString()).apply { styleClass.add("form-field"); prefWidth = 120.0 }
        fun secTitle(t: String) = Label(t).apply { styleClass.add("form-section-title") }

        val nameField = field(scriptName)
        val descArea = TextArea(scriptDescription).apply {
            styleClass.add("form-field"); prefRowCount = 3
        }
        val defaultDelayField  = longField(current.defaultDelayBefore)
        val obsDelayField       = longField(current.observerCheckDelay)
        val imgThresholdField   = doubleField(current.defaultImageMatchThreshold)

        var row = 0
        grid.add(secTitle("GENERAL"), 0, row, 2, 1); row++
        grid.add(lbl("Script name"), 0, row); grid.add(nameField, 1, row++);
        grid.add(lbl("Description"), 0, row); grid.add(descArea, 1, row++);

        grid.add(secTitle("EXECUTION"), 0, row, 2, 1); row++
        grid.add(lbl("Default delay before (ms)"), 0, row); grid.add(defaultDelayField, 1, row++);
        grid.add(lbl("Observer check delay (ms)"), 0, row); grid.add(obsDelayField, 1, row++);
        grid.add(lbl("Default image threshold"), 0, row); grid.add(imgThresholdField, 1, row);

        dialogPane.content = grid
        dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

        setResultConverter { bt ->
            if (bt == ButtonType.OK) Result(
                name = nameField.text.trim().ifBlank { "Untitled" },
                description = descArea.text,
                settings = current.copy(
                    defaultDelayBefore = defaultDelayField.text.toLongOrNull() ?: current.defaultDelayBefore,
                    observerCheckDelay = obsDelayField.text.toLongOrNull() ?: current.observerCheckDelay,
                    defaultImageMatchThreshold = imgThresholdField.text.toDoubleOrNull() ?: current.defaultImageMatchThreshold
                )
            ) else null
        }
    }
}

