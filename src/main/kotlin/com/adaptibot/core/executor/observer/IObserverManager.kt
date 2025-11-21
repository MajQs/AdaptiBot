package com.adaptibot.core.executor.observer

import com.adaptibot.common.model.Step

/**
 * Manages observer lifecycle and priority-based execution.
 * Runs in separate thread to check conditions asynchronously.
 */
interface IObserverManager {
    fun registerObserver(observer: Step.ObserverBlock, priority: Int)
    fun unregisterObserver(observer: Step.ObserverBlock)
    fun checkObservers(): Step.ObserverBlock?
    fun clearAll()
}

