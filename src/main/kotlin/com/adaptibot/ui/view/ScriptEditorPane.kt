package com.adaptibot.ui.view

import javafx.scene.control.Label
import javafx.scene.control.TreeCell
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.util.Callback

class ScriptEditorPane : BorderPane() {
    
    val stepsTreeView: TreeView<com.adaptibot.ui.model.StepNode>
    
    init {
        val header = Label("Script Editor")
        header.style = "-fx-font-weight: bold; -fx-padding: 10;"
        
        stepsTreeView = TreeView()
        stepsTreeView.isShowRoot = true
        
        // Custom cell factory for rendering StepNode
        stepsTreeView.cellFactory = Callback {
            object : TreeCell<com.adaptibot.ui.model.StepNode>() {
                override fun updateItem(item: com.adaptibot.ui.model.StepNode?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || item == null) {
                        null
                    } else {
                        "${item.icon} ${item.displayText}"
                    }
                }
            }
        }
        
        top = header
        center = stepsTreeView
        
        VBox.setVgrow(this, Priority.ALWAYS)
    }
}

