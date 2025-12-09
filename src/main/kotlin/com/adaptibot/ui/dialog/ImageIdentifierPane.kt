package com.adaptibot.ui.dialog

import com.adaptibot.common.model.Coordinate
import com.adaptibot.common.model.ElementIdentifier
import com.adaptibot.common.model.ImagePattern
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox

class ImageIdentifierPane : VBox(10.0) {
    
    private val identifierTypeCombo = ComboBox<IdentifierType>()
    private val xField = TextField()
    private val yField = TextField()
    private val thresholdField = TextField()
    private val imageDataField = TextArea()
    private val coordinatePane = GridPane()
    private val imagePane = VBox(5.0)
    
    init {
        padding = Insets(10.0)
        
        identifierTypeCombo.items.addAll(IdentifierType.values())
        identifierTypeCombo.value = IdentifierType.BY_COORDINATE
        
        identifierTypeCombo.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            updatePanes(newValue)
        }
        
        xField.promptText = "X coordinate"
        yField.promptText = "Y coordinate"
        thresholdField.promptText = "0.7"
        thresholdField.text = "0.7"
        imageDataField.promptText = "Base64 image data"
        imageDataField.prefRowCount = 3
        imageDataField.isWrapText = true
        
        coordinatePane.apply {
            hgap = 10.0
            vgap = 5.0
            add(Label("X:"), 0, 0)
            add(xField, 1, 0)
            add(Label("Y:"), 0, 1)
            add(yField, 1, 1)
        }
        
        val captureButton = Button("Capture Image...").apply {
            setOnAction {
                captureImage()
            }
        }
        
        imagePane.children.addAll(
            Label("Match Threshold (0.0 - 1.0):"),
            thresholdField,
            Label("Image Base64 Data:"),
            imageDataField,
            captureButton
        )
        
        children.addAll(
            HBox(10.0).apply {
                children.addAll(Label("Identifier Type:"), identifierTypeCombo)
            },
            coordinatePane,
            imagePane
        )
        
        updatePanes(IdentifierType.BY_COORDINATE)
    }
    
    private fun updatePanes(type: IdentifierType) {
        when (type) {
            IdentifierType.BY_COORDINATE -> {
                coordinatePane.isVisible = true
                coordinatePane.isManaged = true
                imagePane.isVisible = false
                imagePane.isManaged = false
            }
            IdentifierType.BY_IMAGE -> {
                coordinatePane.isVisible = false
                coordinatePane.isManaged = false
                imagePane.isVisible = true
                imagePane.isManaged = true
            }
        }
    }
    
    fun setIdentifier(identifier: ElementIdentifier) {
        when (identifier) {
            is ElementIdentifier.ByCoordinate -> {
                identifierTypeCombo.value = IdentifierType.BY_COORDINATE
                xField.text = identifier.coordinate.x.toString()
                yField.text = identifier.coordinate.y.toString()
            }
            is ElementIdentifier.ByImage -> {
                identifierTypeCombo.value = IdentifierType.BY_IMAGE
                thresholdField.text = identifier.pattern.matchThreshold.toString()
                imageDataField.text = identifier.pattern.base64Data
            }
        }
    }
    
    fun getIdentifier(): ElementIdentifier? {
        return when (identifierTypeCombo.value) {
            IdentifierType.BY_COORDINATE -> {
                val x = xField.text.toIntOrNull() ?: return null
                val y = yField.text.toIntOrNull() ?: return null
                ElementIdentifier.ByCoordinate(Coordinate(x, y))
            }
            IdentifierType.BY_IMAGE -> {
                val threshold = thresholdField.text.toDoubleOrNull() ?: 0.7
                val imageData = imageDataField.text
                if (imageData.isBlank()) return null
                ElementIdentifier.ByImage(ImagePattern(imageData, threshold))
            }
            null -> null
        }
    }
    
    private fun captureImage() {
        try {
            val dialog = ScreenCaptureDialog()
            val result = dialog.showAndWait()
            
            result.ifPresent { captureResult ->
                imageDataField.text = captureResult.base64Data
            }
        } catch (e: Exception) {
            val alert = javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR)
            alert.title = "Capture Error"
            alert.headerText = "Failed to capture image"
            alert.contentText = e.message
            alert.showAndWait()
        }
    }
    
    enum class IdentifierType {
        BY_COORDINATE,
        BY_IMAGE
    }
}

