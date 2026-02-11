package com.adaptibot.ui.service

import com.adaptibot.common.model.Script
import com.adaptibot.core.CoreConfiguration
import com.adaptibot.core.CoreFacade
import com.adaptibot.core.dto.ExecutionStateDto
import org.slf4j.LoggerFactory

class ExecutionService {
    
    private val logger = LoggerFactory.getLogger(ExecutionService::class.java)
    
    private val coreFacade: CoreFacade = CoreConfiguration.getFacade()

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
        coreFacade.startScript(scriptToRun)
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
}

