package com.adaptibot.ui.service

import com.adaptibot.common.model.*
import com.adaptibot.serialization.json.ScriptSerializer
import com.adaptibot.ui.model.ContainerType
import javafx.stage.FileChooser
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths

class ScriptService {

    private val logger = LoggerFactory.getLogger(ScriptService::class.java)

    private var currentScript: Script = createEmptyScript()
    private var currentFile: File? = null
    private var hasUnsavedChanges = false

    fun createNewScript() {
        currentScript = createEmptyScript()
        currentFile = null
        hasUnsavedChanges = false
        logger.info("Created new script")
    }

    fun openScript(stage: Stage? = null): Boolean {
        val fileChooser = FileChooser().apply {
            title = "Open Script"
            extensionFilters.add(
                FileChooser.ExtensionFilter("AdaptiBot Scripts", "*.json")
            )
        }

        val file = fileChooser.showOpenDialog(stage) ?: return false

        return try {
            val path = Paths.get(file.absolutePath)
            currentScript = ScriptSerializer.loadFromFile(path)
            currentFile = file
            hasUnsavedChanges = false
            logger.info("Opened script: ${file.name}")
            true
        } catch (e: Exception) {
            logger.error("Failed to open script", e)
            false
        }
    }

    fun saveScript(stage: Stage? = null): Boolean {
        return if (currentFile != null) {
            saveToFile(currentFile!!)
        } else {
            saveScriptAs(stage)
        }
    }

    fun saveScriptAs(stage: Stage? = null): Boolean {
        val fileChooser = FileChooser().apply {
            title = "Save Script As"
            initialFileName = "${currentScript.name}.json"
            extensionFilters.add(
                FileChooser.ExtensionFilter("AdaptiBot Scripts", "*.json")
            )
        }

        val file = fileChooser.showSaveDialog(stage) ?: return false

        return saveToFile(file)
    }

    fun getCurrentScript(): Script = currentScript

    fun updateScript(script: Script) {
        currentScript = script
        hasUnsavedChanges = true
    }

    fun addStep(step: Step) {
        currentScript = currentScript.copy(steps = currentScript.steps + step)
        hasUnsavedChanges = true
        logger.debug("Added step: ${step.id.value}")
    }

    fun updateStep(stepId: StepId, updatedStep: Step) {
        val updatedSteps = replaceStepRecursively(currentScript.steps, stepId, updatedStep)
        currentScript = currentScript.copy(steps = updatedSteps)
        hasUnsavedChanges = true
        logger.debug("Updated step: ${stepId.value}")
    }

    fun deleteStep(stepId: StepId) {
        val updatedSteps = deleteStepRecursively(currentScript.steps, stepId)
        currentScript = currentScript.copy(steps = updatedSteps)
        hasUnsavedChanges = true
        logger.debug("Deleted step: ${stepId.value}")
    }

    fun addStepToGroup(groupId: StepId, step: Step) {
        val updatedSteps = addStepToGroupRecursively(currentScript.steps, groupId, step)
        currentScript = currentScript.copy(steps = updatedSteps)
        hasUnsavedChanges = true
        logger.debug("Added step ${step.id.value} to group ${groupId.value}")
    }

