package com.adaptibot.ui.service

import com.adaptibot.model.Script
import com.adaptibot.engine.EngineConfiguration
import com.adaptibot.engine.EngineFacade
import com.adaptibot.engine.dto.ExecutionStateDto
import org.slf4j.LoggerFactory

class ExecutionService {
    
    private val logger = LoggerFactory.getLogger(ExecutionService::class.java)
    
    private val engineFacade: EngineFacade = EngineConfiguration.getFacade()

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
        engineFacade.startScript(scriptToRun)
    }
    
    fun stop() {
        engineFacade.stopScript()
    }
    
    fun getState(): ExecutionStateDto {
        return engineFacade.getExecutionState()
    }
    
    fun isRunning(): Boolean {
        return engineFacade.getExecutionState() == ExecutionStateDto.RUNNING
    }
}

