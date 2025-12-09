package com.adaptibot.ui.view

import com.adaptibot.common.model.Condition
import com.adaptibot.common.model.Coordinate
import com.adaptibot.common.model.ElementIdentifier
import com.adaptibot.common.model.ImagePattern
import com.adaptibot.ui.dialog.ScreenCaptureDialog
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import org.slf4j.LoggerFactory

class ConditionBuilderPane : VBox(10.0) {

    private val logger = LoggerFactory.getLogger(ConditionBuilderPane::class.java)
    
    val conditionProperty = SimpleObjectProperty<Condition?>()
    
    private val conditionTypeCombo = ComboBox<ConditionType>()
    private val contentPane = VBox(10.0)
    private val subConditionsPane = VBox(5.0)
    
    private var currentSimpleConditionType: SimpleConditionType = SimpleConditionType.ELEMENT_EXISTS
    private var currentIdentifierType: IdentifierType = IdentifierType.BY_COORDINATE
    
    private val xField = TextField().apply { 
        promptText = "X coordinate"
        text = "0"
    }
    private val yField = TextField().apply { 
        promptText = "Y coordinate"
        text = "0"
    }
    private val thresholdField = TextField().apply { 
        promptText = "0.7" 
        text = "0.7"
    }
    private val imagePreview = javafx.scene.image.ImageView().apply {
        fitWidth = 200.0
        fitHeight = 150.0
        isPreserveRatio = true
        style = "-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-background-color: #fafafa;"
    }
    private val imageInfoLabel = Label("No image captured").apply {
        style = "-fx-text-fill: gray; -fx-font-style: italic; -fx-font-size: 11px;"
    }
    private var capturedImageBase64: String = ""
    
    private val subConditions = mutableListOf<ConditionBuilderPane>()
    
    private val simpleConditionPane = createSimpleConditionPane()

    init {
        padding = Insets(10.0)
        style = "-fx-border-color: lightgray; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-color: white;"
        
        conditionTypeCombo.items.addAll(ConditionType.values())
        conditionTypeCombo.value = ConditionType.SIMPLE
        
        conditionTypeCombo.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            updateContentPane(newValue)
            updateConditionProperty()
        }
        
        setupFieldListeners()
        
        val headerBox = HBox(10.0).apply {
            children.addAll(
                Label("Condition Type:"),
                conditionTypeCombo
            )
        }
        
        children.addAll(headerBox, contentPane)
        
