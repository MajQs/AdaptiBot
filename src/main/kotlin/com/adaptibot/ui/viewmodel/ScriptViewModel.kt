package com.adaptibot.ui.viewmodel

import com.adaptibot.execution.ExecutionFacade
import com.adaptibot.execution.dto.ExecutionStateDto
import com.adaptibot.model.*
import com.adaptibot.serialization.ScriptSerializer
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import java.nio.file.Path

class ScriptViewModel(private val executionFacade: ExecutionFacade) {


    // Script metadata
    val scriptNameProperty = SimpleStringProperty("New Script")
    val scriptDescriptionProperty = SimpleStringProperty("")

    // Script settings
    val settingsProperty = SimpleObjectProperty(ScriptSettings())

    // Top-level steps
    val steps: ObservableList<Step> = FXCollections.observableArrayList()

    // Execution state
    val isRunningProperty = SimpleBooleanProperty(false)

    // Log messages
    val logMessages: ObservableList<LogMessage> = FXCollections.observableArrayList()

    // Currently active step (highlighted during execution)
    val activeStepIdProperty = SimpleObjectProperty<StepId?>(null)

    // Dirty flag – unsaved changes
    val isDirtyProperty = SimpleBooleanProperty(false)

    // Current file path
    var currentFilePath: Path? = null

    // ── Script operations ──────────────────────────────────────────────────────

    fun newScript() {
        steps.clear()
        scriptNameProperty.set("New Script")
        scriptDescriptionProperty.set("")
        settingsProperty.set(ScriptSettings())
        logMessages.clear()
        currentFilePath = null
        isDirtyProperty.set(false)
    }

    fun saveScript(path: Path) {
        val script = buildScript()
        ScriptSerializer.saveToFile(script, path)
        currentFilePath = path
        isDirtyProperty.set(false)
        addLog(LogMessage.info("Script saved to ${path.fileName}"))
    }

    fun loadScript(path: Path) {
        val script = ScriptSerializer.loadFromFile(path)
        steps.setAll(script.steps)
        scriptNameProperty.set(script.name)
        scriptDescriptionProperty.set(script.description)
        settingsProperty.set(script.settings)
        logMessages.clear()
        currentFilePath = path
        isDirtyProperty.set(false)
        addLog(LogMessage.info("Script loaded from ${path.fileName}"))
    }

    private fun buildScript(): Script = Script(
        name = scriptNameProperty.get(),
        description = scriptDescriptionProperty.get(),
        steps = steps.toList(),
        settings = settingsProperty.get()
    )

    // ── Execution ──────────────────────────────────────────────────────────────

    fun startExecution() {
        if (isRunningProperty.get()) return
        isRunningProperty.set(true)
        addLog(LogMessage.info("▶ Execution started"))
        val script = buildScript()

        // Launch script on a background thread (startScript blocks until done/stopped)
        Thread({
            try {
                executionFacade.startScript(script)
            } catch (e: Exception) {
                Platform.runLater {
                    addLog(LogMessage.error("Execution error: ${e.message}"))
                }
            }
        }, "script-execution").also { it.isDaemon = true; it.start() }

        // Monitor thread: polls backend state every 200ms and syncs isRunningProperty
        Thread({
            // Wait briefly so the backend has time to transition to RUNNING state
            Thread.sleep(500)
            while (true) {
                Thread.sleep(200)
                val state = executionFacade.getExecutionState()
                if (state != ExecutionStateDto.RUNNING) {
                    Platform.runLater {
                        isRunningProperty.set(false)
                        activeStepIdProperty.set(null)
                        addLog(LogMessage.info("⏹ Execution finished (${state.name.lowercase()})"))
                    }
                    break
                }
            }
        }, "script-state-monitor").also { it.isDaemon = true; it.start() }
    }

    fun stopExecution() {
        executionFacade.stopScript()
        // isRunningProperty will be set to false by the monitor thread
        addLog(LogMessage.info("⏹ Stop requested…"))
    }

    // ── Step tree mutation helpers ─────────────────────────────────────────────

    /** Adds [step] to the top-level list. */
    fun addStep(step: Step) {
        steps.add(step)
        isDirtyProperty.set(true)
    }

