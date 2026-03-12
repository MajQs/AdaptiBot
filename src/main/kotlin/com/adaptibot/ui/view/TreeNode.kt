package com.adaptibot.ui.view

import com.adaptibot.model.ConditionalBlock
import com.adaptibot.model.Step
import com.adaptibot.model.StepId

/**
 * Unified node type for the script tree view.
 *
 * [StepNode]   – wraps a real [Step] (all step types).
 * [BranchNode] – synthetic header node representing the TRUE or ELSE branch
 *                of a [ConditionalBlock]. Never persisted; rebuilt from model.
 */
sealed class TreeNode {

    data class StepNode(val step: Step) : TreeNode()

    data class BranchNode(
        /** Id of the owning [ConditionalBlock]. */
        val parentId: StepId,
        val branch: ConditionalBranch,
        /** Live step count shown in the header badge. */
        val stepCount: Int
    ) : TreeNode()
}

