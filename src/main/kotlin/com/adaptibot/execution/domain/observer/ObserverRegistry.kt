package com.adaptibot.execution.domain.observer

import com.adaptibot.script.step.ObserverStep
import com.adaptibot.infrastructure.InterruptibleSleep
import com.adaptibot.execution.domain.ConditionEvaluator
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

internal class ObserverRegistry(
    private val conditionEvaluator: ConditionEvaluator,
    @Volatile var checkDelayMs: Long = 1000
) {

    private val logger = LoggerFactory.getLogger(ObserverRegistry::class.java)

    private val observersScopeStack = ArrayDeque<MutableSet<ObserverStep>>()
    
    @Volatile
    private var activeObservers: List<ObserverStep> = emptyList()

    private val isRunning = AtomicBoolean(false)

    private var observerThread: Thread? = null

    @Volatile
    private var onObserverTriggered: ((ObserverStep) -> Unit)? = null

    /** Pushing an empty scope cannot change the flattened snapshot, so none is published here. */
    fun enterScope() {
        observersScopeStack.addLast(linkedSetOf())
    }

    fun activateObserver(observer: ObserverStep) {
        observersScopeStack.lastOrNull()?.add(observer)
        logger.debug("Activated observer: ${observer.id.value} in scope depth ${observersScopeStack.size}")
        publishSnapshot()

        // Lazy start: ensure observer thread is running
        ensureObserverThreadRunning()
    }

    fun exitScope() {
        observersScopeStack.removeLastOrNull()
        publishSnapshot()
        if (observersScopeStack.isEmpty()) {
            stopObserverThread()
        }
    }

    private fun publishSnapshot() {
        activeObservers = if (observersScopeStack.isEmpty()) {
            emptyList()
        } else {
            observersScopeStack.flatMap { it }
        }
    }

    fun setOnObserverTriggered(callback: (ObserverStep) -> Unit) {
        onObserverTriggered = callback
    }

    fun clearAll() {
        logger.debug("Clearing all observers")
        observersScopeStack.clear()
        activeObservers = emptyList()
        stopObserverThread()
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
        try {
            while (isRunning.get() && !Thread.currentThread().isInterrupted) {
                checkObservers()
                InterruptibleSleep.sleep(checkDelayMs)
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.debug("Observer loop interrupted")
        }
    }

    private fun checkObservers() {
        activeObservers.forEach {
            try {
                if (conditionEvaluator.evaluate(it.condition)) {
                    logger.info("Observer triggered: ${it.id.value}")
                    onObserverTriggered?.invoke(it)
                    return
                }
            } catch (e: Exception) {
                logger.error("Error checking observer: ${it.id.value}", e)
            }
        }
    }

    private fun stopObserverThread() {
        if (isRunning.compareAndSet(true, false)) {
            observerThread?.interrupt()
            observerThread = null
            logger.debug("Observer thread stopped")
        }
    }
}
