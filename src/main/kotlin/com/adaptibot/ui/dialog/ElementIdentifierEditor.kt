package com.adaptibot.ui.dialog

import com.adaptibot.script.value.Coordinate
import com.adaptibot.script.value.ImagePattern
import com.adaptibot.script.value.Target as ScriptTarget
import com.adaptibot.serialization.ImageEncoder
import com.adaptibot.ui.util.CoordinatePicker
import com.adaptibot.ui.util.ScreenRegionPicker
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.stage.Stage
import javafx.stage.Window

/**
 * Reusable compound control that lets the user pick a [Target]:
 * either AtCoordinate (with mouse-pick helper) or AtImage (with screen-capture helper).
 *
 * Embed inside any dialog pane that configures a mouse action target.
 */
class MouseTargetEditor(
    initial: ScriptTarget? = null
) : VBox(8.0) {

    private val typeGroup  = ToggleGroup()
    private val coordRadio = RadioButton("By Coordinate").apply { toggleGroup = typeGroup }
    private val imageRadio = RadioButton("By Image").apply { toggleGroup = typeGroup }
    private val textRadio  = RadioButton("By Text").apply { toggleGroup = typeGroup }

    // Coordinate section
    private val xField       = TextField().apply { styleClass.add("form-field"); prefWidth = 80.0; promptText = "X" }
    private val yField       = TextField().apply { styleClass.add("form-field"); prefWidth = 80.0; promptText = "Y" }
    private val pickCoordBtn = Button("🖱 Pick").apply { styleClass.add("toolbar-btn") }

    // Image section
    private var capturedBase64: String? = null
    private val imagePreview    = ImageView().apply { fitWidth = 160.0; fitHeight = 80.0; isPreserveRatio = true }
    private val thresholdSpinner = Spinner<Double>(0.1, 1.0, 0.7, 0.05).apply {
        styleClass.add("spinner"); prefWidth = 90.0; isEditable = true
    }
    private val captureBtn   = Button("📷 Capture Region").apply { styleClass.add("toolbar-btn") }
    private val noImageLabel = Label("No image selected").apply { styleClass.add("step-detail-text") }

    // Text section
    private val textField = TextField().apply { styleClass.add("form-field"); promptText = "Text to find on screen" }

    private val coordPane = buildCoordPane()
    private val imagePane = buildImagePane()
    private val textPane  = buildTextPane()

    init {
        padding = Insets(0.0)
        style = "-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;"

        val radioRow = HBox(16.0, coordRadio, imageRadio, textRadio).apply { alignment = Pos.CENTER_LEFT }
        children.addAll(radioRow, coordPane, imagePane, textPane)

        typeGroup.selectedToggleProperty().addListener { _ -> refreshVisibility() }
        pickCoordBtn.setOnAction { launchCoordPicker() }
        captureBtn.setOnAction { launchImageCapture() }

        when (initial) {
            is ScriptTarget.AtCoordinate -> {
                coordRadio.isSelected = true
                xField.text = initial.coordinate.x.toString()
                yField.text = initial.coordinate.y.toString()
            }
            is ScriptTarget.AtImage -> {
                imageRadio.isSelected = true
                capturedBase64 = initial.pattern.base64Data
                thresholdSpinner.valueFactory.value = initial.pattern.matchThreshold
                showImagePreview(initial.pattern.base64Data)
            }
            is ScriptTarget.AtText -> {
                textRadio.isSelected = true
                textField.text = initial.text
            }
            null -> coordRadio.isSelected = true
        }
        refreshVisibility()
    }

    /** Returns the current [ScriptTarget] or null if not fully configured. */
    fun getTarget(): ScriptTarget? {
        return when {
            coordRadio.isSelected -> {
                val x = xField.text.trim().toIntOrNull() ?: return null
                val y = yField.text.trim().toIntOrNull() ?: return null
                ScriptTarget.AtCoordinate(Coordinate(x, y))
            }
            imageRadio.isSelected -> {
                val b64 = capturedBase64 ?: return null
                ScriptTarget.AtImage(ImagePattern(b64, thresholdSpinner.value))
            }
            textRadio.isSelected -> {
                val text = textField.text.trim().takeIf { it.isNotEmpty() } ?: return null
                ScriptTarget.AtText(text)
            }
            else -> null
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun refreshVisibility() {
        coordPane.isVisible  = coordRadio.isSelected
        coordPane.isManaged  = coordRadio.isSelected
        imagePane.isVisible  = imageRadio.isSelected
        imagePane.isManaged  = imageRadio.isSelected
        textPane.isVisible   = textRadio.isSelected
        textPane.isManaged   = textRadio.isSelected
    }

    private fun buildCoordPane(): VBox {
        val row = HBox(8.0,
            Label("X:").apply { styleClass.add("form-label") }, xField,
            Label("Y:").apply { styleClass.add("form-label") }, yField,
            pickCoordBtn
        ).apply { alignment = Pos.CENTER_LEFT }
        return VBox(6.0, row)
    }

    private fun buildImagePane(): VBox {
        val previewBox    = HBox(8.0, imagePreview, noImageLabel).apply { alignment = Pos.CENTER_LEFT }
        val thresholdRow  = HBox(8.0,
            Label("Match threshold:").apply { styleClass.add("form-label") }, thresholdSpinner
        ).apply { alignment = Pos.CENTER_LEFT }
        return VBox(6.0, previewBox, thresholdRow, captureBtn)
    }

    private fun buildTextPane(): VBox {
        val row = HBox(8.0,
            Label("Text:").apply { styleClass.add("form-label") }, textField
        ).apply { alignment = Pos.CENTER_LEFT }
        return VBox(6.0, row)
    }

    private fun hideAllWindows(): List<Stage> =
        Window.getWindows().filterIsInstance<Stage>().filter { it.isShowing }.onEach { it.opacity = 0.0 }

    private fun restoreAllWindows(windows: List<Stage>) {
        windows.forEach { it.opacity = 1.0 }
        windows.lastOrNull()?.toFront()
    }

    private fun launchCoordPicker() {
        val visible = hideAllWindows()
        CoordinatePicker.pick(
            onPicked = { x, y -> xField.text = x.toString(); yField.text = y.toString(); restoreAllWindows(visible) },
            onCancel = { restoreAllWindows(visible) }
        )
    }

    private fun launchImageCapture() {
        val visible = hideAllWindows()
        ScreenRegionPicker.pick(
            onCapture = { b64 -> capturedBase64 = b64; showImagePreview(b64); restoreAllWindows(visible) },
            onCancel  = { restoreAllWindows(visible) }
        )
    }

    private fun showImagePreview(base64: String) {
        Thread {
            try {
                val buffered = ImageEncoder.decodeFromBase64(base64)
                val fxImage  = SwingFXUtils.toFXImage(buffered, null)
                Platform.runLater {
                    imagePreview.image   = fxImage
                    noImageLabel.isVisible  = false
                    noImageLabel.isManaged  = false
                }
            } catch (_: Exception) {
                Platform.runLater { noImageLabel.isVisible = true; noImageLabel.isManaged = true }
            }
        }.also { it.isDaemon = true; it.start() }
    }
}
