package com.adaptibot.core.executor

import com.adaptibot.common.model.Script

/**
 * Main interface for script execution engine.
 * Manages script lifecycle, infinite loop execution, and observer threads.
 */
interface IScriptExecutor {
    fun start(script: Script)
    fun pause()
    fun resume()
    fun stop()
    fun getState(): ExecutionState
    fun getContext(): ExecutionContext
}