        updateContentPane(ConditionType.SIMPLE)
        updateConditionProperty()
    }
    
    private fun setupFieldListeners() {
        xField.textProperty().addListener { _, _, _ -> updateConditionProperty() }
        yField.textProperty().addListener { _, _, _ -> updateConditionProperty() }
        thresholdField.textProperty().addListener { _, _, _ -> updateConditionProperty() }
    }
    
    private fun updateConditionProperty() {
        conditionProperty.value = getCondition()
    }
    
    private fun createSimpleConditionPane(): VBox {
        val pane = VBox(10.0)
        
        val simpleTypeCombo = ComboBox<SimpleConditionType>().apply {
            items.addAll(SimpleConditionType.values())
            value = SimpleConditionType.ELEMENT_EXISTS
            
            selectionModel.selectedItemProperty().addListener { _, _, newValue ->
                currentSimpleConditionType = newValue
                updateConditionProperty()
            }
        }
        
        val identifierTypeCombo = ComboBox<IdentifierType>().apply {
            items.addAll(IdentifierType.values())
            value = IdentifierType.BY_COORDINATE
            
            selectionModel.selectedItemProperty().addListener { _, _, newValue ->
                currentIdentifierType = newValue
                updateIdentifierFields(newValue)
                updateConditionProperty()
            }
        }
        
        val coordPane = GridPane().apply {
            hgap = 10.0
            vgap = 5.0
            add(Label("X:"), 0, 0)
            add(xField, 1, 0)
            add(Label("Y:"), 0, 1)
            add(yField, 1, 1)
        }
        
        val imagePane = VBox(8.0).apply {
            children.addAll(
                Label("Match Threshold (0.0 - 1.0):").apply {
                    style = "-fx-font-weight: bold; -fx-font-size: 12px;"
                },
                thresholdField,
                Label("Captured Image:").apply {
                    style = "-fx-font-weight: bold; -fx-font-size: 12px;"
                },
                javafx.scene.layout.StackPane().apply {
                    children.add(imagePreview)
                    style = "-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-background-color: #fafafa; -fx-padding: 10;"
                    minHeight = 170.0
                },
                imageInfoLabel,
                HBox(10.0).apply {
                    children.addAll(
                        Button("Capture Image...").apply {
                            style = "-fx-background-color: #2196f3; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;"
                            setOnAction { captureImage() }
                        },
                        Button("Clear Image").apply {
                            style = "-fx-background-color: #f44336; -fx-text-fill: white; -fx-padding: 8 16;"
                            setOnAction { clearImage() }
                        }
                    )
                }
            )
            isVisible = false
            isManaged = false
        }
        
        pane.children.addAll(
            HBox(10.0).apply {
                children.addAll(Label("Type:"), simpleTypeCombo)
            },
            HBox(10.0).apply {
                children.addAll(Label("Identifier:"), identifierTypeCombo)
            },
            coordPane,
            imagePane
        )
        
        pane.userData = mapOf(
            "simpleTypeCombo" to simpleTypeCombo,
            "identifierTypeCombo" to identifierTypeCombo,
            "coordPane" to coordPane,
            "imagePane" to imagePane
        )
        
        return pane
    }
    
    private fun updateIdentifierFields(type: IdentifierType) {
        val userData = simpleConditionPane.userData as? Map<*, *> ?: return
        val coordPane = userData["coordPane"] as? GridPane
        val imagePane = userData["imagePane"] as? VBox
        
        when (type) {
            IdentifierType.BY_COORDINATE -> {
                coordPane?.isVisible = true
                coordPane?.isManaged = true
                imagePane?.isVisible = false
                imagePane?.isManaged = false
            }
            IdentifierType.BY_IMAGE -> {
                coordPane?.isVisible = false
                coordPane?.isManaged = false
                imagePane?.isVisible = true
                imagePane?.isManaged = true
            }
        }
    }
    
    private fun updateContentPane(type: ConditionType) {
        contentPane.children.clear()
        
        when (type) {
            ConditionType.SIMPLE -> {
                contentPane.children.add(simpleConditionPane)
            }
            ConditionType.AND, ConditionType.OR -> {
                val addButton = Button("+ Add Sub-Condition").apply {
                    setOnAction { addSubCondition() }
                }
                
                val label = Label(if (type == ConditionType.AND) "All conditions must be true:" else "At least one condition must be true:")
                label.style = "-fx-font-weight: bold;"
                
                contentPane.children.addAll(label, subConditionsPane, addButton)
                
                if (subConditions.isEmpty()) {
                    addSubCondition()
                    addSubCondition()
                }
            }
            ConditionType.NOT -> {
                val label = Label("Negate the following condition:")
                label.style = "-fx-font-weight: bold;"
                
                contentPane.children.add(label)
                
                if (subConditions.isEmpty()) {
                    addSubCondition()
                }
                
                contentPane.children.add(subConditionsPane)
            }
        }
    }
    
    private fun addSubCondition() {
        val subBuilder = ConditionBuilderPane()
        subConditions.add(subBuilder)
        
        val removeButton = Button("Remove").apply {
            style = "-fx-text-fill: red;"
            setOnAction {
                subConditions.remove(subBuilder)
                subConditionsPane.children.remove(parent)
            }
        }
        
        val container = HBox(10.0).apply {
            children.addAll(subBuilder, removeButton)
            HBox.setHgrow(subBuilder, Priority.ALWAYS)
        }
        
        subConditionsPane.children.add(container)
    }
    
    fun setCondition(condition: Condition) {
        when (condition) {
            is Condition.ElementExists -> {
                conditionTypeCombo.value = ConditionType.SIMPLE
                val userData = simpleConditionPane.userData as? Map<*, *>
                @Suppress("UNCHECKED_CAST")
                (userData?.get("simpleTypeCombo") as? ComboBox<SimpleConditionType>)?.value = SimpleConditionType.ELEMENT_EXISTS
                setIdentifier(condition.identifier)
            }
            is Condition.ElementNotExists -> {
                conditionTypeCombo.value = ConditionType.SIMPLE
                val userData = simpleConditionPane.userData as? Map<*, *>
                @Suppress("UNCHECKED_CAST")
                (userData?.get("simpleTypeCombo") as? ComboBox<SimpleConditionType>)?.value = SimpleConditionType.ELEMENT_NOT_EXISTS
                setIdentifier(condition.identifier)
            }
            is Condition.And -> {
                conditionTypeCombo.value = ConditionType.AND
                subConditions.clear()
                subConditionsPane.children.clear()
                condition.conditions.forEach { subCond ->
                    addSubCondition()
                    subConditions.lastOrNull()?.setCondition(subCond)
                }
            }
            is Condition.Or -> {
                conditionTypeCombo.value = ConditionType.OR
                subConditions.clear()
                subConditionsPane.children.clear()
                condition.conditions.forEach { subCond ->
                    addSubCondition()
                    subConditions.lastOrNull()?.setCondition(subCond)
                }
            }
            is Condition.Not -> {
                conditionTypeCombo.value = ConditionType.NOT
                subConditions.clear()
                subConditionsPane.children.clear()
                addSubCondition()
                subConditions.firstOrNull()?.setCondition(condition.condition)
            }
        }
    }
    
    private fun setIdentifier(identifier: ElementIdentifier) {
        when (identifier) {
            is ElementIdentifier.ByCoordinate -> {
                currentIdentifierType = IdentifierType.BY_COORDINATE
                val userData = simpleConditionPane.userData as? Map<*, *>
                @Suppress("UNCHECKED_CAST")
                (userData?.get("identifierTypeCombo") as? ComboBox<IdentifierType>)?.value = IdentifierType.BY_COORDINATE
                xField.text = identifier.coordinate.x.toString()
                yField.text = identifier.coordinate.y.toString()
            }
            is ElementIdentifier.ByImage -> {
                currentIdentifierType = IdentifierType.BY_IMAGE
                val userData = simpleConditionPane.userData as? Map<*, *>
                @Suppress("UNCHECKED_CAST")
                (userData?.get("identifierTypeCombo") as? ComboBox<IdentifierType>)?.value = IdentifierType.BY_IMAGE
                thresholdField.text = identifier.pattern.matchThreshold.toString()
                capturedImageBase64 = identifier.pattern.base64Data
                loadImageFromBase64(identifier.pattern.base64Data)
            }
        }
    }
    
    fun getCondition(): Condition? {
        return when (conditionTypeCombo.value) {
            ConditionType.SIMPLE -> {
                val identifier = when (currentIdentifierType) {
                    IdentifierType.BY_COORDINATE -> {
                        val x = xField.text.toIntOrNull() ?: return null
                        val y = yField.text.toIntOrNull() ?: return null
                        ElementIdentifier.ByCoordinate(Coordinate(x, y))
                    }
                    IdentifierType.BY_IMAGE -> {
                        val threshold = thresholdField.text.toDoubleOrNull() ?: 0.7
                        if (capturedImageBase64.isBlank()) return null
                        ElementIdentifier.ByImage(ImagePattern(capturedImageBase64, threshold))
                    }
                }
                
                when (currentSimpleConditionType) {
                    SimpleConditionType.ELEMENT_EXISTS -> Condition.ElementExists(identifier)
                    SimpleConditionType.ELEMENT_NOT_EXISTS -> Condition.ElementNotExists(identifier)
                }
            }
            ConditionType.AND -> {
                val conditions = subConditions.mapNotNull { it.getCondition() }
                if (conditions.size < 2) return null
                Condition.And(conditions)
            }
            ConditionType.OR -> {
                val conditions = subConditions.mapNotNull { it.getCondition() }
                if (conditions.size < 2) return null
                Condition.Or(conditions)
            }
            ConditionType.NOT -> {
                val condition = subConditions.firstOrNull()?.getCondition() ?: return null
                Condition.Not(condition)
            }
            null -> null
        }
    }
    
    enum class ConditionType {
        SIMPLE,
        AND,
        OR,
        NOT
    }
    
    enum class SimpleConditionType {
        ELEMENT_EXISTS,
        ELEMENT_NOT_EXISTS
    }
    
    enum class IdentifierType {
        BY_COORDINATE,
        BY_IMAGE
    }
    
    private fun captureImage() {
        try {
            logger.info("Opening screen capture dialog")
            val dialog = ScreenCaptureDialog()
            val result = dialog.showAndWait()
            
            if (result.isPresent) {
                val captureResult = result.get()
                logger.info("Image captured, Base64 length: ${captureResult.base64Data.length}")
                capturedImageBase64 = captureResult.base64Data
                
                val fxImage = javafx.embed.swing.SwingFXUtils.toFXImage(captureResult.image, null)
                imagePreview.image = fxImage
                imageInfoLabel.text = "Image: ${captureResult.image.width} x ${captureResult.image.height} px"
                imageInfoLabel.style = "-fx-text-fill: green; -fx-font-style: normal; -fx-font-size: 11px;"
                
                updateConditionProperty()
            } else {
                logger.info("Screen capture cancelled")
            }
        } catch (e: Exception) {
            logger.error("Error capturing image", e)
            val alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Capture Error"
            alert.headerText = "Failed to capture image"
            alert.contentText = e.message
            alert.showAndWait()
        }
    }
    
    private fun clearImage() {
        capturedImageBase64 = ""
        imagePreview.image = null
        imageInfoLabel.text = "No image captured"
        imageInfoLabel.style = "-fx-text-fill: gray; -fx-font-style: italic; -fx-font-size: 11px;"
        updateConditionProperty()
    }
    
    private fun loadImageFromBase64(base64Data: String) {
        try {
            val imageBytes = java.util.Base64.getDecoder().decode(base64Data)
            val inputStream = java.io.ByteArrayInputStream(imageBytes)
            val bufferedImage = javax.imageio.ImageIO.read(inputStream)
            
            val fxImage = javafx.embed.swing.SwingFXUtils.toFXImage(bufferedImage, null)
            imagePreview.image = fxImage
            imageInfoLabel.text = "Image: ${bufferedImage.width} x ${bufferedImage.height} px"
            imageInfoLabel.style = "-fx-text-fill: green; -fx-font-style: normal; -fx-font-size: 11px;"
        } catch (e: Exception) {
            logger.error("Error loading image from Base64", e)
            imageInfoLabel.text = "Error loading image"
            imageInfoLabel.style = "-fx-text-fill: red; -fx-font-style: italic; -fx-font-size: 11px;"
        }
    }
}