    /**
     * Inserts [step] immediately after the step identified by [afterStepId]
     * in the same parent list. Falls back to appending at the root level
     * if [afterStepId] is not found.
     */
    fun addStepAfter(afterStepId: StepId, step: Step): Boolean {
        val result = insertAfterInList(steps, afterStepId, step)
        if (result) isDirtyProperty.set(true) else {
            steps.add(step)
            isDirtyProperty.set(true)
        }
        return result
    }

    /** Adds [step] inside [parentId] block (to `steps` list). Returns true on success. */
    fun addStepToParent(parentId: StepId, step: Step): Boolean {
        val result = addToStepsInList(steps, parentId, step)
        if (result) isDirtyProperty.set(true)
        return result
    }

    /** Adds [step] to `elseSteps` list of the [parentId] [ConditionalBlock]. Returns true on success. */
    fun addStepToElse(parentId: StepId, step: Step): Boolean {
        val result = addToElseInList(steps, parentId, step)
        if (result) isDirtyProperty.set(true)
        return result
    }

    /** Removes step with [id] from anywhere in the tree. */
    fun removeStep(id: StepId): Boolean {
        val result = removeFromList(steps, id)
        if (result) isDirtyProperty.set(true)
        return result
    }

    /** Replaces step with [updated] (same id). */
    fun updateStep(updated: Step): Boolean {
        val result = replaceInList(steps, updated)
        if (result) isDirtyProperty.set(true)
        return result
    }

    /**
     * Moves step with [stepId] to [targetParentId] (null = top-level) at [targetIndex].
     */
    fun moveStep(stepId: StepId, targetParentId: StepId?, targetIndex: Int): Boolean {
        val step = findStep(stepId) ?: return false
        if (!removeStep(stepId)) return false
        val result = insertStep(step, targetParentId, targetIndex)
        if (result) isDirtyProperty.set(true)
        return result
    }

    /**
     * Moves step with [stepId] into the [branch] of the [ConditionalBlock] identified by [parentId],
     * appending it at the end of that branch.
     */
    fun moveStepToBranch(stepId: StepId, parentId: StepId, branch: com.adaptibot.ui.view.ConditionalBranch): Boolean {
        val step = findStep(stepId) ?: return false
        if (!removeStep(stepId)) return false
        val result = if (branch == com.adaptibot.ui.view.ConditionalBranch.ELSE)
            addStepToElse(parentId, step)
        else
            addStepToParent(parentId, step)
        if (result) isDirtyProperty.set(true)
        return result
    }

    fun findStep(id: StepId): Step? = findInList(steps, id)

    // ── Logging ───────────────────────────────────────────────────────────────

    fun addLog(message: LogMessage) {
        if (Platform.isFxApplicationThread()) {
            logMessages.add(message)
            if (logMessages.size > 1000) logMessages.removeAt(0)
        } else {
            Platform.runLater {
                logMessages.add(message)
                if (logMessages.size > 1000) logMessages.removeAt(0)
            }
        }
    }

    fun onStepExecuted(stepName: String, durationMs: Long) {
        activeStepIdProperty.set(null)
        addLog(LogMessage.success("✔ $stepName  (${durationMs}ms)"))
    }

    fun onStepFailed(stepName: String, durationMs: Long, error: String) {
        addLog(LogMessage.error("✖ $stepName  (${durationMs}ms) — $error"))
    }

    fun onExecutionStart(scriptName: String) {
        addLog(LogMessage.info("▶ Running: $scriptName"))
    }

