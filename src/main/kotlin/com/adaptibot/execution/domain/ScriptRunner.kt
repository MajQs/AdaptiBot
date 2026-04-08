package com.adaptibot.execution.domain

import com.adaptibot.script.Script
import com.adaptibot.execution.dto.ExecutionContext
import com.adaptibot.execution.dto.ExecutionStateDto

internal class ScriptRunner(
    private val scriptExecutionState: ScriptExecutionState,
    private val scriptInterpreter: ScriptInterpreter,
) {

    @Volatile
    private var executionThread: Thread? = null

    fun execute(script: Script) {
        scriptExecutionState.create(script)
            .runLoop()
    }

    fun stop() {
        scriptExecutionState.stop()

        executionThread?.interrupt()
        executionThread = null
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
