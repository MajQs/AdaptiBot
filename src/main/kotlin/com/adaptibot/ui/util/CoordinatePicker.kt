package com.adaptibot.ui.util

import javafx.application.Platform
import javafx.scene.Cursor
import javafx.scene.Scene
import javafx.scene.input.KeyCode
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle

/**
 * Full-screen transparent overlay. On click, returns the screen coordinates.
 * Pressing ESC cancels.
 */
object CoordinatePicker {

    fun pick(onPicked: (x: Int, y: Int) -> Unit, onCancel: () -> Unit) {
        val screen = Screen.getPrimary().bounds
        val stage = Stage(StageStyle.TRANSPARENT)
        stage.isAlwaysOnTop = true

        val crosshair = Text("+").apply {
            fill = Color.color(0.537, 0.706, 0.98, 1.0)
            style = "-fx-font-size: 24px; -fx-font-weight: bold;"
        }

        val hint = Text("Click to capture coordinates  •  ESC to cancel").apply {
            fill = Color.WHITE
            style = "-fx-font-size: 13px;"
            x = screen.width / 2 - 200
            y = 40.0
        }

        val coordsText = Text("").apply {
            fill = Color.color(0.537, 0.706, 0.98, 0.9)
            style = "-fx-font-size: 12px;"
        }

        val overlay = Pane().apply {
            style = "-fx-background-color: rgba(0,0,0,0.01);"
        }
        overlay.children.addAll(hint, coordsText, crosshair)
        overlay.cursor = Cursor.CROSSHAIR

        val scene = Scene(overlay, screen.width, screen.height, Color.color(0.0, 0.0, 0.0, 0.3))
        stage.scene = scene

        val scaleX = Screen.getPrimary().outputScaleX
        val scaleY = Screen.getPrimary().outputScaleY

        scene.setOnMouseMoved { e ->
            val physicalX = (e.screenX * scaleX).toInt()
            val physicalY = (e.screenY * scaleY).toInt()
            crosshair.x = e.sceneX - 8
            crosshair.y = e.sceneY + 8
            coordsText.text = "x=$physicalX  y=$physicalY"
            coordsText.x = e.sceneX + 16
            coordsText.y = e.sceneY - 6
        }

        scene.setOnMouseClicked { e ->
            val physicalX = (e.screenX * scaleX).toInt()
            val physicalY = (e.screenY * scaleY).toInt()
            stage.hide()
            Platform.runLater { onPicked(physicalX, physicalY) }
        }

        scene.setOnKeyPressed { e ->
            if (e.code == KeyCode.ESCAPE) {
                stage.hide()
                onCancel()
            }
        }

        stage.x = screen.minX; stage.y = screen.minY
        stage.show()
        stage.requestFocus()
    }
}

