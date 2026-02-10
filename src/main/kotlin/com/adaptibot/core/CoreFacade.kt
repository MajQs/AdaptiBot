package com.adaptibot.core

import com.adaptibot.core.domain.ScriptExecutor
import com.adaptibot.core.dto.ExecutionContextDto
import com.adaptibot.core.dto.ExecutionStateDto
import com.adaptibot.core.dto.ScriptExecutionInputDto

class CoreFacade internal constructor(
    private val scriptExecutor: ScriptExecutor
) {

    fun startScript(input: ScriptExecutionInputDto) = scriptExecutor.start(input.script)

    fun pauseScript() = scriptExecutor.pause()

    fun resumeScript() = scriptExecutor.resume()

    fun stopScript() = scriptExecutor.stop()

    fun getExecutionState(): ExecutionStateDto = scriptExecutor.getState()

}

