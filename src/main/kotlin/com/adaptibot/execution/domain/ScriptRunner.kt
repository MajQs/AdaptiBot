package com.adaptibot.execution.domain

import com.adaptibot.script.Script
import com.adaptibot.execution.dto.ExecutionContext
import com.adaptibot.execution.dto.ExecutionStateDto
import com.adaptibot.vision.metrics.VisionMetrics

internal class ScriptRunner(
    private val scriptExecutionState: ScriptExecutionState,
    private val scriptInterpreter: ScriptInterpreter,
) {

    @Volatile
    private var executionThread: Thread? = null

    fun execute(script: Script) {
        VisionMetrics.reset()
        scriptExecutionState.create(script)
            .runLoop()
    }

    @Synchronized
    fun stop() {
        scriptExecutionState.stop()

        executionThread?.interrupt()
        executionThread = null

        VisionMetrics.logSummary()
    }

    fun getExecutionState(): ExecutionStateDto = scriptExecutionState.getState()

    private fun ExecutionContext.runLoop() {
        executionThread = Thread.ofVirtual()
            .name("script-execution")
            .start {
                scriptInterpreter.interpret(this.script)
            }
    }
}
