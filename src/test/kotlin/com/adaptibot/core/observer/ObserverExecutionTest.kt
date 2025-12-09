package com.adaptibot.core.observer

import com.adaptibot.common.model.*
import com.adaptibot.core.executor.ScriptExecutor
import com.adaptibot.core.executor.actions.ConditionEvaluatorImpl
import com.adaptibot.core.executor.actions.IActionExecutor
import com.adaptibot.core.executor.actions.IElementFinder
import com.adaptibot.core.executor.observer.ObserverManager
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ObserverExecutionTest {
    
    private lateinit var actionExecutor: IActionExecutor
    private lateinit var elementFinder: IElementFinder
    private lateinit var observerManager: ObserverManager
    private lateinit var scriptExecutor: ScriptExecutor
    
    @BeforeEach
    fun setup() {
        actionExecutor = mockk(relaxed = true)
        elementFinder = mockk()
        
        every { actionExecutor.execute(any(), any()) } returns true
    }
    
    @AfterEach
    fun tearDown() {
        if (::scriptExecutor.isInitialized) {
            scriptExecutor.stop()
        }
        if (::observerManager.isInitialized) {
            observerManager.clearAll()
        }
    }
    
    @Test
    fun `should execute observer actions when condition is met`() = runBlocking {
        val observerActionExecuted = AtomicBoolean(false)
        val mainActionExecuted = AtomicBoolean(false)
        
        every { actionExecutor.execute(match { it is Action.System.Wait }, null) } answers {
            val action = firstArg<Action.System.Wait>()
            if (action.milliseconds == 100L) {
                mainActionExecuted.set(true)
            } else if (action.milliseconds == 50L) {
                observerActionExecuted.set(true)
            }
            true
        }
        
        val observerConditionMet = AtomicBoolean(false)
        val observerIdentifier = ElementIdentifier.ByCoordinate(Coordinate(999, 999))
        
        every { elementFinder.find(observerIdentifier) } answers {
            if (observerConditionMet.get()) Coordinate(999, 999) else null
        }
        
        val conditionEvaluator = ConditionEvaluatorImpl(elementFinder)
        observerManager = ObserverManager(conditionEvaluator, checkDelayMs = 100)
        scriptExecutor = ScriptExecutor(actionExecutor, elementFinder, conditionEvaluator, observerManager)
        
        val observer = Step.ObserverBlock(
            id = StepId("observer-1"),
            label = "Test Observer",
            delayBefore = 0,
            delayAfter = 0,
            condition = Condition.ElementExists(observerIdentifier),
            actionSteps = listOf(
                Step.ActionStep(
                    id = StepId("observer-action-1"),
                    label = "Observer Action",
                    delayBefore = 0,
                    delayAfter = 0,
                    action = Action.System.Wait(50)
                )
            )
        )
        
        val script = Script(
            name = "Test Script with Observer",
            steps = listOf(
                observer,
                Step.ActionStep(
                    id = StepId("step-1"),
                    label = "Main Action",
                    delayBefore = 0,
                    delayAfter = 0,
                    action = Action.System.Wait(100)
                )
            )
        )
        
        scriptExecutor.start(script)
        
        delay(300)
        
        observerConditionMet.set(true)
        
        delay(1000)
        
        scriptExecutor.stop()
        
        assertTrue(observerActionExecuted.get(), "Observer action should have been executed")
        assertTrue(mainActionExecuted.get(), "Main action should have been executed")
    }
    
    @Test
    fun `should execute observer with correct priority`() = runBlocking {
        val executionOrder = mutableListOf<String>()
        
        every { actionExecutor.execute(any(), null) } answers {
            val action = firstArg<Action>()
            when (action) {
                is Action.System.Wait -> {
                    when (action.milliseconds) {
                        10L -> executionOrder.add("observer-action")
                        20L -> executionOrder.add("main-action")
                    }
                }
                else -> {}
            }
            true
        }
        
        val observerConditionMet = AtomicBoolean(true)
        val observerIdentifier = ElementIdentifier.ByCoordinate(Coordinate(888, 888))
        
        every { elementFinder.find(observerIdentifier) } answers {
            if (observerConditionMet.get()) {
                observerConditionMet.set(false)
                Coordinate(888, 888)
            } else {
                null
            }
        }
        
        val conditionEvaluator = ConditionEvaluatorImpl(elementFinder)
        observerManager = ObserverManager(conditionEvaluator, checkDelayMs = 50)
        scriptExecutor = ScriptExecutor(actionExecutor, elementFinder, conditionEvaluator, observerManager)
        
        val observer = Step.ObserverBlock(
            id = StepId("observer-1"),
            label = "Priority Observer",
            delayBefore = 0,
            delayAfter = 0,
            condition = Condition.ElementExists(observerIdentifier),
            actionSteps = listOf(
                Step.ActionStep(
                    id = StepId("observer-action"),
                    action = Action.System.Wait(10)
                )
            )
        )
        
        val script = Script(
            name = "Priority Test Script",
            steps = listOf(
                observer,
                Step.ActionStep(
                    id = StepId("main-action"),
                    action = Action.System.Wait(20)
                )
            )
        )
        
        scriptExecutor.start(script)
        
        delay(800)
        
        scriptExecutor.stop()
        
        assertTrue(executionOrder.contains("observer-action"), "Observer action should be executed")
        assertTrue(executionOrder.contains("main-action"), "Main action should be executed")
        
        val observerIndex = executionOrder.indexOf("observer-action")
        val mainIndex = executionOrder.lastIndexOf("main-action")
        assertTrue(observerIndex >= 0 && mainIndex > observerIndex, 
            "Observer should execute before completing main actions")
    }
    
    @Test
    fun `should not execute observer when condition is not met`() = runBlocking {
        val observerActionExecuted = AtomicBoolean(false)
        val mainActionExecuted = AtomicBoolean(false)
        
        every { actionExecutor.execute(any(), null) } answers {
            val action = firstArg<Action>()
            when (action) {
                is Action.System.Wait -> {
                    when (action.milliseconds) {
                        50L -> observerActionExecuted.set(true)
                        100L -> mainActionExecuted.set(true)
                    }
                }
                else -> {}
            }
            true
        }
        
        val observerIdentifier = ElementIdentifier.ByCoordinate(Coordinate(777, 777))
        every { elementFinder.find(observerIdentifier) } returns null
        
        val conditionEvaluator = ConditionEvaluatorImpl(elementFinder)
        observerManager = ObserverManager(conditionEvaluator, checkDelayMs = 100)
        scriptExecutor = ScriptExecutor(actionExecutor, elementFinder, conditionEvaluator, observerManager)
        
        val observer = Step.ObserverBlock(
            id = StepId("observer-1"),
            label = "Never Triggered Observer",
            delayBefore = 0,
            delayAfter = 0,
            condition = Condition.ElementExists(observerIdentifier),
            actionSteps = listOf(
                Step.ActionStep(
                    id = StepId("observer-action-1"),
                    action = Action.System.Wait(50)
                )
            )
        )
        
        val script = Script(
            name = "Test Script",
            steps = listOf(
                observer,
                Step.ActionStep(
                    id = StepId("step-1"),
                    action = Action.System.Wait(100)
                )
            )
        )
        
        scriptExecutor.start(script)
        
        delay(800)
        
        scriptExecutor.stop()
        
        assertTrue(!observerActionExecuted.get(), "Observer action should NOT have been executed")
        assertTrue(mainActionExecuted.get(), "Main action should have been executed")
    }
}

