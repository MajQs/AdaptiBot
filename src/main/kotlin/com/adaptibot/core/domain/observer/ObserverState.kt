package com.adaptibot.core.domain.observer

import com.adaptibot.common.model.Step

/**
 * INTERNAL - State of an observer.
 */
internal data class ObserverState(
    val observer: Step.ObserverBlock,
    val isActive: Boolean,
    val priority: Int
)

