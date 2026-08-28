package com.adaptibot.ui.dialog

import com.adaptibot.script.value.ElementLocation
import com.adaptibot.script.value.ScreenRect
import com.adaptibot.ui.util.ScreenRegionPicker
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.RadioButton
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.stage.Window

/**
 * Lets the user declare how the searched element behaves on screen.
 * The declaration is what drives the search area - and therefore the speed - of every lookup.
 */
class ElementLocationEditor(
    initial: ElementLocation = ElementLocation.Anywhere
) : VBox(6.0) {

    private val group = ToggleGroup()

    private val anywhereRadio = RadioButton("Can appear anywhere on screen").apply {
        toggleGroup = group; styleClass.add("form-label")
    }
    private val movesWithinRadio = RadioButton("Moves, but only within a selected area").apply {
        toggleGroup = group; styleClass.add("form-label")
    }
    private val fixedRadio = RadioButton("Always in the same place").apply {
        toggleGroup = group; styleClass.add("form-label")
    }

    private var selectedArea: ScreenRect? = null

    private val areaLabel = Label("No area selected").apply { styleClass.add("step-detail-text") }
    private val selectAreaBtn = Button("🖵 Select Area").apply { styleClass.add("toolbar-btn") }
    private val areaRow = HBox(8.0, selectAreaBtn, areaLabel).apply { alignment = Pos.CENTER_LEFT }

    init {
        style = "-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;"

        children.addAll(
            Label("Where is this element?").apply { styleClass.add("form-label") },
            anywhereRadio,
            movesWithinRadio,
            areaRow,
            fixedRadio
        )

        selectAreaBtn.setOnAction { launchAreaPicker() }
        group.selectedToggleProperty().addListener { _ -> refreshAreaRow() }

        when (initial) {
            is ElementLocation.Anywhere -> anywhereRadio.isSelected = true
            is ElementLocation.Fixed -> fixedRadio.isSelected = true
            is ElementLocation.MovesWithin -> {
                movesWithinRadio.isSelected = true
                selectedArea = initial.bounds
                areaLabel.text = initial.bounds.toString()
            }
        }

        refreshAreaRow()
    }

    /** Returns the declared location, or `null` when an area was promised but never selected. */
    fun getLocation(): ElementLocation? = when {
        movesWithinRadio.isSelected -> selectedArea?.let { ElementLocation.MovesWithin(it) }
        fixedRadio.isSelected -> ElementLocation.Fixed
        else -> ElementLocation.Anywhere
    }

    private fun refreshAreaRow() {
        areaRow.isVisible = movesWithinRadio.isSelected
        areaRow.isManaged = movesWithinRadio.isSelected
    }

    private fun launchAreaPicker() {
        val visible = hideAllWindows()
        ScreenRegionPicker.pickRegion(
            onSelect = { region ->
                selectedArea = region
                areaLabel.text = region.toString()
                restoreAllWindows(visible)
            },
            onCancel = { restoreAllWindows(visible) }
        )
    }

    private fun hideAllWindows(): List<Stage> =
        Window.getWindows().filterIsInstance<Stage>().filter { it.isShowing }.onEach { it.opacity = 0.0 }

    private fun restoreAllWindows(windows: List<Stage>) {
        windows.forEach { it.opacity = 1.0 }
        windows.lastOrNull()?.toFront()
    }
}

