package com.adaptibot.ui.viewmodel

import com.adaptibot.execution.ExecutionFacade
import com.adaptibot.execution.dto.ExecutionStateDto
import com.adaptibot.execution.dto.ObserverStatusDto
import com.adaptibot.script.*
import com.adaptibot.script.step.*
import com.adaptibot.serialization.ScriptSerializer
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import java.nio.file.Path

class ScriptViewModel(private val executionFacade: ExecutionFacade) {

    // Domain aggregate
    private var script: Script = Script.create("New Script")

    // Observable properties bound to UI
    val scriptNameProperty = SimpleStringProperty("New Script")
    val steps: ObservableList<Step> = FXCollections.observableArrayList()
    val isRunningProperty = SimpleBooleanProperty(false)
    val logMessages: ObservableList<LogMessage> = FXCollections.observableArrayList()
    val activeStepIdProperty = SimpleObjectProperty<StepId?>(null)

    /** Ids of observers currently watching, and of the one whose steps are running. */
    val armedObserverIdsProperty = SimpleObjectProperty<Set<String>>(emptySet())
    val handlingObserverIdProperty = SimpleObjectProperty<String?>(null)

    /** One-line summary shown while a script is running, empty when no observer is active. */
    val observerStatusTextProperty = SimpleStringProperty("")

    val isDirtyProperty = SimpleBooleanProperty(false)
    var currentFilePath: Path? = null

    init {
        syncStepsFromAggregate()
        scriptNameProperty.addListener { _, _, newName ->
            if (newName != script.name) script.rename(newName.ifBlank { "New Script" })
        }
    }

    // ── Script operations ──────────────────────────────────────────────────────

    fun newScript() {
        script = Script.create("New Script")
        scriptNameProperty.set(script.name)
        logMessages.clear()
        currentFilePath = null
        isDirtyProperty.set(false)
        syncStepsFromAggregate()
    }

    fun saveScript(path: Path) {
        ScriptSerializer.saveToFile(script, path)
        currentFilePath = path
        isDirtyProperty.set(false)
        addLog(LogMessage.info("Script saved to ${path.fileName}"))
        ScriptValidator.warnings(script).forEach { addLog(LogMessage.warning("⚠ $it")) }
    }

    fun loadScript(path: Path) {
        script = ScriptSerializer.loadFromFile(path)
        scriptNameProperty.set(script.name)
        logMessages.clear()
        currentFilePath = path
        isDirtyProperty.set(false)
        syncStepsFromAggregate()
        addLog(LogMessage.info("Script loaded from ${path.fileName}"))
    }

    // Script metadata accessors (read by UI)
    fun getScriptName(): String = script.name
    fun getScriptDescription(): String = script.description
    fun getScriptSettings(): ScriptSettings = script.settings

    fun renameScript(newName: String) {
        script.rename(newName)
        markDirty()
    }

    fun updateDescription(newDescription: String) {
        script.updateDescription(newDescription)
        markDirty()
    }

    // ── Execution ──────────────────────────────────────────────────────────────

    fun startExecution() {
        if (isRunningProperty.get()) return
        isRunningProperty.set(true)
        addLog(LogMessage.info("▶ Execution started"))

        Thread({
            try {
                executionFacade.startScript(script)
            } catch (e: Exception) {
                Platform.runLater { addLog(LogMessage.error("Execution error: ${e.message}")) }
            }
        }, "script-execution").also { it.isDaemon = true; it.start() }

        Thread({
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
        addLog(LogMessage.info("⏹ Stop requested…"))
    }

    fun stopExecutionByHotkey(shortcut: String) {
        if (!isRunningProperty.get()) return
        executionFacade.stopScript()
        addLog(LogMessage.info("⏹ Stop requested via $shortcut"))
    }

    // ── Step tree mutations ────────────────────────────────────────────────────

    fun addStepAfter(afterStepId: StepId, step: Step): Boolean {
        val result = script.addStepAfter(afterStepId, step)
        if (!result) script.addStep(script.rootContainerId, step)
        syncStepsFromAggregate()
        markDirty()
        return result
    }

    fun addStep(containerId: ContainerId, step: Step, index: Int = Int.MAX_VALUE): Boolean {
        val result = script.addStep(containerId, step, index)
        if (result) { syncStepsFromAggregate(); markDirty() }
        return result
    }

    fun removeStep(id: StepId): Boolean {
        val result = script.removeStep(id)
        if (result) { syncStepsFromAggregate(); markDirty() }
        return result
    }

    fun updateStep(updated: Step): Boolean {
        val result = script.updateStep(updated)
        if (result) { syncStepsFromAggregate(); markDirty() }
        return result
    }

    /** Deep-copies a step and inserts the clone directly after the original. */
    fun duplicateStep(id: StepId): Step? {
        val clone = script.duplicateStep(id)
        if (clone != null) { syncStepsFromAggregate(); markDirty() }
        return clone
    }

    fun toggleStep(id: StepId): Boolean {
        val step = script.findStep(id) ?: return false
        val toggled: Step = when (step) {
            is ActionStep      -> step.copy(enabled = !step.enabled)
            is GroupStep       -> step.copy(enabled = !step.enabled)
            is ObserverStep    -> step.copy(enabled = !step.enabled)
            is ConditionalStep -> step.copy(enabled = !step.enabled)
            is WhileStep       -> step.copy(enabled = !step.enabled)
            is ForStep         -> step.copy(enabled = !step.enabled)
        }
        return updateStep(toggled)
    }

    fun moveStep(stepId: StepId, targetContainerId: ContainerId, targetIndex: Int = Int.MAX_VALUE): Boolean {
        val result = script.moveStep(stepId, targetContainerId, targetIndex)
        if (result) { syncStepsFromAggregate(); markDirty() }
        return result
    }

    fun moveStepToContainer(stepId: StepId, containerId: ContainerId): Boolean {
        val result = script.moveStep(stepId, containerId)
        if (result) { syncStepsFromAggregate(); markDirty() }
        return result
    }


    /** The [ContainerId] of the script's root container – use this to add steps at the top level. */
    val rootContainerId: ContainerId get() = script.rootContainerId

    // ── Logging ────────────────────────────────────────────────────────────────

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

    /**
     * Observers are evaluated on a background thread, so the state is republished on the FX thread.
     * The summary makes it obvious which observers watch the screen at any moment - an observer
     * declared at the top of a looped script is effectively always on, and the user should see that.
     */
    fun onObserverStatusChanged(status: ObserverStatusDto) {
        Platform.runLater {
            armedObserverIdsProperty.set(status.armed.map { it.stepId }.toSet())
            handlingObserverIdProperty.set(status.handling?.stepId)

            val watching = status.armed.joinToString(", ") { it.label }
            observerStatusTextProperty.set(
                buildString {
                    if (watching.isNotEmpty()) append("👁 Watching: $watching")
                    status.handling?.let {
                        if (isNotEmpty()) append("   ·   ")
                        append("▶ Handling: ${it.label}")
                    }
                }
            )
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun syncStepsFromAggregate() {
        steps.setAll(script.steps)
    }

    private fun markDirty() {
        isDirtyProperty.set(true)
    }
}
