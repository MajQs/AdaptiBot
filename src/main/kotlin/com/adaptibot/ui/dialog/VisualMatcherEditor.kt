package com.adaptibot.ui.dialog

import com.adaptibot.script.value.ImagePattern
import com.adaptibot.script.value.VisualMatcher
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
 * Supports:
 * - [VisualMatcher.ImagePresent] – capture a screen region and set a match threshold.
 * - [VisualMatcher.ColorAt]      – pick a single pixel (coordinates + colour) in one click.
 *
 * The active type is chosen via a [ToggleGroup] with two [RadioButton]s at the top,
 * consistent with the existing [MouseTargetEditor] pattern in the codebase.
 */
class VisualMatcherEditor(
    initial: VisualMatcher? = null
) : VBox(8.0) {

    // ── Type selector ──────────────────────────────────────────────────────
    private val typeGroup       = ToggleGroup()
    private val imagePresentBtn = RadioButton("🖼 Image Match").apply {
        toggleGroup = typeGroup; styleClass.add("form-label")
    }
    private val colorAtBtn      = RadioButton("🎨 Color At").apply {
        toggleGroup = typeGroup; styleClass.add("form-label")
    }

    // ── ImagePresent section ───────────────────────────────────────────────
    private var capturedBase64: String? = null
    private val imagePreview    = ImageView().apply { fitWidth = 160.0; fitHeight = 80.0; isPreserveRatio = true }
    private val thresholdSpinner = Spinner<Double>(0.1, 1.0, 0.7, 0.05).apply {
        styleClass.add("spinner"); prefWidth = 90.0; isEditable = true
    }
    private val captureBtn    = Button("📷 Capture Region").apply { styleClass.add("toolbar-btn") }
    private val noImageLabel  = Label("No image selected").apply { styleClass.add("step-detail-text") }
    private val imagePresentSection: VBox

    // ── ColorAt section ────────────────────────────────────────────────────
    private val colorAtSection: ColorAtSection

    init {
        style = "-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;"

        // Build ImagePresent section
        val previewBox   = HBox(8.0, imagePreview, noImageLabel).apply { alignment = Pos.CENTER_LEFT }
        val thresholdRow = HBox(8.0,
            Label("Match threshold:").apply { styleClass.add("form-label") }, thresholdSpinner
        ).apply { alignment = Pos.CENTER_LEFT }
        imagePresentSection = VBox(8.0, previewBox, thresholdRow, captureBtn)

        // Build ColorAt section based on initial value (if any)
        colorAtSection = ColorAtSection(
            initial = if (initial is VisualMatcher.ColorAt) initial else null
        )

        // Radio row
        val radioRow = HBox(16.0, imagePresentBtn, colorAtBtn).apply { alignment = Pos.CENTER_LEFT }

        children.addAll(radioRow, imagePresentSection, colorAtSection)

        // ── Wiring ─────────────────────────────────────────────────────────
        captureBtn.setOnAction { launchImageCapture() }
        typeGroup.selectedToggleProperty().addListener { _ -> refreshSections() }

        // ── Restore initial state ──────────────────────────────────────────
        when (initial) {
            is VisualMatcher.ImagePresent -> {
                capturedBase64 = initial.pattern.base64Data
                thresholdSpinner.valueFactory.value = initial.pattern.matchThreshold
                showImagePreview(initial.pattern.base64Data)
                imagePresentBtn.isSelected = true
            }
            is VisualMatcher.ColorAt -> colorAtBtn.isSelected = true
            null                     -> imagePresentBtn.isSelected = true
        }

        refreshSections()
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Returns the current [VisualMatcher], or `null` when the configuration
     * is incomplete (e.g. no image captured, or X/Y missing for ColorAt).
     */
    fun getMatcher(): VisualMatcher? {
        return when {
            imagePresentBtn.isSelected -> {
                val b64 = capturedBase64 ?: return null
                VisualMatcher.ImagePresent(ImagePattern(b64, thresholdSpinner.value))
            }
            else -> colorAtSection.getColorAt()
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private fun refreshSections() {
        val showImage = imagePresentBtn.isSelected
        imagePresentSection.isVisible  = showImage
        imagePresentSection.isManaged  = showImage
        colorAtSection.isVisible       = !showImage
        colorAtSection.isManaged       = !showImage
    }

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
                    imagePreview.image     = fxImage
                    noImageLabel.isVisible = false
                    noImageLabel.isManaged = false
                }
            } catch (_: Exception) {
                Platform.runLater { noImageLabel.isVisible = true; noImageLabel.isManaged = true }
            }
        }.also { it.isDaemon = true; it.start() }
    }
}