    fun onExecutionStop() {
        Platform.runLater {
            isRunningProperty.set(false)
            activeStepIdProperty.set(null)
            addLog(LogMessage.info("⏹ Finished"))
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun removeFromList(list: ObservableList<Step>, id: StepId): Boolean {
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) {
            list.removeAt(idx)
            return true
        }
        for (i in list.indices) {
            when (val step = list[i]) {
                is ConditionalBlock -> {
                    val nested = FXCollections.observableArrayList(step.steps)
                    if (removeFromList(nested, id)) {
                        list[i] = step.copy(steps = nested.toList())
                        return true
                    }
                    val nestedElse = FXCollections.observableArrayList(step.elseSteps)
                    if (removeFromList(nestedElse, id)) {
                        list[i] = step.copy(elseSteps = nestedElse.toList())
                        return true
                    }
                }
                is BlockStep -> {
                    val nested = FXCollections.observableArrayList(step.steps)
                    if (removeFromList(nested, id)) {
                        list[i] = step.withSteps(nested.toList())
                        return true
                    }
                }
                is ObserverStep -> {
                    val nested = FXCollections.observableArrayList(step.steps)
                    if (removeFromList(nested, id)) {
                        list[i] = step.copy(steps = nested.toList())
                        return true
                    }
                }
                else -> {}
            }
        }
        return false
    }

    private fun replaceInList(list: ObservableList<Step>, updated: Step): Boolean {
        val idx = list.indexOfFirst { it.id == updated.id }
        if (idx >= 0) {
            list[idx] = updated
            return true
        }
        for (i in list.indices) {
            val step = list[i]
            when (step) {
                is ConditionalBlock -> {
                    val nested = FXCollections.observableArrayList(step.steps)
                    if (replaceInList(nested, updated)) {
                        list[i] = step.copy(steps = nested.toList())
                        return true
                    }
                    val nestedElse = FXCollections.observableArrayList(step.elseSteps)
                    if (replaceInList(nestedElse, updated)) {
                        list[i] = step.copy(elseSteps = nestedElse.toList())
                        return true
                    }
                }
                is BlockStep -> {
                    val nested = FXCollections.observableArrayList(step.steps)
                    if (replaceInList(nested, updated)) {
                        list[i] = step.withSteps(nested.toList())
                        return true
                    }
                }
                is ObserverStep -> {
                    val nested = FXCollections.observableArrayList(step.steps)
                    if (replaceInList(nested, updated)) {
                        list[i] = step.copy(steps = nested.toList())
                        return true
                    }
                }
                else -> {}
            }
        }
        return false
    }

    private fun findInList(list: List<Step>, id: StepId): Step? {
        for (step in list) {
            if (step.id == id) return step
            val nested: Step? = when (step) {
                is ConditionalBlock -> findInList(step.steps, id) ?: findInList(step.elseSteps, id)
                is BlockStep -> findInList(step.steps, id)
                is ObserverStep -> findInList(step.steps, id)
                else -> null
            }
            if (nested != null) return nested
        }
        return null
    }

    private fun insertStep(step: Step, parentId: StepId?, index: Int): Boolean {
        if (parentId == null) {
            val clamped = index.coerceIn(0, steps.size)
            steps.add(clamped, step)
            return true
        }
        return insertIntoParent(steps, step, parentId, index)
    }

    private fun insertIntoParent(
        list: ObservableList<Step>, step: Step, parentId: StepId, index: Int
    ): Boolean {
        for (i in list.indices) {
            val current = list[i]
            if (current.id == parentId) {
                when (current) {
                    is BlockStep -> {
                        val newList = current.steps.toMutableList()
                        newList.add(index.coerceIn(0, newList.size), step)
                        list[i] = current.withSteps(newList)
                        return true
                    }
                    is ObserverStep -> {
                        val newList = current.steps.toMutableList()
                        newList.add(index.coerceIn(0, newList.size), step)
                        list[i] = current.copy(steps = newList)
                        return true
                    }
                    else -> return false
                }
            }
            when (current) {
                is BlockStep -> {
                    val nested = FXCollections.observableArrayList(current.steps)
                    if (insertIntoParent(nested, step, parentId, index)) {
                        list[i] = current.withSteps(nested.toList())
                        return true
                    }
                }
                is ObserverStep -> {
                    val nested = FXCollections.observableArrayList(current.steps)
                    if (insertIntoParent(nested, step, parentId, index)) {
                        list[i] = current.copy(steps = nested.toList())
                        return true
                    }
                }
                else -> {}
            }
        }
        return false
    }

    private fun mutateSteps(
        list: ObservableList<Step>,
        action: (ObservableList<Step>, Int) -> Boolean
    ): Boolean {
        for (i in list.indices) {
            if (action(list, i)) return true
        }
        return false
    }

    private fun addToStepsInList(list: ObservableList<Step>, parentId: StepId, step: Step): Boolean {
        for (i in list.indices) {
            val current = list[i]
            // Found the target parent – append to its `steps`
            if (current.id == parentId) {
                when (current) {
                    is BlockStep    -> { list[i] = current.withSteps(current.steps + step); return true }
                    is ObserverStep -> { list[i] = current.copy(steps = current.steps + step); return true }
                    else            -> return false
                }
            }
            // Recurse into children
            when (current) {
                is ConditionalBlock -> {
                    val nested = FXCollections.observableArrayList(current.steps)
                    if (addToStepsInList(nested, parentId, step)) {
                        list[i] = current.copy(steps = nested.toList()); return true
                    }
                    val nestedElse = FXCollections.observableArrayList(current.elseSteps)
                    if (addToStepsInList(nestedElse, parentId, step)) {
                        list[i] = current.copy(elseSteps = nestedElse.toList()); return true
                    }
                }
                is BlockStep -> {
                    val nested = FXCollections.observableArrayList(current.steps)
                    if (addToStepsInList(nested, parentId, step)) {
                        list[i] = current.withSteps(nested.toList()); return true
                    }
                }
                is ObserverStep -> {
                    val nested = FXCollections.observableArrayList(current.steps)
                    if (addToStepsInList(nested, parentId, step)) {
                        list[i] = current.copy(steps = nested.toList()); return true
                    }
                }
                else -> {}
            }
        }
        return false
    }

    private fun addToElseInList(list: ObservableList<Step>, parentId: StepId, step: Step): Boolean {
        for (i in list.indices) {
            val current = list[i]
            if (current is ConditionalBlock && current.id == parentId) {
                list[i] = current.copy(elseSteps = current.elseSteps + step)
                return true
            }
            // recurse into children
            when (current) {
                is ConditionalBlock -> {
                    val nested = FXCollections.observableArrayList(current.steps)
                    if (addToElseInList(nested, parentId, step)) {
                        list[i] = current.copy(steps = nested.toList())
                        return true
                    }
                    val nestedElse = FXCollections.observableArrayList(current.elseSteps)
                    if (addToElseInList(nestedElse, parentId, step)) {
                        list[i] = current.copy(elseSteps = nestedElse.toList())
                        return true
                    }
                }
                is BlockStep -> {
                    val nested = FXCollections.observableArrayList(current.steps)
                    if (addToElseInList(nested, parentId, step)) {
                        list[i] = current.withSteps(nested.toList())
                        return true
                    }
                }
                is ObserverStep -> {
                    val nested = FXCollections.observableArrayList(current.steps)
                    if (addToElseInList(nested, parentId, step)) {
                        list[i] = current.copy(steps = nested.toList())
                        return true
                    }
                }
                else -> {}
            }
        }
        return false
    }

    /** Inserts [newStep] right after the step with [afterId] anywhere in the tree. */
    private fun insertAfterInList(list: ObservableList<Step>, afterId: StepId, newStep: Step): Boolean {
        val idx = list.indexOfFirst { it.id == afterId }
        if (idx >= 0) {
            list.add(idx + 1, newStep)
            return true
        }
        for (i in list.indices) {
            when (val current = list[i]) {
                is ConditionalBlock -> {
                    val nested = FXCollections.observableArrayList(current.steps)
                    if (insertAfterInList(nested, afterId, newStep)) {
                        list[i] = current.copy(steps = nested.toList())
                        return true
                    }
                    val nestedElse = FXCollections.observableArrayList(current.elseSteps)
                    if (insertAfterInList(nestedElse, afterId, newStep)) {
                        list[i] = current.copy(elseSteps = nestedElse.toList())
                        return true
                    }
                }
                is BlockStep -> {
                    val nested = FXCollections.observableArrayList(current.steps)
                    if (insertAfterInList(nested, afterId, newStep)) {
                        list[i] = current.withSteps(nested.toList())
                        return true
                    }
                }
                is ObserverStep -> {
                    val nested = FXCollections.observableArrayList(current.steps)
                    if (insertAfterInList(nested, afterId, newStep)) {
                        list[i] = current.copy(steps = nested.toList())
                        return true
                    }
                }
                else -> {}
            }
        }
        return false
    }
}

// Extension to clone BlockStep with new children
private fun BlockStep.withSteps(newSteps: List<Step>): BlockStep = when (this) {
    is GroupBlock -> copy(steps = newSteps)
    is ConditionalBlock -> copy(steps = newSteps)
}

