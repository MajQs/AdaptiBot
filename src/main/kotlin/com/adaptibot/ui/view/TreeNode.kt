package com.adaptibot.ui.view

import com.adaptibot.script.step.Step

/**
 * Unified node type for the script tree view.
 * Every tree item wraps a real [Step]
 */
sealed class TreeNode {
    data class StepNode(val step: Step) : TreeNode()
}
