package com.adaptibot.ui.service

import com.adaptibot.common.model.Script
import com.adaptibot.core.CoreModule
import com.adaptibot.core.CoreFacade
import com.adaptibot.core.dto.ScriptExecutionInputDto
import com.adaptibot.core.dto.ExecutionStateDto
import org.slf4j.LoggerFactory

class ExecutionService {
    
    private val logger = LoggerFactory.getLogger(ExecutionService::class.java)
    
    private val coreFacade: CoreFacade = CoreModule.create()

    private var currentScript: Script? = null
    
    fun start(script: Script? = null) {
        val scriptToRun = script ?: currentScript
        
        if (scriptToRun == null) {
            logger.warn("Cannot start - no script loaded")
            return
        }
        
        if (scriptToRun.steps.isEmpty()) {
            logger.warn("Cannot start - script has no steps")
            return
        }
        
        currentScript = scriptToRun
        val input = ScriptExecutionInputDto(scriptToRun)
        coreFacade.startScript(input)
    }
    
    fun pause() {
        coreFacade.pauseScript()
    }
    
    fun resume() {
        coreFacade.resumeScript()
    }
    
    fun stop() {
        coreFacade.stopScript()
    }
    
    fun getState(): ExecutionStateDto {
        return coreFacade.getExecutionState()
    }
    
    fun isRunning(): Boolean {
        return coreFacade.getExecutionState() == ExecutionStateDto.RUNNING
    }
    
    fun isPaused(): Boolean {
        return coreFacade.getExecutionState() == ExecutionStateDto.PAUSED
    }
}

