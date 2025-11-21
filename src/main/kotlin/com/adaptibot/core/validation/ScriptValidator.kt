package com.adaptibot.core.validation

import com.adaptibot.common.model.Action
import com.adaptibot.common.model.Script
import com.adaptibot.common.model.Step
import com.adaptibot.common.model.StepId

object ScriptValidator {
    
    fun validate(script: Script): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        if (script.name.isBlank()) {
            errors.add(ValidationError("Script must have a name", "name"))
        }
        
        if (script.steps.isEmpty()) {
            errors.add(
                ValidationError(
                    "Script should contain at least one step",
                    "steps",
                    ValidationError.Severity.WARNING
                )
            )
        }
        
        validateStepIds(script, errors)
        validateLabels(script, errors)
        validateJumpTargets(script, errors)
        
        script.steps.forEach { step ->
            StepValidator.validate(step).errors.forEach { errors.add(it) }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    private fun validateStepIds(script: Script, errors: MutableList<ValidationError>) {
        val stepIds = mutableSetOf<StepId>()
        
        fun collectStepIds(step: Step) {
            if (stepIds.contains(step.id)) {
                errors.add(ValidationError("Duplicate step ID: ${step.id.value}", "id"))
            } else {
                stepIds.add(step.id)
            }
            
            when (step) {
                is Step.ConditionalBlock -> {
                    step.thenSteps.forEach { collectStepIds(it) }
                    step.elseSteps.forEach { collectStepIds(it) }
                }
                is Step.ObserverBlock -> {
                    step.actionSteps.forEach { collectStepIds(it) }
                }
                is Step.GroupBlock -> {
                    step.steps.forEach { collectStepIds(it) }
                }
                is Step.ActionStep -> {}
            }
        }
        
        script.steps.forEach { collectStepIds(it) }
    }
    
    private fun validateLabels(script: Script, errors: MutableList<ValidationError>) {
        val labels = mutableMapOf<String, StepId>()
        
        fun collectLabels(step: Step) {
            step.label?.let { label ->
                if (label.isBlank()) {
                    errors.add(ValidationError("Label cannot be empty", "label"))
                } else if (labels.containsKey(label)) {
                    errors.add(ValidationError("Duplicate label: $label", "label"))
                } else {
                    labels[label] = step.id
                }
            }
            
            when (step) {
                is Step.ConditionalBlock -> {
                    step.thenSteps.forEach { collectLabels(it) }
                    step.elseSteps.forEach { collectLabels(it) }
                }
                is Step.ObserverBlock -> {
                    step.actionSteps.forEach { collectLabels(it) }
                }
                is Step.GroupBlock -> {
                    step.steps.forEach { collectLabels(it) }
                }
                is Step.ActionStep -> {}
            }
        }
        
        script.steps.forEach { collectLabels(it) }
    }
    
    private fun validateJumpTargets(script: Script, errors: MutableList<ValidationError>) {
        val allStepIds = mutableSetOf<StepId>()
        
        fun collectStepIds(step: Step) {
            allStepIds.add(step.id)
            when (step) {
                is Step.ConditionalBlock -> {
                    step.thenSteps.forEach { collectStepIds(it) }
                    step.elseSteps.forEach { collectStepIds(it) }
                }
                is Step.ObserverBlock -> {
                    step.actionSteps.forEach { collectStepIds(it) }
                }
                is Step.GroupBlock -> {
                    step.steps.forEach { collectStepIds(it) }
                }
                is Step.ActionStep -> {}
            }
        }
        
        fun validateJumps(step: Step) {
            when (step) {
                is Step.ActionStep -> {
                    if (step.action is Action.Flow.JumpTo) {
                        val targetId = step.action.targetStepId
                        if (!allStepIds.contains(targetId)) {
                            errors.add(
                                ValidationError(
                                    "Jump target step not found: ${targetId.value}",
                                    "targetStepId"
                                )
                            )
                        }
                    }
                }
                is Step.ConditionalBlock -> {
                    step.thenSteps.forEach { validateJumps(it) }
                    step.elseSteps.forEach { validateJumps(it) }
                }
                is Step.ObserverBlock -> {
                    step.actionSteps.forEach { validateJumps(it) }
                }
                is Step.GroupBlock -> {
                    step.steps.forEach { validateJumps(it) }
                }
            }
        }
        
        script.steps.forEach { collectStepIds(it) }
        script.steps.forEach { validateJumps(it) }
    }
}

