package com.adaptibot.core.executor.observer

import com.adaptibot.common.model.Step
import com.adaptibot.core.executor.actions.IConditionEvaluator
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages observer lifecycle and priority-based execution.
 * Runs in separate thread to check conditions asynchronously without blocking main script execution.
 */
class ObserverManager(
    private val conditionEvaluator: IConditionEvaluator,
    private val checkDelayMs: Long = 1000
) : IObserverManager {
    
    private val logger = LoggerFactory.getLogger(ObserverManager::class.java)
    
    private val observers = ConcurrentHashMap<Step.ObserverBlock, ObserverState>()
    private val isRunning = AtomicBoolean(false)
    private var observerScope: CoroutineScope? = null
    
    @Volatile
    private var onObserverTriggered: ((Step.ObserverBlock) -> Unit)? = null
    
    init {
        startObserverThread()
    }
    
    override fun registerObserver(observer: Step.ObserverBlock, priority: Int) {
        observers[observer] = ObserverState(
            observer = observer,
            isActive = true,
            priority = priority
        )
        logger.debug("Registered observer: ${observer.id.value} with priority $priority")
    }
    
    override fun unregisterObserver(observer: Step.ObserverBlock) {
        observers.remove(observer)
        logger.debug("Unregistered observer: ${observer.id.value}")
    }
    
    override fun setOnObserverTriggered(callback: (Step.ObserverBlock) -> Unit) {
        onObserverTriggered = callback
    }
    
    override fun clearAll() {
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

