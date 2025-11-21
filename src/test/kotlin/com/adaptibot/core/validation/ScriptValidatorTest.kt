package com.adaptibot.core.validation

import com.adaptibot.TestUtils
import com.adaptibot.common.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ScriptValidatorTest {
    
    @Test
    fun `valid script passes validation`() {
        val script = TestUtils.createTestScript()
        val result = ScriptValidator.validate(script)
        
        assertTrue(result.isValid, "Valid script should pass validation")
        assertTrue(result.errors.isEmpty(), "Should have no errors")
    }
    
    @Test
    fun `script without name fails validation`() {
        val script = Script(
            name = "",
            steps = emptyList()
        )
        
        val result = ScriptValidator.validate(script)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.field == "name" })
    }
    
    @Test
    fun `script with duplicate step IDs fails validation`() {
        val duplicateId = StepId("duplicate")
        val script = Script(
            name = "Test",
            steps = listOf(
                Step.ActionStep(
                    id = duplicateId,
                    action = Action.System.Wait(100)
                ),
                Step.ActionStep(
                    id = duplicateId,
                    action = Action.System.Wait(200)
                )
            )
        )
        
        val result = ScriptValidator.validate(script)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.message.contains("Duplicate step ID") })
    }
    
    @Test
    fun `script with invalid jump target fails validation`() {
        val script = Script(
            name = "Test",
            steps = listOf(
                Step.ActionStep(
                    id = StepId("step_1"),
                    action = Action.Flow.JumpTo(
                        targetStepId = StepId("non_existent")
                    )
                )
            )
        )
        
        val result = ScriptValidator.validate(script)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.message.contains("Jump target step not found") })
    }
}

