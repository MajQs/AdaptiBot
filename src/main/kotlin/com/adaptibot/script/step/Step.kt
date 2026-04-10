package com.adaptibot.script.step

import com.adaptibot.script.value.Action
import com.adaptibot.script.value.Condition
import kotlinx.serialization.Serializable

@Serializable
sealed class Step {
    val id: StepId = StepId()
    abstract val label: String?
    abstract val delayBefore: Long
}

@Serializable
data class ActionStep(
    override val label: String? = null,
    override val delayBefore: Long = 1000,
    val action: Action
) : Step()

@Serializable
sealed class BlockStep : Step() {
    abstract val steps: List<Step>
}

@Serializable
data class IfBlock(
    override val label: String? = null,
    override val delayBefore: Long = 0,
    override val steps: List<Step> = emptyList()
) : BlockStep()

@Serializable
data class ElseBlock(
    override val label: String? = null,
    override val delayBefore: Long = 0,
    override val steps: List<Step> = emptyList()
) : BlockStep()

@Serializable
data class ConditionalStep(
    override val label: String? = null,
    override val delayBefore: Long = 0,
    val condition: Condition,
    val ifBlock: IfBlock = IfBlock(),
    val elseBlock: ElseBlock = ElseBlock()
) : Step()

@Serializable
data class GroupBlock(
    override val label: String? = null,
    override val delayBefore: Long = 0,
    override val steps: List<Step>
) : BlockStep()

@Serializable
data class ObserverStep(
    override val label: String? = null,
    override val delayBefore: Long = 0,
    val condition: Condition,
    val steps: List<Step>
) : Step()
