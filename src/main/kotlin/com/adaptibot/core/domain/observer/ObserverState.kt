package com.adaptibot.core.domain.observer

import com.adaptibot.common.model.ObserverStep

/**
 * INTERNAL - State of an observer.
 */
internal data class ObserverState(
    val observer: ObserverStep,
    val isActive: Boolean,
    val priority: Int
)
