package com.adaptibot.ui.view

import com.adaptibot.model.StepId
import com.adaptibot.model.Step
import com.adaptibot.ui.viewmodel.ScriptViewModel
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*

class ScriptPanel(
    private val viewModel: ScriptViewModel,
    private val onEditStep: (Step) -> Unit,
    private val onAddStep: (parentId: StepId?) -> Unit
) : BorderPane() {

    val treeView = StepTreeView(viewModel)

    init {
        styleClass.add("script-panel")

        val header = buildHeader()
        top = header

        treeView.setOnEditStep(onEditStep)
        treeView.setOnAddStep(onAddStep)

        val scroll = ScrollPane(treeView).apply {
            isFitToWidth = true
            isFitToHeight = true
            styleClass.add("scroll-pane")
        }
        center = scroll
    }

    private fun buildHeader(): HBox {
        val title = Label("SCRIPT STEPS").apply { styleClass.add("panel-title") }

        val addBtn = Button("＋  Add Step").apply {
            styleClass.add("toolbar-btn")
            style = "-fx-padding: 2 10 2 10; -fx-font-size: 11px;"
            setOnAction { onAddStep(null) }
        }

        val spacer = Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }
        return HBox(8.0, title, spacer, addBtn).apply {
            styleClass.add("panel-header")
            alignment = Pos.CENTER_LEFT
        }
    }
}

