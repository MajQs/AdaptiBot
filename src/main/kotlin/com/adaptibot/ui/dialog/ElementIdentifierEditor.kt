package com.adaptibot.ui.dialog

import com.adaptibot.model.Coordinate
import com.adaptibot.model.ElementIdentifier
import com.adaptibot.model.ImagePattern
import com.adaptibot.serialization.ImageEncoder
import com.adaptibot.ui.util.CoordinatePicker
import com.adaptibot.ui.util.ScreenRegionPicker
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.stage.Stage
import javafx.stage.Window

/**
 * Reusable compound control that lets the user pick an [ElementIdentifier]:
 * either ByCoordinate (with mouse-pick helper) or ByImage (with screen-capture helper).
 *
 * Embed inside any dialog pane.
 */
class ElementIdentifierEditor(
    initial: ElementIdentifier? = null,
    private val ownerWindow: Window? = null
) : VBox(8.0) {

    private val typeGroup = ToggleGroup()
    private val coordRadio = RadioButton("By Coordinate").apply { toggleGroup = typeGroup }
    private val imageRadio = RadioButton("By Image").apply { toggleGroup = typeGroup }

    // Coordinate section
    private val xField = TextField().apply { styleClass.add("form-field"); prefWidth = 80.0; promptText = "X" }
    private val yField = TextField().apply { styleClass.add("form-field"); prefWidth = 80.0; promptText = "Y" }
    private val pickCoordBtn = Button("🖱 Pick").apply { styleClass.add("toolbar-btn") }

    // Image section
    private var capturedBase64: String? = null
    private val imagePreview = ImageView().apply { fitWidth = 160.0; fitHeight = 80.0; isPreserveRatio = true }
    private val thresholdSpinner = Spinner<Double>(0.1, 1.0, 0.7, 0.05).apply {
        styleClass.add("spinner"); prefWidth = 90.0; isEditable = true
    }
    private val captureBtn = Button("📷 Capture Region").apply { styleClass.add("toolbar-btn") }
    private val noImageLabel = Label("No image selected").apply { styleClass.add("step-detail-text") }

    private val coordPane = buildCoordPane()
    private val imagePane = buildImagePane()

    init {
        padding = Insets(0.0)
        styleClass.add("form-field")
        style = "-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;"

        val radioRow = HBox(16.0, coordRadio, imageRadio).apply { alignment = Pos.CENTER_LEFT }
        children.addAll(radioRow, coordPane, imagePane)

        typeGroup.selectedToggleProperty().addListener { _ -> refreshVisibility() }

        pickCoordBtn.setOnAction { launchCoordPicker() }
        captureBtn.setOnAction { launchImageCapture() }

        // Apply initial value
        when (initial) {
            is ElementIdentifier.ByCoordinate -> {
                coordRadio.isSelected = true
                xField.text = initial.coordinate.x.toString()
                yField.text = initial.coordinate.y.toString()
            }
            is ElementIdentifier.ByImage -> {
                imageRadio.isSelected = true
                capturedBase64 = initial.pattern.base64Data
                thresholdSpinner.valueFactory.value = initial.pattern.matchThreshold
                showImagePreview(initial.pattern.base64Data)
            }
            null -> coordRadio.isSelected = true
        }
        refreshVisibility()
    }

    /** Returns the current [ElementIdentifier] or null if nothing configured. */
    fun getIdentifier(): ElementIdentifier? {
        return when {
            coordRadio.isSelected -> {
                val x = xField.text.trim().toIntOrNull() ?: return null
                val y = yField.text.trim().toIntOrNull() ?: return null
                ElementIdentifier.ByCoordinate(Coordinate(x, y))
            }
            imageRadio.isSelected -> {
                val b64 = capturedBase64 ?: return null
                ElementIdentifier.ByImage(ImagePattern(b64, thresholdSpinner.value))
            }
            else -> null
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun refreshVisibility() {
        coordPane.isVisible = coordRadio.isSelected
        coordPane.isManaged = coordRadio.isSelected
        imagePane.isVisible = imageRadio.isSelected
        imagePane.isManaged = imageRadio.isSelected
    }

    private fun buildCoordPane(): VBox {
        val row = HBox(8.0, Label("X:").apply { styleClass.add("form-label") }, xField,
            Label("Y:").apply { styleClass.add("form-label") }, yField, pickCoordBtn)
        row.alignment = Pos.CENTER_LEFT
        return VBox(6.0, row)
    }

    private fun buildImagePane(): VBox {
        val previewBox = HBox(8.0, imagePreview, noImageLabel)
        previewBox.alignment = Pos.CENTER_LEFT
        val thresholdRow = HBox(8.0,
            Label("Match threshold:").apply { styleClass.add("form-label") }, thresholdSpinner)
        thresholdRow.alignment = Pos.CENTER_LEFT
        return VBox(6.0, previewBox, thresholdRow, captureBtn)
    }

    private fun launchCoordPicker() {
        val stage = ownerWindow as? Stage
        stage?.isIconified = true
        CoordinatePicker.pick(
            onPicked = { x, y ->
                xField.text = x.toString()
                yField.text = y.toString()
                stage?.isIconified = false
                stage?.toFront()
            },
            onCancel = {
                stage?.isIconified = false
                stage?.toFront()
            }
        )
    }

    private fun launchImageCapture() {
        val stage = ownerWindow as? Stage
        stage?.isIconified = true
        ScreenRegionPicker.pick(
            onCapture = { b64 ->
                capturedBase64 = b64
                showImagePreview(b64)
                stage?.isIconified = false
                stage?.toFront()
            },
            onCancel = {
                stage?.isIconified = false
                stage?.toFront()
            }
        )
    }

    private fun showImagePreview(base64: String) {
        try {
            val buffered = ImageEncoder.decodeFromBase64(base64)
            val fxImage = SwingFXUtils.toFXImage(buffered, null)
            imagePreview.image = fxImage
            noImageLabel.isVisible = false
            noImageLabel.isManaged = false
        } catch (_: Exception) {
            noImageLabel.isVisible = true
            noImageLabel.isManaged = true
        }
    }
}

