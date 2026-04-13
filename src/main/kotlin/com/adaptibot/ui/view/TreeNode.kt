package com.adaptibot.ui.view

import com.adaptibot.script.step.Branch
import com.adaptibot.script.step.Step
import com.adaptibot.script.step.StepId

/**
 * Unified node type for the script tree view.
 *
 * [StepNode]   – wraps any [Step] that is directly placed in the tree.
 * [BranchNode] – represents a [Branch] (TRUE or ELSE) owned by a [ConditionalStep].
 *                Carries the full [Branch] object so the UI can access branch.id
 *                and branch.steps without a secondary lookup.
 */
sealed class TreeNode {
    data class StepNode(val step: Step) : TreeNode()

    data class BranchNode(
        val branch: Branch,
        val parentStepId: StepId,
        val isTrueBranch: Boolean
    ) : TreeNode()
}
