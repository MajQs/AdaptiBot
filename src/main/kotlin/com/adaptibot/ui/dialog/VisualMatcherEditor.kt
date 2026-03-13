package com.adaptibot.ui.dialog

import com.adaptibot.model.ImagePattern
import com.adaptibot.model.VisualMatcher
import com.adaptibot.serialization.ImageEncoder
import com.adaptibot.ui.util.ScreenRegionPicker
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.stage.Stage
import javafx.stage.Window

/**
 * Reusable compound control that lets the user configure a [VisualMatcher].
 *
 * Currently only [VisualMatcher.ImagePresent] is supported (no coordinates –
 * coordinates carry no meaning in a visual condition).
 * New matcher types can be added here independently of mouse-target logic.
 */
class VisualMatcherEditor(
    initial: VisualMatcher? = null
) : VBox(8.0) {

    private var capturedBase64: String? = null
    private val imagePreview    = ImageView().apply { fitWidth = 160.0; fitHeight = 80.0; isPreserveRatio = true }
    private val thresholdSpinner = Spinner<Double>(0.1, 1.0, 0.7, 0.05).apply {
        styleClass.add("spinner"); prefWidth = 90.0; isEditable = true
    }
    private val captureBtn   = Button("📷 Capture Region").apply { styleClass.add("toolbar-btn") }
    private val noImageLabel = Label("No image selected").apply { styleClass.add("step-detail-text") }

    init {
        style = "-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;"

        val previewBox   = HBox(8.0, imagePreview, noImageLabel).apply { alignment = Pos.CENTER_LEFT }
        val thresholdRow = HBox(8.0,
            Label("Match threshold:").apply { styleClass.add("form-label") }, thresholdSpinner
        ).apply { alignment = Pos.CENTER_LEFT }

        children.addAll(previewBox, thresholdRow, captureBtn)
        captureBtn.setOnAction { launchImageCapture() }

        when (initial) {
            is VisualMatcher.ImagePresent -> {
                capturedBase64 = initial.pattern.base64Data
                thresholdSpinner.valueFactory.value = initial.pattern.matchThreshold
                showImagePreview(initial.pattern.base64Data)
            }
            is VisualMatcher.ColorAt -> TODO()
            null -> { /* empty state */ }
        }
    }

    /** Returns the current [VisualMatcher] or null if not fully configured. */
    fun getMatcher(): VisualMatcher? {
        val b64 = capturedBase64 ?: return null
        return VisualMatcher.ImagePresent(ImagePattern(b64, thresholdSpinner.value))
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun hideAllWindows(): List<Stage> =
        Window.getWindows().filterIsInstance<Stage>().filter { it.isShowing }.onEach { it.opacity = 0.0 }

    private fun restoreAllWindows(windows: List<Stage>) {
        windows.forEach { it.opacity = 1.0 }
        windows.lastOrNull()?.toFront()
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
                    imagePreview.image      = fxImage
                    noImageLabel.isVisible  = false
                    noImageLabel.isManaged  = false
                }
            } catch (_: Exception) {
                Platform.runLater { noImageLabel.isVisible = true; noImageLabel.isManaged = true }
            }
        }.also { it.isDaemon = true; it.start() }
    }
}

