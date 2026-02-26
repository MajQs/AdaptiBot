package com.adaptibot.execution

import com.adaptibot.model.Script
import com.adaptibot.execution.domain.ScriptRunner
import com.adaptibot.execution.dto.ExecutionStateDto

class ExecutionFacade internal constructor(
    private val scriptRunner: ScriptRunner
) {

    fun startScript(script: Script) = scriptRunner.execute(script)

    fun stopScript() = scriptRunner.stop()

    fun getExecutionState(): ExecutionStateDto = ExecutionStateDto.valueOf(scriptRunner.getExecutionState().name)
}
