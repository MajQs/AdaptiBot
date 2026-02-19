package com.adaptibot.engine

import com.adaptibot.common.model.Script
import com.adaptibot.engine.domain.ScriptRunner
import com.adaptibot.engine.dto.ExecutionStateDto

class EngineFacade internal constructor(
    private val scriptRunner: ScriptRunner
) {

    fun startScript(script: Script) = scriptRunner.execute(script)

    fun stopScript() = scriptRunner.stop()

    fun getExecutionState(): ExecutionStateDto = ExecutionStateDto.valueOf(scriptRunner.getExecutionState().name)
}
