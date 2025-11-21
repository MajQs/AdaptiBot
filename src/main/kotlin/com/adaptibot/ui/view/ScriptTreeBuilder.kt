package com.adaptibot.ui.view

import com.adaptibot.common.model.Step
import com.adaptibot.ui.model.StepNode
import javafx.scene.control.TreeItem

object ScriptTreeBuilder {
    
    fun buildTree(steps: List<Step>): TreeItem<StepNode> {
        val root = TreeItem(StepNode(
            step = Step.GroupBlock(
                id = com.adaptibot.common.model.StepId("root"),
                name = "Script Root",
                steps = steps
            ),
            displayText = "Script Steps",
            icon = "📜"
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
            is Step.ConditionalBlock -> {
                if (step.thenSteps.isNotEmpty()) {
                    val thenBranch = TreeItem(StepNode(
                        step = Step.GroupBlock(
                            id = com.adaptibot.common.model.StepId("then_${step.id.value}"),
                            name = "THEN",
                            steps = step.thenSteps
                        ),
                        displayText = "THEN",
                        icon = "✓"
                    ))
                    thenBranch.isExpanded = true
                    step.thenSteps.forEach { childStep ->
                        thenBranch.children.add(buildTreeItem(childStep))
                    }
                    treeItem.children.add(thenBranch)
                }
                
                if (step.elseSteps.isNotEmpty()) {
                    val elseBranch = TreeItem(StepNode(
                        step = Step.GroupBlock(
                            id = com.adaptibot.common.model.StepId("else_${step.id.value}"),
                            name = "ELSE",
                            steps = step.elseSteps
                        ),
                        displayText = "ELSE",
                        icon = "✗"
                    ))
                    elseBranch.isExpanded = true
                    step.elseSteps.forEach { childStep ->
                        elseBranch.children.add(buildTreeItem(childStep))
                    }
                    treeItem.children.add(elseBranch)
                }
            }
            is Step.ObserverBlock -> {
                step.actionSteps.forEach { childStep ->
                    treeItem.children.add(buildTreeItem(childStep))
                }
            }
            is Step.GroupBlock -> {
                step.steps.forEach { childStep ->
                    treeItem.children.add(buildTreeItem(childStep))
                }
            }
            is Step.ActionStep -> {
                // Leaf node - no children
            }
        }
        
        return treeItem
    }
    
    fun findTreeItem(root: TreeItem<StepNode>, stepId: com.adaptibot.common.model.StepId): TreeItem<StepNode>? {
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

