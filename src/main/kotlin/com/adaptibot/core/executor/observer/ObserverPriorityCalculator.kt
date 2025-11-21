package com.adaptibot.core.executor.observer

import com.adaptibot.common.model.Step

object ObserverPriorityCalculator {
    
    /**
     * Calculates observer priority based on nesting depth.
     * Higher depth = higher priority (deeper nested observers have precedence)
     */
    fun calculatePriority(observer: Step.ObserverBlock, depth: Int, positionInLevel: Int): Int {
        // Priority formula: (depth * 1000) - positionInLevel
        // This ensures deeper observers have higher priority
        // Within same depth, earlier observers have higher priority
        return (depth * 1000) - positionInLevel
    }
    
    /**
     * Calculates depth of a step in the script tree
     */
    fun calculateDepth(steps: List<Step>, targetObserver: Step.ObserverBlock): Int {
        var currentDepth = 0
        
        fun findDepth(stepsList: List<Step>, depth: Int): Int? {
            stepsList.forEachIndexed { _, step ->
                if (step == targetObserver) {
                    return depth
                }
                
                val childDepth = when (step) {
                    is Step.ConditionalBlock -> {
                        findDepth(step.thenSteps, depth + 1) ?: findDepth(step.elseSteps, depth + 1)
                    }
                    is Step.ObserverBlock -> {
                        findDepth(step.actionSteps, depth + 1)
                    }
                    is Step.GroupBlock -> {
                        findDepth(step.steps, depth + 1)
                    }
                    is Step.ActionStep -> null
                }
                
                if (childDepth != null) {
                    return childDepth
                }
            }
            return null
        }
        
        return findDepth(steps, 0) ?: 0
    }
}

