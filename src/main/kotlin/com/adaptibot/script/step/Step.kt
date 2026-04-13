package com.adaptibot.script.step

import com.adaptibot.script.value.Action
import com.adaptibot.script.value.Condition
import kotlinx.serialization.Serializable

@Serializable
sealed class Step {
    abstract val id: StepId
    abstract val label: String?
    abstract val delayBefore: Long
}

@Serializable
data class ActionStep(
    override val id: StepId = StepId(),
    override val label: String? = null,
    override val delayBefore: Long = 1000,
    val action: Action
) : Step()

@Serializable
data class GroupStep(
    override val id: StepId = StepId(),
    override val label: String? = null,
    override val delayBefore: Long = 0,
    val container: StepContainer = StepContainer()
) : Step()

@Serializable
data class ObserverStep(
    override val id: StepId = StepId(),
    override val label: String? = null,
    override val delayBefore: Long = 0,
    val condition: Condition,
    val container: StepContainer = StepContainer()
) : Step()

@Serializable
data class ConditionalStep(
    override val id: StepId = StepId(),
    override val label: String? = null,
    override val delayBefore: Long = 0,
    val condition: Condition,
    val trueContainer: StepContainer = StepContainer(),
    val elseContainer: StepContainer = StepContainer()
) : Step()

// ── Future loop steps ─────────────────────────────────────────────────────────
// Defined here so the sealed class hierarchy is exhaustive and the compiler
// enforces handling in every `when` expression. Execution is not yet implemented
// – ScriptInterpreter will throw UnsupportedOperationException for these types.

@Serializable
data class WhileStep(
    override val id: StepId = StepId(),
    override val label: String? = null,
    override val delayBefore: Long = 0,
    val condition: Condition,
    val container: StepContainer = StepContainer()
) : Step()

@Serializable
data class ForStep(
    override val id: StepId = StepId(),
    override val label: String? = null,
    override val delayBefore: Long = 0,
    val iterations: Int = 1,
    val container: StepContainer = StepContainer()
) : Step()

// ── Extension helpers ─────────────────────────────────────────────────────────

/**
 * Returns all direct [StepContainer]s owned by this step.
 * [ActionStep] has none; single-container steps return one; [ConditionalStep] returns two.
 */
fun Step.containers(): List<StepContainer> = when (this) {
    is ActionStep      -> emptyList()
    is GroupStep       -> listOf(container)
    is ObserverStep    -> listOf(container)
    is WhileStep       -> listOf(container)
    is ForStep         -> listOf(container)
    is ConditionalStep -> listOf(trueContainer, elseContainer)
}


