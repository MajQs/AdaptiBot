package com.adaptibot.execution.domain.observer

import com.adaptibot.script.step.ObserverStep
import com.adaptibot.infrastructure.InterruptibleSleep
import com.adaptibot.infrastructure.SharedScreenFrame
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

    /**
     * Observers whose handler is currently running. An observer must not be able to trigger itself
     * while it is already handling, so it is excluded from the published snapshot until its handler
     * finishes. Mutated only by the script execution thread.
     */
    private val handlingObservers = linkedSetOf<ObserverStep>()

    private val isRunning = AtomicBoolean(false)

    /**
     * Set when an observer has matched and is waiting to be picked up by the execution thread.
     * While it is set no condition is evaluated: a further match could not be executed anyway
     * (only one handler runs at a time) and evaluating conditions is the expensive part.
     */
    private val triggerPending = AtomicBoolean(false)

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

        // Lazy start: the thread lives for the whole script run and only idles when nothing is armed.
        ensureObserverThreadRunning()
    }

    /**
     * Leaving a scope disarms its observers but never stops the observer thread — with an empty
     * snapshot the loop has nothing to evaluate and just sleeps, which avoids interrupting and
     * recreating the thread on every iteration of the main script loop.
     */
    fun exitScope() {
        observersScopeStack.removeLastOrNull()
        publishSnapshot()
    }

    /**
     * Marks the start of [observer]'s handler. From now on the observer is not checked, which
     * prevents it from re-triggering itself while its own steps are running.
     */
    fun markHandlerStarted(observer: ObserverStep) {
        handlingObservers.add(observer)
        publishSnapshot()
        // The pending trigger has been picked up; from now on the self-lock protects the observer
        // and the remaining observers may be checked again.
        triggerPending.set(false)
        logger.debug("Observer self-locked while handling: ${observer.id.value}")
    }

    /** Marks the end of [observer]'s handler; the observer is armed again immediately. */
    fun markHandlerFinished(observer: ObserverStep) {
        handlingObservers.remove(observer)
        publishSnapshot()
        logger.debug("Observer re-armed after handling: ${observer.id.value}")
    }

    /**
     * Rebuilds the snapshot read by the observer thread, ordered from the outermost scope to the
     * innermost one and, within a scope, in activation (tree) order — so the first match wins.
     * Called from the execution thread only.
     */
    private fun publishSnapshot() {
        activeObservers = if (observersScopeStack.isEmpty()) {
            emptyList()
        } else {
            observersScopeStack.flatMap { scope -> scope.filterNot { it in handlingObservers } }
        }
    }

    fun setOnObserverTriggered(callback: (ObserverStep) -> Unit) {
        onObserverTriggered = callback
    }

    fun clearAll() {
        logger.debug("Clearing all observers")
        observersScopeStack.clear()
        handlingObservers.clear()
        activeObservers = emptyList()
        triggerPending.set(false)
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

    /**
     * One evaluation pass over the armed observers. All captures taken during the pass are shared,
     * so observers watching the same area pay for a single screenshot and see a consistent screen
     * state.
     */
    private fun checkObservers() {
        if (triggerPending.get()) return
        val observers = activeObservers
        if (observers.isEmpty()) return

        SharedScreenFrame.shareCaptures {
            observers.forEach {
                try {
                    if (conditionEvaluator.evaluate(it.condition)) {
                        logger.info("Observer triggered: ${it.id.value}")
                        triggerPending.set(true)
                        onObserverTriggered?.invoke(it)
                        return@shareCaptures
                    }
                } catch (e: Exception) {
                    logger.error("Error checking observer: ${it.id.value}", e)
                }
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
