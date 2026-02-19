package com.adaptibot.ui.adapter

import com.adaptibot.engine.domain.ExecutionEventPublisher
import com.adaptibot.ui.model.ExecutionLogger

/**
 * Adapter that bridges the domain layer ExecutionEventPublisher interface
 * to the UI layer ExecutionLogger implementation.
 */
class UiExecutionEventPublisher : ExecutionEventPublisher {

    override fun logExecutionStart(scriptName: String) {
        ExecutionLogger.logExecutionStart(scriptName)
    }

    override fun logExecutionStop() {
        ExecutionLogger.logExecutionStop()
    }

    override fun logStepSuccess(stepName: String, durationMs: Long) {
        ExecutionLogger.logStepSuccess(stepName, durationMs)
    }

    override fun logStepFailure(stepName: String, durationMs: Long, error: String) {
        ExecutionLogger.logStepFailure(stepName, durationMs, error)
    }
}

