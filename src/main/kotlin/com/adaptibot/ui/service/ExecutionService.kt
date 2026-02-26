package com.adaptibot.ui.service

import com.adaptibot.model.Script
import com.adaptibot.execution.ExecutionConfiguration
import com.adaptibot.execution.ExecutionFacade
import com.adaptibot.execution.dto.ExecutionStateDto
import org.slf4j.LoggerFactory

class ExecutionService {
    
    private val logger = LoggerFactory.getLogger(ExecutionService::class.java)
    
    private val executionFacade: ExecutionFacade = ExecutionConfiguration.getFacade()

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
        executionFacade.startScript(scriptToRun)
    }
    
    fun stop() {
        executionFacade.stopScript()
    }
    
    fun getState(): ExecutionStateDto {
        return executionFacade.getExecutionState()
    }
    
    fun isRunning(): Boolean {
        return executionFacade.getExecutionState() == ExecutionStateDto.RUNNING
    }
}

