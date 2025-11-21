package com.adaptibot.core.validation

import com.adaptibot.common.model.Step
import com.adaptibot.serialization.image.ImageEncoder

object StepValidator {
    
    fun validate(step: Step): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        when (step) {
            is Step.ActionStep -> validateActionStep(step, errors)
            is Step.ConditionalBlock -> validateConditionalBlock(step, errors)
            is Step.ObserverBlock -> validateObserverBlock(step, errors)
            is Step.GroupBlock -> validateGroupBlock(step, errors)
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    private fun validateActionStep(step: Step.ActionStep, errors: MutableList<ValidationError>) {
        if (step.delayBefore < 0) {
            errors.add(ValidationError("Delay before must be non-negative", "delayBefore"))
        }
        if (step.delayAfter < 0) {
            errors.add(ValidationError("Delay after must be non-negative", "delayAfter"))
        }
    }
    
    private fun validateConditionalBlock(step: Step.ConditionalBlock, errors: MutableList<ValidationError>) {
        if (step.thenSteps.isEmpty() && step.elseSteps.isEmpty()) {
            errors.add(
                ValidationError(
                    "Conditional block must have at least one branch with steps",
                    "thenSteps/elseSteps",
                    ValidationError.Severity.WARNING
                )
            )
        }
        
        step.thenSteps.forEach { childStep ->
            validate(childStep).errors.forEach { errors.add(it) }
        }
        
        step.elseSteps.forEach { childStep ->
            validate(childStep).errors.forEach { errors.add(it) }
        }
    }
    
    private fun validateObserverBlock(step: Step.ObserverBlock, errors: MutableList<ValidationError>) {
        if (step.actionSteps.isEmpty()) {
            errors.add(
                ValidationError(
                    "Observer block must have at least one action step",
                    "actionSteps",
                    ValidationError.Severity.WARNING
                )
            )
        }
        
        step.actionSteps.forEach { childStep ->
            validate(childStep).errors.forEach { errors.add(it) }
        }
    }
    
    private fun validateGroupBlock(step: Step.GroupBlock, errors: MutableList<ValidationError>) {
        if (step.name.isBlank()) {
            errors.add(ValidationError("Group block must have a name", "name"))
        }
        
        if (step.steps.isEmpty()) {
            errors.add(
                ValidationError(
                    "Group block should contain at least one step",
                    "steps",
                    ValidationError.Severity.WARNING
                )
            )
        }
        
        step.steps.forEach { childStep ->
            validate(childStep).errors.forEach { errors.add(it) }
        }
    }
}

