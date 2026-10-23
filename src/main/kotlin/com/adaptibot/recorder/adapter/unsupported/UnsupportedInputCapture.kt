package com.adaptibot.recorder.adapter.unsupported

import com.adaptibot.recorder.domain.InputCapture
import com.adaptibot.recorder.domain.InputCaptureException
import com.adaptibot.recorder.domain.RecordedEvent

internal class UnsupportedInputCapture : InputCapture {

    override fun start(onEvent: (RecordedEvent) -> Unit, onFailure: (Throwable) -> Unit) {
        throw InputCaptureException(RECORDING_UNAVAILABLE_REASON)
    }

    override fun stop() = Unit

    private companion object {
        const val RECORDING_UNAVAILABLE_REASON = "Global input recording is unavailable on this platform."
    }
}
