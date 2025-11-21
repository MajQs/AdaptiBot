package com.adaptibot.ui.view

import javafx.scene.control.Label
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

class ScriptEditorPane : BorderPane() {
    
    val stepsTreeView: TreeView<String>
    
    init {
        val header = Label("Script Editor")
        header.style = "-fx-font-weight: bold; -fx-padding: 10;"
        
        val rootItem = TreeItem("Script Steps")
        rootItem.isExpanded = true
        
        stepsTreeView = TreeView<String>(rootItem)
        
        top = header
        center = stepsTreeView
        
        VBox.setVgrow(this, Priority.ALWAYS)
    }
}

