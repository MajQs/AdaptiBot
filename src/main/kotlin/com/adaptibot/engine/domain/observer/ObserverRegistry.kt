package com.adaptibot.engine.domain.observer

import com.adaptibot.common.model.ObserverStep
import com.adaptibot.engine.domain.actions.ConditionEvaluator
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

internal class ObserverRegistry(
    private val conditionEvaluator: ConditionEvaluator,
    private val checkDelayMs: Long = 1000
) {

    private val logger = LoggerFactory.getLogger(ObserverRegistry::class.java)

    private val observersScopeStack = ArrayDeque<MutableSet<ObserverStep>>()
    private val isRunning = AtomicBoolean(false)

    private var observerThread: Thread? = null

    @Volatile
    private var onObserverTriggered: ((ObserverStep) -> Unit)? = null

    fun enterScope() {
        observersScopeStack.addLast(mutableSetOf())
    }

    fun registerObserver(observer: ObserverStep) {
        observersScopeStack.lastOrNull()?.add(observer)
        logger.debug("Registered observer: ${observer.id.value} in scope depth ${observersScopeStack.size}")

        // Lazy start: ensure observer thread is running
        ensureObserverThreadRunning()
    }

    fun exitScope() {
        observersScopeStack.removeLast()
        if (observersScopeStack.isEmpty()) {
            stopObserverThread()
        }
    }

    fun setOnObserverTriggered(callback: (ObserverStep) -> Unit) {
        onObserverTriggered = callback
    }

    fun clearAll() {
        logger.debug("Clearing all observers")
        observersScopeStack.clear()
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
        while (isRunning.get() && !Thread.currentThread().isInterrupted) {
            checkObservers()
            Thread.sleep(checkDelayMs)
        }
    }

    private fun checkObservers() {
        observersScopeStack.flatMap { it.asSequence() }.forEach {
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

