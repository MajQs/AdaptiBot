package com.adaptibot.ui.view

import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region

/**
 * Builds the cell graphic for a [TreeNode.BranchNode] (IF / ELSE header row).
 */
object BranchNodeGraphic {

    fun build(node: TreeNode.BranchNode): HBox {
        val isTrueBranch = node.branch == ConditionalBranch.TRUE

        val branchLabel = Label(if (isTrueBranch) "▸ IF TRUE" else "▸ IF ELSE").apply {
            styleClass.addAll("branch-node-label", if (isTrueBranch) "branch-node-true" else "branch-node-else")
        }

        val countBadge = Label("${node.stepCount}").apply {
            styleClass.addAll("branch-node-count")
        }

        val spacer = Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }

        return HBox(6.0, branchLabel, spacer, countBadge).apply {
            alignment = Pos.CENTER_LEFT
            styleClass.add("branch-node-row")
        }
    }
}

