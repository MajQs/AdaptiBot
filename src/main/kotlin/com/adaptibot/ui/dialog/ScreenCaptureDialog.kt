package com.adaptibot.ui.dialog

import com.adaptibot.serialization.image.ImageEncoder
import com.adaptibot.vision.capture.ScreenCapture
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage

class ScreenCaptureDialog : Dialog<CaptureResult>() {
    
    private val logger = LoggerFactory.getLogger(ScreenCaptureDialog::class.java)
    private var capturedImage: BufferedImage? = null
    private val imagePreview = ImageView()
    
    init {
        title = "Capture Screen Region"
        headerText = "Select a region on your screen to capture"
        
        initModality(Modality.APPLICATION_MODAL)
        isResizable = true
        width = 600.0
        height = 500.0
        
        dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)
        
        val captureButton = Button("Start Capture").apply {
            setOnAction {
                startCapture()
            }
        }
        
        val previewBox = VBox(10.0).apply {
            children.add(imagePreview)
            style = "-fx-border-color: lightgray; -fx-border-width: 1; -fx-alignment: center;"
            minHeight = 300.0
        }
        
        val instructionLabel = javafx.scene.control.Label(
            "Click 'Start Capture' and drag to select a region on the screen"
        ).apply {
            isWrapText = true
            style = "-fx-text-fill: gray;"
        }
        
        val content = VBox(15.0).apply {
            padding = Insets(10.0)
            children.addAll(
                instructionLabel,
                captureButton,
                javafx.scene.control.Label("Preview:"),
                previewBox
            )
        }
        
        dialogPane.content = content
        
        setResultConverter { buttonType ->
            logger.info("ResultConverter called: buttonType=$buttonType, capturedImage=${if (capturedImage != null) "present" else "null"}")
            if (buttonType == ButtonType.OK && capturedImage != null) {
                logger.info("Creating CaptureResult with image: ${capturedImage!!.width}x${capturedImage!!.height}")
                val base64 = ImageEncoder.encodeToBase64(capturedImage!!)
                logger.info("Base64 encoded, length: ${base64.length}")
                CaptureResult(capturedImage!!, base64)
            } else {
                logger.info("Returning null from ResultConverter")
                null
            }
        }
        
        val okButton = dialogPane.lookupButton(ButtonType.OK) as Button
        okButton.isDisable = true
    }
    
    private fun startCapture() {
        logger.info("startCapture called")
        val window = dialogPane.scene.window as Stage
        window.isIconified = true
        
        Thread {
            Thread.sleep(500)
            
            Platform.runLater {
                val selectionStage = createSelectionStage()
                selectionStage.initOwner(null)
                logger.info("Showing selection stage")
                selectionStage.showAndWait()
                logger.info("Selection stage closed")
                
                Platform.runLater {
                    window.isIconified = false
                    window.toFront()
                    updateUIAfterCapture()
                }
            }
        }.start()
    }
    
    private fun updateUIAfterCapture() {
        logger.info("updateUIAfterCapture called, capturedImage=${if (capturedImage != null) "present (${capturedImage!!.width}x${capturedImage!!.height})" else "null"}")
        capturedImage?.let { image ->
            logger.info("Updating UI with captured image")
            val fxImage = SwingFXUtils.toFXImage(image, null)
            imagePreview.image = fxImage
            imagePreview.fitWidth = 400.0
            imagePreview.isPreserveRatio = true
            
            val okButton = dialogPane.lookupButton(ButtonType.OK) as? Button
            logger.info("OK button found: ${okButton != null}, enabling it")
            okButton?.isDisable = false
        } ?: logger.warn("capturedImage is null, skipping UI update")
    }
    
    private fun createSelectionStage(): Stage {
        val screenshot = ScreenCapture.captureFullScreen()
        val fxImage = SwingFXUtils.toFXImage(screenshot, null)
        
        val canvas = Canvas(fxImage.width, fxImage.height)
        val gc = canvas.graphicsContext2D
        gc.drawImage(fxImage, 0.0, 0.0)
        
        var startX = 0.0
        var startY = 0.0
        var selecting = false
        
        val escapeLabel = javafx.scene.control.Label("Press ESC to cancel - Drag to select area").apply {
            style = "-fx-text-fill: white; -fx-background-color: rgba(0,0,0,0.5); -fx-padding: 10; -fx-font-size: 14px;"
            opacity = 0.8
        }
        
        canvas.setOnMousePressed { event ->
            if (event.button == MouseButton.PRIMARY) {
                startX = event.x
                startY = event.y
                selecting = true
                escapeLabel.isVisible = false
            }
        }
        
        canvas.setOnMouseDragged { event ->
            if (selecting) {
                gc.clearRect(0.0, 0.0, canvas.width, canvas.height)
                gc.drawImage(fxImage, 0.0, 0.0)
                
                val currentX = event.x
                val currentY = event.y
                val width = currentX - startX
                val height = currentY - startY
                
                gc.stroke = Color.RED
                gc.lineWidth = 2.0
                gc.strokeRect(startX, startY, width, height)
                
                gc.fill = Color.color(1.0, 0.0, 0.0, 0.2)
                gc.fillRect(startX, startY, width, height)
            }
        }
        
        canvas.setOnMouseReleased { event ->
            if (selecting && event.button == MouseButton.PRIMARY) {
                val endX = event.x
                val endY = event.y
                
                val x = minOf(startX, endX).toInt()
                val y = minOf(startY, endY).toInt()
                val width = kotlin.math.abs(endX - startX).toInt()
                val height = kotlin.math.abs(endY - startY).toInt()
                
                if (width > 5 && height > 5) {
                    captureRegionFromScreenshot(screenshot, x, y, width, height)
                    (canvas.scene.window as Stage).close()
                }
                
                selecting = false
            }
        }
        
        val root = javafx.scene.layout.StackPane().apply {
            children.addAll(
                canvas,
                javafx.scene.layout.VBox().apply {
                    children.add(escapeLabel)
                    style = "-fx-alignment: top-center; -fx-padding: 20;"
                    isMouseTransparent = true
                }
            )
        }
        
        val scene = Scene(root)
        scene.setOnKeyPressed { event ->
            if (event.code == javafx.scene.input.KeyCode.ESCAPE) {
                (scene.window as Stage).close()
            }
        }
        
        return Stage().apply {
            initStyle(StageStyle.UNDECORATED)
            this.scene = scene
            isFullScreen = true
        }
    }
    
    private fun captureRegion(x: Int, y: Int, width: Int, height: Int) {
        try {
            val region = ScreenCapture.captureRegion(x, y, width, height)
            capturedImage = region
            logger.info("Captured region: x=$x, y=$y, width=$width, height=$height")
        } catch (e: Exception) {
            logger.error("Failed to capture region", e)
            capturedImage = null
        }
    }
    
    private fun captureRegionFromScreenshot(screenshot: BufferedImage, x: Int, y: Int, width: Int, height: Int) {
        try {
            val region = screenshot.getSubimage(x, y, width, height)
            capturedImage = region
            logger.info("Captured region from screenshot: x=$x, y=$y, width=$width, height=$height, image=${region.width}x${region.height}")
            logger.info("capturedImage field is now: ${if (capturedImage != null) "set (${capturedImage!!.width}x${capturedImage!!.height})" else "null"}")
        } catch (e: Exception) {
            logger.error("Failed to capture region from screenshot", e)
            capturedImage = null
        }
    }
}

data class CaptureResult(
    val image: BufferedImage,
    val base64Data: String
)

