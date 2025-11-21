package com.adaptibot.ui.view

import javafx.geometry.Orientation
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

class MainView : BorderPane() {
    
    val menuBar: MenuBar
    val scriptEditorPane: ScriptEditorPane
    val logsPane: LogsPane
    val controlToolBar: ToolBar
    
    init {
        menuBar = createMenuBar()
        controlToolBar = createControlToolBar()
        scriptEditorPane = ScriptEditorPane()
        logsPane = LogsPane()
        
        top = VBox(menuBar, controlToolBar)
        center = createContentArea()
    }
    
    private fun createMenuBar(): MenuBar {
        return MenuBar().apply {
            menus.addAll(
                createFileMenu(),
                createEditMenu(),
                createRunMenu(),
                createHelpMenu()
            )
        }
    }
    
    private fun createFileMenu(): Menu {
        return Menu("File").apply {
            items.addAll(
                MenuItem("New Script"),
                MenuItem("Open Script..."),
                MenuItem("Save Script"),
                MenuItem("Save Script As..."),
                SeparatorMenuItem(),
                MenuItem("Exit")
            )
        }
    }
    
    private fun createEditMenu(): Menu {
        return Menu("Edit").apply {
            items.addAll(
                MenuItem("Add Step"),
                MenuItem("Delete Step"),
                MenuItem("Copy Step"),
                MenuItem("Paste Step"),
                SeparatorMenuItem(),
                MenuItem("Settings")
            )
        }
    }
    
    private fun createRunMenu(): Menu {
        return Menu("Run").apply {
            items.addAll(
                MenuItem("Start"),
                MenuItem("Pause"),
                MenuItem("Stop"),
                SeparatorMenuItem(),
                CheckMenuItem("Debug Mode")
            )
        }
    }
    
    private fun createHelpMenu(): Menu {
        return Menu("Help").apply {
            items.addAll(
                MenuItem("Documentation"),
                MenuItem("Examples"),
                SeparatorMenuItem(),
                MenuItem("About")
            )
        }
    }
    
    private fun createControlToolBar(): ToolBar {
        return ToolBar().apply {
            items.addAll(
                Button("Start"),
                Button("Pause"),
                Button("Stop"),
                Separator(Orientation.VERTICAL),
                Label("Status: Idle")
            )
        }
    }
    
    private fun createContentArea(): SplitPane {
        return SplitPane().apply {
            orientation = Orientation.VERTICAL
            items.addAll(scriptEditorPane, logsPane)
            setDividerPositions(0.6)
            VBox.setVgrow(this, Priority.ALWAYS)
        }
    }
}