    fun addStepToContainer(
        containerId: StepId,
        containerType: ContainerType,
        step: Step,
        parentBlockId: StepId? = null
    ) {
        when (containerType) {
            ContainerType.ROOT -> {
                addStep(step)
            }
            ContainerType.GROUP_BLOCK -> {
                addStepToGroup(containerId, step)
            }
            ContainerType.CONDITIONAL_THEN -> {
                val actualId = parentBlockId ?: containerId
                val updatedSteps = addStepToConditionalBranchRecursively(
                    currentScript.steps, actualId, step, true
                )
                currentScript = currentScript.copy(steps = updatedSteps)
                hasUnsavedChanges = true
                logger.debug("Added step ${step.id.value} to THEN branch of ${actualId.value}")
            }
            ContainerType.CONDITIONAL_ELSE -> {
                val actualId = parentBlockId ?: containerId
                val updatedSteps = addStepToConditionalBranchRecursively(
                    currentScript.steps, actualId, step, false
                )
                currentScript = currentScript.copy(steps = updatedSteps)
                hasUnsavedChanges = true
                logger.debug("Added step ${step.id.value} to ELSE branch of ${actualId.value}")
            }
            ContainerType.OBSERVER_BLOCK -> {
                val updatedSteps = addStepToObserverRecursively(currentScript.steps, containerId, step)
                currentScript = currentScript.copy(steps = updatedSteps)
                hasUnsavedChanges = true
            }
            ContainerType.NONE -> {
                logger.warn("Cannot add step to non-container node")
            }
        }
    }

    fun moveStep(
        stepId: StepId,
        targetContainerId: StepId,
        targetContainerType: ContainerType,
        targetIndex: Int,
        parentBlockId: StepId? = null
    ) {
        val step = findStepById(currentScript.steps, stepId)
        if (step != null) {
            val stepsWithoutSource = deleteStepRecursively(currentScript.steps, stepId)
            val updatedSteps = insertStepAtPosition(
                stepsWithoutSource,
                targetContainerId,
                targetContainerType,
                step,
                targetIndex,
                parentBlockId
            )
            currentScript = currentScript.copy(steps = updatedSteps)
            hasUnsavedChanges = true
            logger.debug("Moved step ${stepId.value} to ${targetContainerType} at index $targetIndex")
        }
    }

    fun moveStepToGroup(stepId: StepId, targetGroupId: StepId) {
        val step = findStepById(currentScript.steps, stepId)
        if (step != null) {
            val stepsWithoutSource = deleteStepRecursively(currentScript.steps, stepId)
            val updatedSteps = addStepToGroupRecursively(stepsWithoutSource, targetGroupId, step)
            currentScript = currentScript.copy(steps = updatedSteps)
            hasUnsavedChanges = true
            logger.debug("Moved step ${stepId.value} to group ${targetGroupId.value}")
        } else {
            logger.warn("Step ${stepId.value} not found for moving")
        }
    }

    fun copyStep(stepId: StepId): Step? {
        return findStepById(currentScript.steps, stepId)
    }

    fun pasteStep(step: Step, targetGroupId: StepId? = null) {
        val newStep = regenerateStepIds(step)
        if (targetGroupId != null) {
            addStepToGroup(targetGroupId, newStep)
        } else {
            addStep(newStep)
        }
        logger.debug("Pasted step with new id: ${newStep.id.value}")
    }

    private fun replaceStepRecursively(steps: List<Step>, targetId: StepId, newStep: Step): List<Step> {
        return steps.map { step ->
            if (step.id == targetId) {
                newStep
            } else {
                when (step) {
                    is ConditionalBlock -> step.copy(
                        thenSteps = replaceStepRecursively(step.thenSteps, targetId, newStep),
                        elseSteps = replaceStepRecursively(step.elseSteps, targetId, newStep)
                    )
                    is ObserverStep -> step.copy(
                        actionSteps = replaceStepRecursively(step.actionSteps, targetId, newStep)
                    )
                    is GroupBlock -> step.copy(
                        steps = replaceStepRecursively(step.steps, targetId, newStep)
                    )
                    else -> step
                }
            }
        }
    }

    private fun deleteStepRecursively(steps: List<Step>, targetId: StepId): List<Step> {
        return steps.filter { it.id != targetId }.map { step ->
            when (step) {
                is ConditionalBlock -> step.copy(
                    thenSteps = deleteStepRecursively(step.thenSteps, targetId),
                    elseSteps = deleteStepRecursively(step.elseSteps, targetId)
                )
                is ObserverStep -> step.copy(
                    actionSteps = deleteStepRecursively(step.actionSteps, targetId)
                )
                is GroupBlock -> step.copy(
                    steps = deleteStepRecursively(step.steps, targetId)
                )
                else -> step
            }
        }
    }

