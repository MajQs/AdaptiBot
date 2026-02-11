package com.adaptibot.core.domain.observer

import com.adaptibot.common.model.ObserverStep
import com.adaptibot.core.domain.actions.ConditionEvaluator
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * INTERNAL - Manages observer lifecycle and priority-based execution.
 * Runs in separate thread to check conditions asynchronously without blocking main script execution.
 */
internal class ObserverManager(
    private val conditionEvaluator: ConditionEvaluator,
    private val checkDelayMs: Long = 1000
) {

    private val logger = LoggerFactory.getLogger(ObserverManager::class.java)

    private val observers = ConcurrentHashMap<ObserverStep, ObserverState>()
    private val isRunning = AtomicBoolean(false)
    private var observerScope: CoroutineScope? = null

    @Volatile
    private var onObserverTriggered: ((ObserverStep) -> Unit)? = null

    init {
        startObserverThread()
    }

    //TODO not sure if priority is needed
    fun registerObserver(observer: ObserverStep, priority: Int = 100) {
        observers[observer] = ObserverState(
            observer = observer,
            isActive = true,
            priority = priority
        )
        logger.debug("Registered observer: ${observer.id.value} with priority $priority")
    }

    fun unregisterObserver(observer: ObserverStep) {
        observers.remove(observer)
        logger.debug("Unregistered observer: ${observer.id.value}")
    }

    fun setOnObserverTriggered(callback: (ObserverStep) -> Unit) {
        onObserverTriggered = callback
    }

    fun clearAll() {
        logger.debug("Clearing all observers")
        observers.clear()
        stopObserverThread()
    }

    private fun checkObservers() {
        val activeObservers = observers.values
            .filter { it.isActive }
            .sortedByDescending { it.priority }

        for (state in activeObservers) {
            try {
                if (conditionEvaluator.evaluate(state.observer.condition)) {
                    logger.info("Observer triggered: ${state.observer.id.value}")
                    onObserverTriggered?.invoke(state.observer)
                    return
                }
            } catch (e: Exception) {
                logger.error("Error checking observer: ${state.observer.id.value}", e)
            }
        }
    }

    private fun startObserverThread() {
        if (isRunning.compareAndSet(false, true)) {
            observerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            observerScope?.launch {
                while (isRunning.get()) {
                    try {
                        checkObservers()
                    } catch (e: Exception) {
                        logger.error("Observer check failed", e)
                    }
                    delay(checkDelayMs)
                }
            }
            logger.debug("Observer thread started")
        }
    }

    private fun stopObserverThread() {
        if (isRunning.compareAndSet(true, false)) {
            observerScope?.cancel()
            observerScope = null
            logger.debug("Observer thread stopped")
        }
    }
}
