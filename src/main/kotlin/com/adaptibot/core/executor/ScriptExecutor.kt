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
        com.adaptibot.ui.model.ExecutionLogger.logExecutionStart(script.name)
        
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
            com.adaptibot.ui.model.ExecutionLogger.logExecutionPause()
            currentContext = currentContext.copy(state = ExecutionState.PAUSED)
        }
    }
    
    override fun resume() {
        if (currentContext.state == ExecutionState.PAUSED) {
            logger.info("Resuming script execution")
            com.adaptibot.ui.model.ExecutionLogger.logExecutionResume()
            currentContext = currentContext.copy(state = ExecutionState.RUNNING)
        }
    }
    
    override fun stop() {
        logger.info("Stopping script execution")
        com.adaptibot.ui.model.ExecutionLogger.logExecutionStop()
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
            is Step.ActionStep -> executeActionStep(step)
            is Step.ConditionalBlock -> executeConditionalBlock(step)
            is Step.ObserverBlock -> registerObserver(step)
            is Step.GroupBlock -> executeGroupBlock(step)
        }
        
        if (step.delayAfter > 0) {
            delay(step.delayAfter)
        }
    }
    
    private fun executeActionStep(step: Step.ActionStep) {
        val stepName = step.label ?: step.action::class.simpleName ?: "Action"
        val startTime = System.currentTimeMillis()
        
        try {
            val coordinate = when (val action = step.action) {
                is com.adaptibot.common.model.Action.Mouse -> {
                    val target = when (action) {
                        is com.adaptibot.common.model.Action.Mouse.LeftClick -> action.target
                        is com.adaptibot.common.model.Action.Mouse.RightClick -> action.target
                        is com.adaptibot.common.model.Action.Mouse.DoubleClick -> action.target
                        is com.adaptibot.common.model.Action.Mouse.MoveTo -> action.target
                        else -> null
                    }
                    target?.let { elementFinder.find(it) }
                }
                else -> null
            }
            
            val success = actionExecutor.execute(step.action, coordinate)
            val duration = System.currentTimeMillis() - startTime
            
            if (success) {
                com.adaptibot.ui.model.ExecutionLogger.logStepSuccess(stepName, duration)
            } else {
                com.adaptibot.ui.model.ExecutionLogger.logStepFailure(stepName, duration, "Action failed")
                logger.error("Action execution failed: ${step.label ?: step.id.value}")
            }
            
            handleFlowControl(step.action)
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            com.adaptibot.ui.model.ExecutionLogger.logStepFailure(stepName, duration, e.message ?: "Exception")
            logger.error("Exception executing action step: ${step.label ?: step.id.value}", e)
        }
    }
    
    private suspend fun executeConditionalBlock(block: Step.ConditionalBlock) {
        try {
            val conditionMet = conditionEvaluator.evaluate(block.condition)
            
            val stepsToExecute = if (conditionMet) {
                block.thenSteps
            } else {
                block.elseSteps
            }
            
            for (step in stepsToExecute) {
                if (shouldStop || currentContext.state == ExecutionState.PAUSED) {
                    return
                }
                executeStep(step)
            }
            
        } catch (e: Exception) {
            logger.error("Exception executing conditional block: ${block.label ?: block.id.value}", e)
        }
    }
    
    private fun registerObserver(block: Step.ObserverBlock) {
        try {
            val priority = calculateObserverPriority(block)
            observerManager.registerObserver(block, priority)
            
        } catch (e: Exception) {
            logger.error("Exception registering observer: ${block.label ?: block.id.value}", e)
        }
    }
    
    private suspend fun executeGroupBlock(block: Step.GroupBlock) {
        try {
            for (step in block.steps) {
                if (shouldStop || currentContext.state == ExecutionState.PAUSED) {
                    return
                }
                executeStep(step)
            }
            
        } catch (e: Exception) {
            logger.error("Exception executing group block: ${block.label ?: block.id.value}", e)
        }
    }
    
    private fun handleFlowControl(action: com.adaptibot.common.model.Action) {
        when (action) {
            is com.adaptibot.common.model.Action.Flow.Stop -> {
                stop()
            }
            is com.adaptibot.common.model.Action.Flow.JumpTo -> {
                // TODO: Implement jump logic
                logger.warn("JumpTo not yet implemented")
            }
            is com.adaptibot.common.model.Action.Flow.Continue -> {
                // Continue to next step (default behavior)
            }
            else -> {}
        }
    }
    
    private fun calculateObserverPriority(observer: Step.ObserverBlock): Int {
        // Simple priority for now - can be enhanced later
        return 100
    }
}

