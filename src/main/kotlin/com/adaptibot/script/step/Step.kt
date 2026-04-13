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
data class GroupStep(
    override val label: String? = null,
    override val delayBefore: Long = 0,
    val steps: List<Step> = emptyList()
) : Step()

@Serializable
data class ObserverStep(
    override val label: String? = null,
    override val delayBefore: Long = 0,
    val condition: Condition,
    val steps: List<Step> = emptyList()
) : Step()

@Serializable
data class ConditionalStep(
    override val label: String? = null,
    override val delayBefore: Long = 0,
    val condition: Condition,
    val trueBranch: Branch = Branch(),
    val elseBranch: Branch = Branch()
) : Step()

// ── Future loop steps ─────────────────────────────────────────────────────────
// Defined here so the sealed class hierarchy is exhaustive and the compiler
// enforces handling in every `when` expression. Execution is not yet implemented
// – ScriptInterpreter will throw UnsupportedOperationException for these types.

@Serializable
data class WhileStep(
    override val label: String? = null,
    override val delayBefore: Long = 0,
    val condition: Condition,
    val steps: List<Step> = emptyList()
) : Step()

@Serializable
data class ForStep(
    override val label: String? = null,
    override val delayBefore: Long = 0,
    val iterations: Int = 1,
    val steps: List<Step> = emptyList()
) : Step()
