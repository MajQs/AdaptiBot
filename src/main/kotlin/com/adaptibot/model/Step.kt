package com.adaptibot.model

import kotlinx.serialization.Serializable

@Serializable
sealed class Step {
    abstract val id: StepId
    abstract val label: String?
    abstract val delayBefore: Long
}

@Serializable
data class ActionStep(
    override val id: StepId,
    override val label: String? = null,
    override val delayBefore: Long = 1000,
    val action: Action
) : Step()

@Serializable
sealed class BlockStep : Step() {
    abstract val steps: List<Step>
}

@Serializable
data class ConditionalBlock(
    override val id: StepId,
    override val label: String? = null,
    override val delayBefore: Long = 0,
    val condition: Condition,
    override val steps: List<Step>,
    val elseSteps: List<Step> = emptyList()
) : BlockStep() {
}

@Serializable
data class GroupBlock(
    override val id: StepId,
    override val label: String? = null,
    override val delayBefore: Long = 0,
    override val steps: List<Step>
) : BlockStep()

@Serializable
data class ObserverStep(
    override val id: StepId,
    override val label: String? = null,
    override val delayBefore: Long = 0,
    val condition: Condition,
    val steps: List<Step>
) : Step()
