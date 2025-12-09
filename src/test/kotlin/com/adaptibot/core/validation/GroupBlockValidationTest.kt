package com.adaptibot.core.validation

import com.adaptibot.common.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class GroupBlockValidationTest {

    @Test
    fun `valid group block should pass validation`() {
        val groupBlock = Step.GroupBlock(
            id = StepId("group_1"),
            name = "Test Group",
            steps = listOf(
                Step.ActionStep(
                    id = StepId("step_1"),
                    action = Action.System.Wait(1000)
                )
            )
        )

        val result = StepValidator.validate(groupBlock)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `group block with blank name should fail validation`() {
        val groupBlock = Step.GroupBlock(
            id = StepId("group_1"),
            name = "",
            steps = listOf(
                Step.ActionStep(
                    id = StepId("step_1"),
                    action = Action.System.Wait(1000)
                )
            )
        )

        val result = StepValidator.validate(groupBlock)
        assertTrue(result is ValidationResult.Invalid)
        val errors = (result as ValidationResult.Invalid).errors
        assertTrue(errors.any { it.field == "name" })
    }

    @Test
    fun `empty group block should produce warning`() {
        val groupBlock = Step.GroupBlock(
            id = StepId("group_1"),
            name = "Empty Group",
            steps = emptyList()
        )

        val result = StepValidator.validate(groupBlock)
        assertTrue(result is ValidationResult.Invalid)
        val errors = (result as ValidationResult.Invalid).errors
        assertTrue(errors.any { it.field == "steps" && it.severity == ValidationError.Severity.WARNING })
    }

    @Test
    fun `nested group blocks should validate recursively`() {
        val innerGroup = Step.GroupBlock(
            id = StepId("inner_group"),
            name = "Inner Group",
            steps = listOf(
                Step.ActionStep(
                    id = StepId("step_1"),
                    action = Action.System.Wait(1000)
                )
            )
        )

        val outerGroup = Step.GroupBlock(
            id = StepId("outer_group"),
            name = "Outer Group",
            steps = listOf(innerGroup)
        )

        val result = StepValidator.validate(outerGroup)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `group block with invalid child step should fail validation`() {
        val groupBlock = Step.GroupBlock(
            id = StepId("group_1"),
            name = "Test Group",
            steps = listOf(
                Step.ActionStep(
                    id = StepId("step_1"),
                    delayBefore = -100, // Invalid negative delay
                    action = Action.System.Wait(1000)
                )
            )
        )

        val result = StepValidator.validate(groupBlock)
        assertTrue(result is ValidationResult.Invalid)
        val errors = (result as ValidationResult.Invalid).errors
        assertTrue(errors.any { it.field == "delayBefore" })
    }
}


