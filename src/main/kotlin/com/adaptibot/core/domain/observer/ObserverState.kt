package com.adaptibot.core.domain.observer

import com.adaptibot.common.model.ObserverStep

internal data class ObserverState(
    val observer: ObserverStep,
    val isActive: Boolean,
    val priority: Int
)
