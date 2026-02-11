package com.adaptibot.core

import com.adaptibot.common.model.Script
import com.adaptibot.core.domain.ScriptOrchestrator
import com.adaptibot.core.dto.ExecutionStateDto

class CoreFacade internal constructor(
    private val scriptOrchestrator: ScriptOrchestrator
) {

    fun startScript(script: Script) = scriptOrchestrator.start(script)

    fun stopScript() = scriptOrchestrator.stop()

    fun getExecutionState(): ExecutionStateDto = scriptOrchestrator.getExecutionState()
}
