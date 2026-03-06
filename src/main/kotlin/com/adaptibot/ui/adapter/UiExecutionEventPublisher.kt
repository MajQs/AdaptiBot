package com.adaptibot.ui.adapter

import com.adaptibot.execution.domain.ExecutionEventPublisher
import com.adaptibot.ui.viewmodel.ScriptViewModel

/**
 * Bridge between backend execution engine and the UI ViewModel.
 * Constructed before ViewModel exists; [attachViewModel] must be called once ViewModel is ready.
 */
class UiExecutionEventPublisher(@Volatile private var viewModel: ScriptViewModel?) : ExecutionEventPublisher {

    fun attachViewModel(vm: ScriptViewModel) { viewModel = vm }

    override fun logExecutionStart(scriptName: String) = viewModel?.onExecutionStart(scriptName) ?: Unit

    override fun logExecutionStop() = viewModel?.onExecutionStop() ?: Unit

    override fun logStepSuccess(stepName: String, durationMs: Long) =
        viewModel?.onStepExecuted(stepName, durationMs) ?: Unit

    override fun logStepFailure(stepName: String, durationMs: Long, error: String) =
        viewModel?.onStepFailed(stepName, durationMs, error) ?: Unit
}

