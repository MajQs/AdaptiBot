package com.adaptibot.script.step

import kotlinx.serialization.Serializable

/**
 * A branch of a [ConditionalStep] (TRUE or ELSE).
 *
 * Intentionally NOT a [Step] – a branch has no label, no delayBefore and no execution semantics
 * of its own. It is purely a named container of child steps owned by [ConditionalStep].
 */
@Serializable
data class Branch(
    val id: BranchId = BranchId(),
    val steps: List<Step> = emptyList()
)

