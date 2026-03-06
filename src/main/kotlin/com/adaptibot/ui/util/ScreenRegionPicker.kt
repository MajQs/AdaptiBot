package com.adaptibot.ui.util

import com.adaptibot.infrastructure.ScreenCapture
import com.adaptibot.serialization.ImageEncoder
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Rectangle2D
import javafx.scene.Cursor
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.input.KeyCode
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import java.awt.image.BufferedImage

/**
 * Full-screen transparent overlay that lets the user drag-select a region.
 * On release, captures that region and returns base64-encoded PNG via [onCapture].
 * Pressing ESC cancels.
 */
object ScreenRegionPicker {

    fun pick(onCapture: (base64: String) -> Unit, onCancel: () -> Unit) {
        val screen = Screen.getPrimary().bounds
        val stage = Stage(StageStyle.TRANSPARENT)
        stage.isAlwaysOnTop = true

        val canvas = Canvas(screen.width, screen.height)
        val gc = canvas.graphicsContext2D
        gc.fill = Color.color(0.0, 0.0, 0.0, 0.35)
        gc.fillRect(0.0, 0.0, screen.width, screen.height)

        val selRect = Rectangle().apply {
            fill = Color.color(0.537, 0.706, 0.98, 0.15)
            stroke = Color.color(0.537, 0.706, 0.98, 1.0)
            strokeWidth = 2.0
        }

        val hintText = javafx.scene.text.Text("Drag to select region  •  ESC to cancel").apply {
            fill = Color.WHITE
            style = "-fx-font-size: 14px;"
            x = screen.width / 2 - 180
            y = 40.0
        }

        val pane = Pane(canvas, selRect, hintText)
        pane.cursor = Cursor.CROSSHAIR
        pane.style = "-fx-background-color: transparent;"

        val scene = Scene(pane, screen.width, screen.height, Color.TRANSPARENT)
        stage.scene = scene

        var startX = 0.0; var startY = 0.0

        scene.setOnMousePressed { e ->
            startX = e.screenX; startY = e.screenY
            selRect.x = startX; selRect.y = startY
            selRect.width = 0.0; selRect.height = 0.0
        }
        scene.setOnMouseDragged { e ->
            val x = minOf(startX, e.screenX)
            val y = minOf(startY, e.screenY)
            val w = Math.abs(e.screenX - startX)
            val h = Math.abs(e.screenY - startY)
            selRect.x = x; selRect.y = y
            selRect.width = w; selRect.height = h
        }
        scene.setOnMouseReleased { e ->
            val x = minOf(startX, e.screenX).toInt()
            val y = minOf(startY, e.screenY).toInt()
            val w = Math.abs(e.screenX - startX).toInt()
            val h = Math.abs(e.screenY - startY).toInt()
            stage.hide()
            if (w > 4 && h > 4) {
                Thread {
                    try {
                        val img: BufferedImage = ScreenCapture.captureRegion(x, y, w, h)
                        val b64 = ImageEncoder.encodeToBase64(img)
                        Platform.runLater { onCapture(b64) }
                    } catch (ex: Exception) {
                        Platform.runLater { onCancel() }
                    }
                }.also { it.isDaemon = true; it.start() }
            } else {
                onCancel()
            }
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

