package com.adaptibot.engine.domain

import com.adaptibot.model.Script
import com.adaptibot.engine.dto.ExecutionContext
import com.adaptibot.engine.dto.ExecutionState

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

    fun getExecutionState(): ExecutionState = scriptExecutionState.getState()

    private fun ExecutionContext.runLoop() {
        executionThread = Thread.ofVirtual()
            .name("script-execution")
            .start {
                scriptInterpreter.interpret(this.script)
            }
    }
}

