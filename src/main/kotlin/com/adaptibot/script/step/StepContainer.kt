package com.adaptibot.script.step

import kotlinx.serialization.Serializable

@Serializable
data class StepContainer(
    val id: ContainerId = ContainerId(),
    val steps: List<Step> = emptyList()
)