    fun hasUnsavedChanges(): Boolean = hasUnsavedChanges

    private fun saveToFile(file: File): Boolean {
        return try {
            val path = Paths.get(file.absolutePath)
            ScriptSerializer.saveToFile(currentScript, path)
            currentFile = file
            hasUnsavedChanges = false
            logger.info("Saved script: ${file.name}")
            true
        } catch (e: Exception) {
            logger.error("Failed to save script", e)
            false
        }
    }

    private fun addStepToGroupRecursively(steps: List<Step>, groupId: StepId, newStep: Step): List<Step> {
        return steps.map { step ->
            when (step) {
                is GroupBlock -> {
                    if (step.id == groupId) {
                        step.copy(steps = step.steps + newStep)
                    } else {
                        step.copy(steps = addStepToGroupRecursively(step.steps, groupId, newStep))
                    }
                }
                is ConditionalBlock -> step.copy(
                    thenSteps = addStepToGroupRecursively(step.thenSteps, groupId, newStep),
                    elseSteps = addStepToGroupRecursively(step.elseSteps, groupId, newStep)
                )
                is ObserverStep -> step.copy(
                    actionSteps = addStepToGroupRecursively(step.actionSteps, groupId, newStep)
                )
                else -> step
            }
        }
    }

    private fun findStepById(steps: List<Step>, targetId: StepId): Step? {
        for (step in steps) {
            if (step.id == targetId) {
                return step
            }
            val found = when (step) {
                is ConditionalBlock ->
                    findStepById(step.thenSteps, targetId) ?: findStepById(step.elseSteps, targetId)
                is ObserverStep ->
                    findStepById(step.actionSteps, targetId)
                is GroupBlock ->
                    findStepById(step.steps, targetId)
                else -> null
            }
            if (found != null) return found
        }
        return null
    }

    private fun regenerateStepIds(step: Step): Step {
        val newId = StepId("${step.id.value}_copy_${System.currentTimeMillis()}")
        return when (step) {
            is ActionStep ->
                step.copy(id = newId)
            is ConditionalBlock ->
                step.copy(
                    id = newId,
                    thenSteps = step.thenSteps.map { regenerateStepIds(it) },
                    elseSteps = step.elseSteps.map { regenerateStepIds(it) }
                )
            is ObserverStep ->
                step.copy(
                    id = newId,
                    actionSteps = step.actionSteps.map { regenerateStepIds(it) }
                )
            is GroupBlock ->
                step.copy(
                    id = newId,
                    steps = step.steps.map { regenerateStepIds(it) }
                )
            else -> step
        }
    }

    private fun addStepToConditionalBranchRecursively(
        steps: List<Step>,
        parentConditionalId: StepId,
        newStep: Step,
        isThenBranch: Boolean
    ): List<Step> {
        return steps.map { step ->
            when (step) {
                is ConditionalBlock -> {
                    if (step.id == parentConditionalId) {
                        if (isThenBranch) {
                            step.copy(thenSteps = step.thenSteps + newStep)
                        } else {
                            step.copy(elseSteps = step.elseSteps + newStep)
                        }
                    } else {
                        step.copy(
                            thenSteps = addStepToConditionalBranchRecursively(step.thenSteps, parentConditionalId, newStep, isThenBranch),
                            elseSteps = addStepToConditionalBranchRecursively(step.elseSteps, parentConditionalId, newStep, isThenBranch)
                        )
                    }
                }
                is ObserverStep -> step.copy(
                    actionSteps = addStepToConditionalBranchRecursively(step.actionSteps, parentConditionalId, newStep, isThenBranch)
                )
                is GroupBlock -> step.copy(
                    steps = addStepToConditionalBranchRecursively(step.steps, parentConditionalId, newStep, isThenBranch)
                )
                else -> step
            }
        }
    }

