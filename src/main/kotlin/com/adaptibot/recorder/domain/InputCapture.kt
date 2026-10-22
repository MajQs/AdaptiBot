package com.adaptibot.recorder.domain

/** Thrown by [InputCapture.start] when native capture cannot be started. */
internal class InputCaptureException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Outbound port: a source of global physical input events. Implemented by a platform adapter
 * (Windows low-level mouse hook).
 *
 * Contract:
 * - [start] begins capture and returns once capture is active; throws [InputCaptureException] on failure.
 * - [onEvent] is invoked on the adapter's own thread; it must be short and non-blocking.
 * - [onFailure] is invoked at most once if capture dies after a successful start.
 * - [stop] releases all native resources and returns only after cleanup is finished (no more callbacks
 *   after it returns). Idempotent, safe before [start], and safe to call from [onEvent]/[onFailure].
 */
internal interface InputCapture {

    @Throws(InputCaptureException::class)
    fun start(onEvent: (RecordedEvent) -> Unit, onFailure: (Throwable) -> Unit)

    fun stop()
}
