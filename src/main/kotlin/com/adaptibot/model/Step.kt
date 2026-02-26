package com.adaptibot.model

import kotlinx.serialization.Serializable

@Serializable
sealed class Step {
    abstract val id: StepId
    abstract val label: String?
    abstract val delayBefore: Long
    abstract val delayAfter: Long
}

@Serializable
data class ActionStep(
    override val id: StepId,
    override val label: String? = null,
    override val delayBefore: Long = 0,
    override val delayAfter: Long = 0,
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
    override val delayAfter: Long = 0,
    val condition: Condition,
    val thenSteps: List<Step>,
    val elseSteps: List<Step> = emptyList()
) : BlockStep() {
    override val steps: List<Step> get() = thenSteps + elseSteps
}

@Serializable
data class ObserverStep(
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
    override val steps: List<Step>
) : BlockStep()
