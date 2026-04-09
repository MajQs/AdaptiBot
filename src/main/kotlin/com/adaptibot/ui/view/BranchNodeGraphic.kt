package com.adaptibot.ui.view

import com.adaptibot.script.step.ConditionalBranch
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.PauseTransition
import javafx.animation.Timeline
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.util.Duration

/**
 * Builds the cell graphic for a [TreeNode.BranchNode] (IF TRUE / IF ELSE header row).
 * On hover an animated "＋" insert strip slides in, identical in behaviour to [StepCellGraphic].
 *
 * @param onAddInside called when the insert strip is pressed
 */
object BranchNodeGraphic {

    private const val STRIP_HEIGHT = 18.0
    private const val ANIM_MS      = 150.0

    fun build(
        node: TreeNode.BranchNode,
        onAddInside: ((anchorX: Double, anchorY: Double) -> Unit)? = null
    ): VBox {
        val isTrueBranch = node.branch == ConditionalBranch.TRUE

        val branchLabel = Label(if (isTrueBranch) "▸ IF TRUE" else "▸ IF ELSE").apply {
            styleClass.addAll("branch-node-label", if (isTrueBranch) "branch-node-true" else "branch-node-else")
        }

        val countBadge = Label("${node.stepCount}").apply {
            styleClass.add("branch-node-count")
        }

        val spacer = Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }

        val headerRow = HBox(6.0, branchLabel, spacer, countBadge).apply {
            alignment = Pos.CENTER_LEFT
            styleClass.add("branch-node-row")
        }

        // ── insert strip ──────────────────────────────────────────────────
        val insertStrip = buildInsertStrip()
        insertStrip.setOnMousePressed { e ->
            val bounds = insertStrip.localToScreen(insertStrip.boundsInLocal)
            if (bounds != null) onAddInside?.invoke(bounds.minX, bounds.minY + bounds.height / 2)
            e.consume()
        }

        insertStrip.prefHeight = 0.0
        insertStrip.opacity    = 0.0
        insertStrip.isManaged  = false
        insertStrip.isVisible  = false

        // ── assemble ──────────────────────────────────────────────────────
        return VBox(0.0, headerRow, insertStrip).apply {
            val showDelay = PauseTransition(Duration.millis(350.0)).apply {
                setOnFinished { showStrip(insertStrip) }
            }
            setOnMouseEntered { showDelay.playFromStart() }
            setOnMouseExited  {
                showDelay.stop()
                hideStrip(insertStrip)
            }
        }
    }

    private fun buildInsertStrip(): StackPane {
        val makeLine = {
            Region().apply {
                styleClass.add("insert-strip-line")
                maxWidth = Double.MAX_VALUE
                HBox.setHgrow(this, Priority.ALWAYS)
            }
        }
        val plusLabel = Label("＋").apply { styleClass.add("insert-strip-plus") }
        val inner = HBox(6.0).apply {
            alignment = Pos.CENTER
            children.addAll(makeLine(), plusLabel, makeLine())
        }
        return StackPane(inner).apply {
            styleClass.add("insert-strip")
            minHeight = 0.0
            maxHeight = STRIP_HEIGHT
            setOnMouseEntered { styleClass.add("insert-strip-hovered") }
            setOnMouseExited  { styleClass.remove("insert-strip-hovered") }
        }
    }

    private fun showStrip(strip: StackPane) {
        strip.isManaged = true
        strip.isVisible = true
        Timeline(
            KeyFrame(Duration.millis(ANIM_MS),
                KeyValue(strip.prefHeightProperty(), STRIP_HEIGHT),
                KeyValue(strip.opacityProperty(),    1.0)
            )
        ).play()
    }

    private fun hideStrip(strip: StackPane) {
        Timeline(
            KeyFrame(Duration.millis(ANIM_MS),
                KeyValue(strip.prefHeightProperty(), 0.0),
                KeyValue(strip.opacityProperty(),    0.0)
            )
        ).apply {
            setOnFinished {
                strip.isManaged = false
                strip.isVisible = false
            }
            play()
        }
    }
}
