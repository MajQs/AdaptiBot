package com.adaptibot.execution

import com.adaptibot.model.Script
import com.adaptibot.execution.domain.ScriptRunner
import com.adaptibot.execution.dto.ExecutionStateDto

/**
 * The module is responsible for **executing automation scripts** step by step:
 * it triggers actions on the user interface, evaluates visual conditions on the screen,
 * and reacts to asynchronous observer events.
 * Only one execution session can be active at a time.
 *
 * @throws RuntimeException when a script is started while another session is already active.
 * @see ExecutionConfiguration
 */
class ExecutionFacade internal constructor(
    private val scriptRunner: ScriptRunner
) {

    /**
     * Starts executing the given script in the background on a dedicated virtual thread.
     *
     * @throws RuntimeException when a session is already active (state other than IDLE)
     */
    fun startScript(script: Script) = scriptRunner.execute(script)

    /**
     * Stops the currently running session.
     * This method is idempotent — calling it when no session is active has no effect.
     */
    fun stopScript() = scriptRunner.stop()

    /**
     * Returns the current state of the execution session.
     */
    fun getExecutionState(): ExecutionStateDto = ExecutionStateDto.valueOf(scriptRunner.getExecutionState().name)
}
