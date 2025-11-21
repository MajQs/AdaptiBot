package com.adaptibot.common.model

import kotlinx.serialization.Serializable

/**
 * Represents a single step in a script execution flow.
 * Uses Composite Pattern for unlimited nesting of blocks.
 */
@Serializable
sealed class Step {
    abstract val id: StepId
    abstract val label: String?
    abstract val delayBefore: Long
    abstract val delayAfter: Long
    
    @Serializable
    data class ActionStep(
        override val id: StepId,
        override val label: String? = null,
        override val delayBefore: Long = 0,
        override val delayAfter: Long = 0,
        val action: Action
    ) : Step()
    
    @Serializable
    data class ConditionalBlock(
        override val id: StepId,
        override val label: String? = null,
        override val delayBefore: Long = 0,
        override val delayAfter: Long = 0,
        val condition: Condition,
        val thenSteps: List<Step>,
        val elseSteps: List<Step> = emptyList()
    ) : Step()
    
    @Serializable
    data class ObserverBlock(
        override val id: StepId,
        override val label: String? = null,
        override val delayBefore: Long = 0,
        override val delayAfter: Long = 0,
        val condition: Condition,
        val actionSteps: List<Step>
    ) : Step()
    
    @Serializable
    data class GroupBlock(
        override val id: StepId,
        override val label: String? = null,
        override val delayBefore: Long = 0,
        override val delayAfter: Long = 0,
        val name: String,
        val steps: List<Step>
    ) : Step()
}

