package com.adaptibot.recorder

import com.adaptibot.infrastructure.PlatformCapabilities
import com.adaptibot.infrastructure.PlatformCapabilityDetector
import com.adaptibot.recorder.adapter.unsupported.UnsupportedInputCapture
import com.adaptibot.recorder.adapter.winapi.WindowsInputCapture
import com.adaptibot.recorder.domain.InputCapture
import com.adaptibot.recorder.domain.Recorder

object RecorderConfiguration {

    fun getFacade(): RecorderFacade = getFacade(PlatformCapabilityDetector.detect())

    internal fun getFacade(capabilities: PlatformCapabilities): RecorderFacade =
        if (capabilities.recordingSupported) {
            create(WindowsInputCapture(), recordingAvailable = true)
        } else {
            create(UnsupportedInputCapture(), recordingAvailable = false)
        }

    internal fun create(
        inputCapture: InputCapture,
        recordingAvailable: Boolean = true
    ): RecorderFacade = RecorderFacade(Recorder(inputCapture), recordingAvailable)

}
