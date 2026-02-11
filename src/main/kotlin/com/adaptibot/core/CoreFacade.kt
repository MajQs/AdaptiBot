package com.adaptibot.core

import com.adaptibot.common.model.Script
import com.adaptibot.core.domain.ScriptExecutionService
import com.adaptibot.core.dto.ExecutionStateDto

class CoreFacade internal constructor(
    private val scriptExecutionService: ScriptExecutionService
) {

    fun startScript(script: Script) = scriptExecutionService.start(script)

    fun stopScript() = scriptExecutionService.stop()

    fun getExecutionState(): ExecutionStateDto = scriptExecutionService.getExecutionState()
}
