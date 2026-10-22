package com.adaptibot.recorder

import com.adaptibot.recorder.domain.Recorder
import com.adaptibot.recorder.dto.RecordingResultDto

/**
 * The module is responsible for **recording global physical mouse clicks**.
 *
 * Owns native capture resources — [close] must be called on application shutdown.
 *
 * @see RecorderConfiguration
 */
class RecorderFacade internal constructor(
    private val recorder: Recorder
) : AutoCloseable {

    /**
     * Starts a recording session.
     *
     * @param onCompleted invoked exactly once, on a background thread, with the session outcome;
     *        dispatching to the UI thread is the caller's responsibility
     * @return `false` when a session is already active
     */
    fun start(onCompleted: (RecordingResultDto) -> Unit): Boolean = recorder.start(onCompleted)

    /** Requests the active session to finish with the recorded data. No-op when not recording. */
    fun stop() = recorder.stop()

    fun isRecording(): Boolean = recorder.isRecording()

    /** Discards any active session (delivering `Failed`) and releases native resources. Idempotent. */
    override fun close() = recorder.close()
}
