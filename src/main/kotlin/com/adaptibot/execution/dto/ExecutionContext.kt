package com.adaptibot.execution.dto

import com.adaptibot.script.Script
import com.adaptibot.script.ScriptId
import com.adaptibot.script.ScriptSettings
import com.adaptibot.script.step.Step

private val EMPTY_SCRIPT = Script.restore(
    id = ScriptId(""),
    name = "",
    description = "",
    steps = emptyList(),
    settings = ScriptSettings()
)

internal data class ExecutionContext(
    val script: Script = EMPTY_SCRIPT, //TODO dirty, to fix it
    val state: ExecutionStateDto = ExecutionStateDto.IDLE,
    val activeStep: Step? = null
) {
    companion object {
        @JvmStatic
        fun default() = ExecutionContext()
        fun runFor(script: Script) = ExecutionContext(script = script, state = ExecutionStateDto.RUNNING)
    }
}
