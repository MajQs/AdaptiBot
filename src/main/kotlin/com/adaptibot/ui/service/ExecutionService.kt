package com.adaptibot.ui.service

import com.adaptibot.common.model.Script
import com.adaptibot.core.executor.ExecutionState
import com.adaptibot.core.executor.IScriptExecutor
import com.adaptibot.core.executor.ScriptExecutor
import com.adaptibot.core.executor.actions.ActionDispatcher
import com.adaptibot.core.executor.actions.ConditionEvaluatorImpl
import com.adaptibot.core.executor.actions.ElementFinderImpl
import com.adaptibot.core.executor.observer.ObserverManager
import org.slf4j.LoggerFactory

class ExecutionService {
    
    private val logger = LoggerFactory.getLogger(ExecutionService::class.java)
    
    private val elementFinder = ElementFinderImpl()
    private val actionExecutor = ActionDispatcher()
    private val conditionEvaluator = ConditionEvaluatorImpl(elementFinder)
    private val observerManager = ObserverManager(conditionEvaluator)
    
    private val executor: IScriptExecutor = ScriptExecutor(
        actionExecutor = actionExecutor,
        elementFinder = elementFinder,
        conditionEvaluator = conditionEvaluator,
        observerManager = observerManager
    )
    
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
        executor.start(scriptToRun)
    }
    
    fun pause() {
        executor.pause()
    }
    
    fun resume() {
        executor.resume()
    }
    
    fun stop() {
        executor.stop()
    }
    
    fun getState(): ExecutionState {
        return executor.getState()
    }
    
    fun isRunning(): Boolean {
        return executor.getState() == ExecutionState.RUNNING
    }
    
    fun isPaused(): Boolean {
        return executor.getState() == ExecutionState.PAUSED
    }
}

