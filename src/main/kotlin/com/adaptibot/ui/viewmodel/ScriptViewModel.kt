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

    /** Adds [step] inside [parentId] block. Returns true on success. */
    fun addStepToParent(parentId: StepId, step: Step): Boolean {
        val result = mutateSteps(steps) { list, index ->
            val parent = list[index]
            if (parent.id == parentId && parent is BlockStep) {
                list[index] = parent.withSteps(parent.steps + step)
                true
            } else if (parent is ObserverStep && parent.id == parentId) {
                list[index] = parent.copy(steps = parent.steps + step)
                true
            } else false
        }
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
}

// Extension to clone BlockStep with new children
private fun BlockStep.withSteps(newSteps: List<Step>): BlockStep = when (this) {
    is GroupBlock -> copy(steps = newSteps)
    is ConditionalBlock -> copy(steps = newSteps)
}

