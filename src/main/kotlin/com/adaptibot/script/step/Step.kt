package com.adaptibot.script.step

import com.adaptibot.script.value.Action
import com.adaptibot.script.value.Condition
import kotlinx.serialization.Serializable

@Serializable
sealed class Step {
    abstract val id: StepId
    abstract val label: String?
    abstract val enabled: Boolean
}

@Serializable
data class ActionStep(
    override val id: StepId = StepId(),
    override val label: String? = null,
    override val enabled: Boolean = true,
    val action: Action
) : Step()

@Serializable
data class GroupStep(
    override val id: StepId = StepId(),
    override val label: String? = null,
    override val enabled: Boolean = true,
    val container: StepContainer = StepContainer()
) : Step()

@Serializable
data class ObserverStep(
    override val id: StepId = StepId(),
    override val label: String? = null,
    override val enabled: Boolean = true,
    val condition: Condition,
    val container: StepContainer = StepContainer()
) : Step()

@Serializable
data class ConditionalStep(
    override val id: StepId = StepId(),
    override val label: String? = null,
    override val enabled: Boolean = true,
    val condition: Condition,
    val trueContainer: StepContainer = StepContainer(),
    val elseContainer: StepContainer = StepContainer()
) : Step()

@Serializable
data class WhileStep(
    override val id: StepId = StepId(),
    override val label: String? = null,
    override val enabled: Boolean = true,
    val condition: Condition,
    val container: StepContainer = StepContainer()
) : Step()

@Serializable
data class ForStep(
    override val id: StepId = StepId(),
    override val label: String? = null,
    override val enabled: Boolean = true,
    val iterations: Int = 1,
    val container: StepContainer = StepContainer()
) : Step()

// ── Extension helpers ─────────────────────────────────────────────────────────

fun Step.containers(): List<StepContainer> = when (this) {
    is ActionStep      -> emptyList()
    is GroupStep       -> listOf(container)
    is ObserverStep    -> listOf(container)
    is WhileStep       -> listOf(container)
    is ForStep         -> listOf(container)
    is ConditionalStep -> listOf(trueContainer, elseContainer)
}

fun Step.withUpdatedContainer(old: StepContainer, new: StepContainer): Step? = when (this) {
    is ActionStep      -> null
    is GroupStep       -> if (container.id == old.id) copy(container = new) else null
    is ObserverStep    -> if (container.id == old.id) copy(container = new) else null
    is WhileStep       -> if (container.id == old.id) copy(container = new) else null
    is ForStep         -> if (container.id == old.id) copy(container = new) else null
    is ConditionalStep -> when {
        trueContainer.id == old.id  -> copy(trueContainer = new)
        elseContainer.id == old.id  -> copy(elseContainer = new)
        else                        -> null
    }
}
