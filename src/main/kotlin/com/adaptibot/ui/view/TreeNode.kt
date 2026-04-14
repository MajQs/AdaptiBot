package com.adaptibot.ui.view

import com.adaptibot.script.step.Step
import com.adaptibot.script.step.StepContainer
import com.adaptibot.script.step.StepId

/**
 * Unified node type for the script tree view.
 *
 * [StepNode]      – wraps any [Step] that is directly placed in the tree.
 * [ContainerNode] – represents a [StepContainer] owned by a parent step (e.g. the
 *                   TRUE / ELSE branches of a [ConditionalStep]).
 *                   Carries the full [StepContainer] so the UI can access its id
 *                   and steps without a secondary lookup.
 */
sealed class TreeNode {
    data class StepNode(val step: Step, val parentDisabled: Boolean = false) : TreeNode()

    data class ContainerNode(
        val container: StepContainer,
        val parentStepId: StepId,
        val label: String,            // e.g. "IF TRUE", "IF ELSE"
        val parentDisabled: Boolean = false
    ) : TreeNode()
}