    private fun addStepToObserverRecursively(
        steps: List<Step>,
        observerId: StepId,
        newStep: Step
    ): List<Step> {
        return steps.map { step ->
            when (step) {
                is ObserverStep -> {
                    if (step.id == observerId) {
                        step.copy(actionSteps = step.actionSteps + newStep)
                    } else {
                        step.copy(actionSteps = addStepToObserverRecursively(step.actionSteps, observerId, newStep))
                    }
                }
                is ConditionalBlock -> step.copy(
                    thenSteps = addStepToObserverRecursively(step.thenSteps, observerId, newStep),
                    elseSteps = addStepToObserverRecursively(step.elseSteps, observerId, newStep)
                )
                is GroupBlock -> step.copy(
                    steps = addStepToObserverRecursively(step.steps, observerId, newStep)
                )
                else -> step
            }
        }
    }

    private fun insertStepAtPosition(
        steps: List<Step>,
        containerId: StepId,
        containerType: ContainerType,
        newStep: Step,
        targetIndex: Int,
        parentBlockId: StepId? = null
    ): List<Step> {
        if (containerType == ContainerType.ROOT && containerId.value == "root") {
            val mutableSteps = steps.toMutableList()
            val safeIndex = targetIndex.coerceIn(0, mutableSteps.size)
            mutableSteps.add(safeIndex, newStep)
            return mutableSteps
        }

        val actualContainerId = when (containerType) {
            ContainerType.CONDITIONAL_THEN,
            ContainerType.CONDITIONAL_ELSE -> parentBlockId ?: containerId
            else -> containerId
        }

        return steps.map { step ->
            when (step) {
                is GroupBlock -> {
                    if (step.id == actualContainerId && containerType == ContainerType.GROUP_BLOCK) {
                        val mutableSteps = step.steps.toMutableList()
                        val safeIndex = targetIndex.coerceIn(0, mutableSteps.size)
                        mutableSteps.add(safeIndex, newStep)
                        step.copy(steps = mutableSteps)
                    } else {
                        step.copy(steps = insertStepAtPosition(step.steps, containerId, containerType, newStep, targetIndex, parentBlockId))
                    }
                }
                is ConditionalBlock -> {
                    when {
                        step.id == actualContainerId && containerType == ContainerType.CONDITIONAL_THEN -> {
                            val mutableSteps = step.thenSteps.toMutableList()
                            val safeIndex = targetIndex.coerceIn(0, mutableSteps.size)
                            mutableSteps.add(safeIndex, newStep)
                            step.copy(thenSteps = mutableSteps)
                        }
                        step.id == actualContainerId && containerType == ContainerType.CONDITIONAL_ELSE -> {
                            val mutableSteps = step.elseSteps.toMutableList()
                            val safeIndex = targetIndex.coerceIn(0, mutableSteps.size)
                            mutableSteps.add(safeIndex, newStep)
                            step.copy(elseSteps = mutableSteps)
                        }
                        else -> step.copy(
                            thenSteps = insertStepAtPosition(step.thenSteps, containerId, containerType, newStep, targetIndex, parentBlockId),
                            elseSteps = insertStepAtPosition(step.elseSteps, containerId, containerType, newStep, targetIndex, parentBlockId)
                        )
                    }
                }
                is ObserverStep -> {
                    if (step.id == actualContainerId && containerType == ContainerType.OBSERVER_BLOCK) {
                        val mutableSteps = step.actionSteps.toMutableList()
                        val safeIndex = targetIndex.coerceIn(0, mutableSteps.size)
                        mutableSteps.add(safeIndex, newStep)
                        step.copy(actionSteps = mutableSteps)
                    } else {
                        step.copy(actionSteps = insertStepAtPosition(step.actionSteps, containerId, containerType, newStep, targetIndex, parentBlockId))
                    }
                }
                else -> step
            }
        }
    }

    private fun createEmptyScript(): Script {
        return Script(
            name = "New Script",
            description = "",
            steps = emptyList(),
            settings = ScriptSettings()
        )
    }
}
