package com.adaptibot.ui.util

import com.adaptibot.model.PixelColor
import javafx.application.Platform
import javafx.scene.Cursor
import javafx.scene.Scene
import javafx.scene.input.KeyCode
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.text.Text
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import java.awt.Robot

/**
 * Full-screen transparent overlay that lets the user click a pixel on the screen.
 * On click, returns the screen coordinates **and** the pixel colour at that point.
 * A live colour-swatch preview follows the cursor so the user can see the colour
 * under the cursor before committing.
 *
 * No custom crosshair is drawn – the OS CROSSHAIR cursor is used instead so it
 * never interferes with Robot pixel sampling.
 *
 * Pressing ESC cancels without producing a result.
 */
object ColorPickerOverlay {

    private val robot = Robot()

    /**
     * @param onPicked  Called on the JavaFX thread with the clicked (x, y) and the
     *                  [PixelColor] sampled at that position.
     * @param onCancel  Called on the JavaFX thread when the user presses ESC.
     */
    fun pick(onPicked: (x: Int, y: Int, color: PixelColor) -> Unit, onCancel: () -> Unit) {
        val screen = Screen.getPrimary().bounds
        val stage  = Stage(StageStyle.TRANSPARENT)
        stage.isAlwaysOnTop = true

        // ── Hint ───────────────────────────────────────────────────────────
        val hint = Text("Click to pick colour & coordinates  •  ESC to cancel").apply {
            fill  = Color.WHITE
            style = "-fx-font-size: 13px;"
            x     = screen.width / 2 - 230
            y     = 40.0
        }

        // ── Live info label (coords + hex) ─────────────────────────────────
        val infoText = Text("").apply {
            fill  = Color.color(1.0, 1.0, 1.0, 0.95)
            style = "-fx-font-size: 11px;"
        }

        // ── Colour swatch that follows the cursor ──────────────────────────
        val swatch = Rectangle(28.0, 28.0).apply {
            arcWidth    = 4.0
            arcHeight   = 4.0
            stroke      = Color.WHITE
            strokeWidth = 1.5
            fill        = Color.BLACK
        }

        val overlay = Pane().apply {
            style = "-fx-background-color: rgba(0,0,0,0.01);"
            children.addAll(hint, swatch, infoText)
        }
        overlay.cursor = Cursor.CROSSHAIR

        val scene = Scene(overlay, screen.width, screen.height, Color.color(0.0, 0.0, 0.0, 0.28))
        stage.scene = scene

        // Throttle: only sample AWT Robot colour every ~50 ms
        var lastSampleMs = 0L

        scene.setOnMouseMoved { e ->
            val sx = e.screenX.toInt()
            val sy = e.screenY.toInt()

            // Throttled pixel colour sample.
            // Sample 1 px below-right so the OS crosshair hotpoint (top-left corner)
            // doesn't land exactly on the sampled pixel – avoids cursor-colour bleed.
            val now = System.currentTimeMillis()
            if (now - lastSampleMs > 50) {
                lastSampleMs = now
                val awtColor = robot.getPixelColor(sx + 1, sy + 1)
                val fxColor  = Color.rgb(awtColor.red, awtColor.green, awtColor.blue)
                swatch.fill  = fxColor
                val hex = "#%02X%02X%02X".format(awtColor.red, awtColor.green, awtColor.blue)
                infoText.text = "x=$sx  y=$sy   $hex  R:${awtColor.red} G:${awtColor.green} B:${awtColor.blue}"
            }

            // Swatch & info follow cursor with a small offset to the right
            swatch.x   = e.sceneX + 18
            swatch.y   = e.sceneY - 14
            infoText.x = e.sceneX + 52
            infoText.y = e.sceneY + 2
        }

        scene.setOnMouseClicked { e ->
            val sx = e.screenX.toInt()
            val sy = e.screenY.toInt()
            // Hide the overlay FIRST so the robot reads the real screen pixel.
            stage.hide()
            Thread {
                Thread.sleep(80) // give the OS time to repaint the area
                val awtColor = robot.getPixelColor(sx, sy)
                val pixelColor = PixelColor(
                    r = awtColor.red,
                    g = awtColor.green,
                    b = awtColor.blue,
                    a = 255
                )
                Platform.runLater { onPicked(sx, sy, pixelColor) }
            }.also { it.isDaemon = true; it.start() }
        }

        scene.setOnKeyPressed { e ->
            if (e.code == KeyCode.ESCAPE) {
                stage.hide()
                Platform.runLater { onCancel() }
            }
        }

        stage.x = screen.minX
        stage.y = screen.minY
        stage.show()
        stage.requestFocus()
    }
}
