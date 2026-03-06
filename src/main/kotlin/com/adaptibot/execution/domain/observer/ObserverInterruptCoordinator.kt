package com.adaptibot.execution.domain.observer

import com.adaptibot.model.ObserverStep
import com.adaptibot.model.Step
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

internal class ObserverInterruptCoordinator {
    private val logger = LoggerFactory.getLogger(ObserverInterruptCoordinator::class.java)
    private val triggeredObserver = AtomicReference<ObserverStep?>(null)

    private var executeSequence: ((List<Step>) -> Unit)? = null

    fun setExecuteSequenceCallback(callback: (List<Step>) -> Unit) {
        this.executeSequence = callback
    }

    fun queueObserver(observer: ObserverStep) {
        triggeredObserver.set(observer)
        logger.info("Observer queued for execution: ${observer.id.value}")
    }

    fun processObserverInterrupt() {
        triggeredObserver.getAndSet(null)?.let { observer ->
            logger.info("Executing triggered observer: ${observer.id.value}")
            try {
                executeSequence?.invoke(observer.steps)
            } finally {
                logger.info("Observer execution completed, resuming from interrupted step")
            }
        }
    }
}

