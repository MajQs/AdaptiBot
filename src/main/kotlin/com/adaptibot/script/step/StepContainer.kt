package com.adaptibot.script.step

import kotlinx.serialization.Serializable

/**
 * A mutable container of child [Step]s identified by a stable [ContainerId].
 *
 * Used by all container-type steps:
 *  - [GroupStep]       – one container (the step body)
 *  - [ObserverStep]    – one container (steps executed on trigger)
 *  - [WhileStep]       – one container (the loop body)
 *  - [ForStep]         – one container (the loop body)
 *  - [ConditionalStep] – two containers (trueContainer / elseContainer)
 *
 * Also used as the **root container** of a [Script], so every tree operation
 * in [StepTreeEditor] works uniformly on [StepContainer] – no special root case.
 *
 * Because [steps] is a [MutableList], [StepTreeEditor] can add/remove/reorder
 * children directly without rebuilding the path back to the root
 * (no `withUpdatedContainer` / `copy()` chain needed).
 */
@Serializable
data class StepContainer(
    val id: ContainerId = ContainerId(),
    val steps: MutableList<Step> = mutableListOf()
)
