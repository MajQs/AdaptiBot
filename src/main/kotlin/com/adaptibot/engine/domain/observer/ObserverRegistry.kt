package com.adaptibot.engine.domain.observer

import com.adaptibot.common.model.ObserverStep
import com.adaptibot.engine.domain.actions.ConditionEvaluator
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Registry for managing observer lifecycle and execution.
 * Maintains observer state and coordinates condition checking.
 * Observer thread uses lazy initialization - starts only when first observer is registered.
 */
internal class ObserverRegistry(
    private val conditionEvaluator: ConditionEvaluator,
    private val checkDelayMs: Long = 1000
) {

    private val logger = LoggerFactory.getLogger(ObserverRegistry::class.java)

    private val observers = ConcurrentHashMap<ObserverStep, ObserverState>()
    private val scopeStack = ArrayDeque<MutableSet<ObserverStep>>()
    private val isRunning = AtomicBoolean(false)

    @Volatile
    private var observerThread: Thread? = null

    @Volatile
    private var onObserverTriggered: ((ObserverStep) -> Unit)? = null

    init {
        scopeStack.add(mutableSetOf()) // Global scope
    }

    fun enterScope() {
        scopeStack.addLast(mutableSetOf())
        logger.trace("Entered new observer scope. Depth: ${scopeStack.size}")
    }

    fun exitScope() {
        if (scopeStack.size <= 1) {
            logger.warn("Attempted to exit global observer scope. Ignoring.")
            return
        }
        val scopeObservers = scopeStack.removeLast()
        scopeObservers.forEach { unregisterObserver(it) }
        logger.trace("Exited observer scope. Unregistered ${scopeObservers.size} observers. Depth: ${scopeStack.size}")
    }

    fun registerObserver(observer: ObserverStep) {
        observers[observer] = ObserverState(
            observer = observer,
            isActive = true,
            priority = 100  // Default priority, can be made configurable later
        )
        scopeStack.lastOrNull()?.add(observer)
        logger.debug("Registered observer: ${observer.id.value} in scope depth ${scopeStack.size}")

        // Lazy start: ensure observer thread is running
        ensureObserverThreadRunning()
    }

    fun unregisterObserver(observer: ObserverStep) {
        observers.remove(observer)
        logger.debug("Unregistered observer: ${observer.id.value}")

        // Auto-stop thread when no observers remain
        if (observers.isEmpty()) {
            logger.debug("No more observers, stopping thread")
            stopObserverThread()
        }
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

    private fun ensureObserverThreadRunning() {
        if (isRunning.compareAndSet(false, true)) {
            observerThread = Thread.ofVirtual()
                .name("observer-registry")
                .start {
                    runObserverLoop()
                }
            logger.debug("Observer thread started (lazy initialization)")
        }
    }

    private fun runObserverLoop() {
        while (isRunning.get() && !Thread.currentThread().isInterrupted) {
            try {
                // Early exit if no observers
                if (observers.isEmpty()) {
                    Thread.sleep(checkDelayMs)
                    continue
                }

                checkObservers()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                logger.debug("Observer thread interrupted")
                break
            } catch (e: Exception) {
                logger.error("Observer check failed", e)
            }

            try {
                Thread.sleep(checkDelayMs)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                logger.debug("Observer sleep interrupted")
                break
            }
        }
        logger.debug("Observer thread stopped")
    }

    private fun stopObserverThread() {
        if (isRunning.compareAndSet(true, false)) {
            observerThread?.interrupt()
            observerThread = null
            logger.debug("Observer thread stopped")
        }
    }
}

