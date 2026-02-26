package com.adaptibot.ui.view

import com.adaptibot.model.ActionStep
import com.adaptibot.model.ConditionalBlock
import com.adaptibot.model.GroupBlock
import com.adaptibot.model.ObserverStep
import com.adaptibot.model.Step
import com.adaptibot.model.StepId
import com.adaptibot.ui.model.StepNode
import javafx.scene.control.TreeItem

object ScriptTreeBuilder {

    fun buildTree(steps: List<Step>): TreeItem<StepNode> {
        val rootStep = GroupBlock(
            id = StepId("root"),
            name = "Script Root",
            steps = steps
        )
        val root = TreeItem(StepNode(
            step = rootStep,
            displayText = "Script Steps",
            icon = "📜",
            containerType = com.adaptibot.ui.model.ContainerType.ROOT
        ))
        root.isExpanded = true

        steps.forEach { step ->
            root.children.add(buildTreeItem(step))
        }

        return root
    }

    private fun buildTreeItem(step: Step): TreeItem<StepNode> {
        val node = StepNode.from(step)
        val treeItem = TreeItem(node)
        treeItem.isExpanded = node.isExpanded

        when (step) {
            is ConditionalBlock -> {
                val thenBranchStep = GroupBlock(
                    id = StepId("then_${step.id.value}"),
                    name = "THEN",
                    steps = step.thenSteps
                )
                val thenBranch = TreeItem(StepNode(
                    step = thenBranchStep,
                    displayText = "THEN",
                    icon = "✓",
                    containerType = com.adaptibot.ui.model.ContainerType.CONDITIONAL_THEN,
                    parentBlockId = step.id
                ))
                thenBranch.isExpanded = true
                step.thenSteps.forEach { childStep ->
                    thenBranch.children.add(buildTreeItem(childStep))
                }
                treeItem.children.add(thenBranch)

                val elseBranchStep = GroupBlock(
                    id = StepId("else_${step.id.value}"),
                    name = "ELSE",
                    steps = step.elseSteps
                )
                val elseBranch = TreeItem(StepNode(
                    step = elseBranchStep,
                    displayText = "ELSE",
                    icon = "✗",
                    containerType = com.adaptibot.ui.model.ContainerType.CONDITIONAL_ELSE,
                    parentBlockId = step.id
                ))
                elseBranch.isExpanded = true
                step.elseSteps.forEach { childStep ->
                    elseBranch.children.add(buildTreeItem(childStep))
                }
                treeItem.children.add(elseBranch)
            }
            is ObserverStep -> {
                step.actionSteps.forEach { childStep ->
                    treeItem.children.add(buildTreeItem(childStep))
                }
            }
            is GroupBlock -> {
                step.steps.forEach { childStep ->
                    treeItem.children.add(buildTreeItem(childStep))
                }
            }
            is ActionStep -> {
                // Leaf node - no children
            }
        }

        return treeItem
    }

    fun findTreeItem(root: TreeItem<StepNode>, stepId: StepId): TreeItem<StepNode>? {
        if (root.value.step.id == stepId) {
            return root
        }

        for (child in root.children) {
            val found = findTreeItem(child, stepId)
            if (found != null) {
                return found
            }
        }

        return null
    }
}

