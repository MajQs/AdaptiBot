package com.adaptibot.core.executor.observer

import com.adaptibot.common.model.Step

data class ObserverState(
    val observer: Step.ObserverBlock,
    val isActive: Boolean,
    val priority: Int
)

