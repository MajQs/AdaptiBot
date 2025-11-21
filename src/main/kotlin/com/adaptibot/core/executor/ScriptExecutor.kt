package com.adaptibot.core.executor

import com.adaptibot.common.model.Script
import com.adaptibot.common.model.Step
import com.adaptibot.core.executor.actions.IActionExecutor
import com.adaptibot.core.executor.actions.IConditionEvaluator
import com.adaptibot.core.executor.actions.IElementFinder
import com.adaptibot.core.executor.observer.IObserverManager
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

/**
 * Main script execution engine.
 * Manages infinite loop execution, state transitions, and observer coordination.
 */
class ScriptExecutor(
    private val actionExecutor: IActionExecutor,
    private val elementFinder: IElementFinder,
    private val conditionEvaluator: IConditionEvaluator,
    private val observerManager: IObserverManager
) : IScriptExecutor {
    
    private val logger = LoggerFactory.getLogger(ScriptExecutor::class.java)
    
    private var executionScope: CoroutineScope? = null
    private var currentContext: ExecutionContext = ExecutionContext(
        script = Script("", steps = emptyList()),
        state = ExecutionState.IDLE
    )
    
    @Volatile
    private var shouldStop = false
    
    override fun start(script: Script) {
        if (currentContext.state != ExecutionState.IDLE) {
            logger.warn("Cannot start script - already running")
            return
        }
        
        logger.info("Starting script execution: ${script.name}")
        shouldStop = false
        currentContext = ExecutionContext(
            script = script,
            state = ExecutionState.RUNNING
        )
        
        executionScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        executionScope?.launch {
            executeInfiniteLoop()
        }
    }
    
    override fun pause() {
        if (currentContext.state == ExecutionState.RUNNING) {
            logger.info("Pausing script execution")
            currentContext = currentContext.copy(state = ExecutionState.PAUSED)
        }
    }
    
    override fun resume() {
        if (currentContext.state == ExecutionState.PAUSED) {
            logger.info("Resuming script execution")
            currentContext = currentContext.copy(state = ExecutionState.RUNNING)
        }
    }
    
    override fun stop() {
        logger.info("Stopping script execution")
        shouldStop = true
        currentContext = currentContext.copy(state = ExecutionState.STOPPED)
        executionScope?.cancel()
        observerManager.clearAll()
    }
    
    override fun getState(): ExecutionState = currentContext.state
    
    override fun getContext(): ExecutionContext = currentContext
    
    private suspend fun executeInfiniteLoop() {
        while (!shouldStop && currentContext.state != ExecutionState.STOPPED) {
            if (currentContext.state == ExecutionState.PAUSED) {
                delay(100)
                continue
            }
            
            executeIteration()
            
            currentContext = currentContext.copy(
                iterationCount = currentContext.iterationCount + 1
            )
        }
        
        currentContext = currentContext.copy(state = ExecutionState.IDLE)
    }
    
    private suspend fun executeIteration() {
        currentContext.script.steps.forEach { step ->
            if (shouldStop || currentContext.state == ExecutionState.PAUSED) {
                return
            }
            executeStep(step)
        }
    }
    
    private suspend fun executeStep(step: Step) {
        currentContext = currentContext.copy(currentStepId = step.id)
        
        if (step.delayBefore > 0) {
            delay(step.delayBefore)
        }
        
        when (step) {
            is Step.ActionStep -> {
                // TODO: Execute action
            }
            is Step.ConditionalBlock -> {
                // TODO: Evaluate condition and execute branch
            }
            is Step.ObserverBlock -> {
                // TODO: Register observer
            }
            is Step.GroupBlock -> {
                // TODO: Execute group steps
            }
        }
        
        if (step.delayAfter > 0) {
            delay(step.delayAfter)
        }
    }
}

