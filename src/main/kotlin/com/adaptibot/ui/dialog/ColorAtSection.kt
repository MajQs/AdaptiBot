package com.adaptibot.ui.dialog

import com.adaptibot.script.value.ColorTolerance
import com.adaptibot.script.value.Coordinate
import com.adaptibot.script.value.PixelColor
import com.adaptibot.script.value.VisualMatcher
import com.adaptibot.ui.util.ColorPickerOverlay
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.Stage
import javafx.stage.Window

/**
 * Compound control for configuring a [VisualMatcher.ColorAt].
 *
 * A single "🎯 Pick" button launches [ColorPickerOverlay] which – in one click –
 * captures the screen coordinates **and** the pixel colour at that point,
 * so the user never has to configure them separately.
 *
 * The colour swatch, R/G/B/A spinners and tolerance slider are all kept in sync:
 * - Eyedropper updates all of them at once.
 * - Manual edits to any spinner update the swatch immediately.
 */
class ColorAtSection(
    initial: VisualMatcher.ColorAt? = null
) : VBox(10.0) {

    // ── Coordinate fields ──────────────────────────────────────────────────
    private val xField = TextField().apply {
        styleClass.add("form-field"); prefWidth = 72.0; promptText = "X"
    }
    private val yField = TextField().apply {
        styleClass.add("form-field"); prefWidth = 72.0; promptText = "Y"
    }

    // ── Colour swatch ──────────────────────────────────────────────────────
    private val colorSwatch = Rectangle(28.0, 28.0).apply {
        arcWidth  = 4.0
        arcHeight = 4.0
        stroke     = Color.color(1.0, 1.0, 1.0, 0.4)
        strokeWidth = 1.0
        fill       = Color.BLACK
    }

    // ── R / G / B / A spinners ─────────────────────────────────────────────
    private val rSpinner = buildChannelSpinner()
    private val gSpinner = buildChannelSpinner()
    private val bSpinner = buildChannelSpinner()
    private val aSpinner = buildChannelSpinner(defaultValue = 255)

    // ── Tolerance slider ───────────────────────────────────────────────────
    private val toleranceSlider = Slider(0.0, 255.0, 0.0).apply {
        prefWidth = 140.0; isShowTickMarks = false; blockIncrement = 1.0
    }
    private val toleranceLabel = Label("0").apply {
        styleClass.add("step-detail-text"); prefWidth = 30.0
    }

    // ── Pick button ────────────────────────────────────────────────────────
    private val pickBtn = Button("🎯 Pick").apply { styleClass.add("toolbar-btn") }

    init {
        padding = Insets(0.0)
        style   = "-fx-background-color: transparent;"

        // ── Coordinates row ────────────────────────────────────────────────
        val coordRow = HBox(8.0,
            Label("X:").apply { styleClass.add("form-label") }, xField,
            Label("Y:").apply { styleClass.add("form-label") }, yField,
            pickBtn
        ).apply { alignment = Pos.CENTER_LEFT }

        // ── Colour row ─────────────────────────────────────────────────────
        val channelRow = HBox(6.0,
            colorSwatch,
            Label("R:").apply { styleClass.add("form-label") }, rSpinner,
            Label("G:").apply { styleClass.add("form-label") }, gSpinner,
            Label("B:").apply { styleClass.add("form-label") }, bSpinner,
            Label("A:").apply { styleClass.add("form-label") }, aSpinner
        ).apply { alignment = Pos.CENTER_LEFT }

        // ── Tolerance row ──────────────────────────────────────────────────
        val toleranceRow = HBox(8.0,
            Label("Tolerance:").apply { styleClass.add("form-label") },
            toleranceSlider,
            toleranceLabel
        ).apply { alignment = Pos.CENTER_LEFT }

        children.addAll(coordRow, channelRow, toleranceRow)

        // ── Wiring ─────────────────────────────────────────────────────────
        pickBtn.setOnAction { launchPicker() }

        listOf(rSpinner, gSpinner, bSpinner, aSpinner).forEach { s ->
            s.valueProperty().addListener { _ -> syncSwatch() }
        }
        toleranceSlider.valueProperty().addListener { _ ->
            toleranceLabel.text = toleranceSlider.value.toInt().toString()
        }

        // ── Initial values ─────────────────────────────────────────────────
        if (initial != null) setColorAt(initial) else syncSwatch()
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Returns a fully configured [VisualMatcher.ColorAt], or `null` when
     * X or Y are missing / invalid (prevents saving an incomplete condition).
     */
    fun getColorAt(): VisualMatcher.ColorAt? {
        val x   = xField.text.trim().toIntOrNull() ?: return null
        val y   = yField.text.trim().toIntOrNull() ?: return null
        return VisualMatcher.ColorAt(
            point     = Coordinate(x, y),
            expected  = PixelColor(
                r = rSpinner.value,
                g = gSpinner.value,
                b = bSpinner.value,
                a = aSpinner.value
            ),
            tolerance = ColorTolerance(toleranceSlider.value.toInt())
        )
    }

    /** Populates all fields from an existing [VisualMatcher.ColorAt]. */
    fun setColorAt(value: VisualMatcher.ColorAt) {
        xField.text = value.point.x.toString()
        yField.text = value.point.y.toString()
        rSpinner.valueFactory.value = value.expected.r
        gSpinner.valueFactory.value = value.expected.g
        bSpinner.valueFactory.value = value.expected.b
        aSpinner.valueFactory.value = value.expected.a
        toleranceSlider.value = value.tolerance.value.toDouble()
        toleranceLabel.text   = value.tolerance.value.toString()
        syncSwatch()
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private fun launchPicker() {
        pickBtn.isDisable = true
        val visible = hideAllWindows()
        ColorPickerOverlay.pick(
            onPicked = { x, y, color ->
                xField.text = x.toString()
                yField.text = y.toString()
                rSpinner.valueFactory.value = color.r
                gSpinner.valueFactory.value = color.g
                bSpinner.valueFactory.value = color.b
                aSpinner.valueFactory.value = color.a
                syncSwatch()
                restoreAllWindows(visible)
                pickBtn.isDisable = false
            },
            onCancel = {
                restoreAllWindows(visible)
                pickBtn.isDisable = false
            }
        )
    }

    private fun syncSwatch() {
        val r = rSpinner.value / 255.0
        val g = gSpinner.value / 255.0
        val b = bSpinner.value / 255.0
        val a = aSpinner.value / 255.0
        colorSwatch.fill = Color.color(
            r.coerceIn(0.0, 1.0),
            g.coerceIn(0.0, 1.0),
            b.coerceIn(0.0, 1.0),
            a.coerceIn(0.0, 1.0)
        )
    }

    private fun hideAllWindows(): List<Stage> =
        Window.getWindows().filterIsInstance<Stage>().filter { it.isShowing }.onEach { it.opacity = 0.0 }

    private fun restoreAllWindows(windows: List<Stage>) {
        windows.forEach { it.opacity = 1.0 }
        windows.lastOrNull()?.toFront()
    }

    private companion object {
        fun buildChannelSpinner(defaultValue: Int = 0): Spinner<Int> =
            Spinner<Int>(0, 255, defaultValue, 1).apply {
                styleClass.add("spinner")
                prefWidth  = 68.0
                isEditable = true
            }
    }
}

