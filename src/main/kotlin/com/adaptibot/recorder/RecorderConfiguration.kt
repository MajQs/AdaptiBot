package com.adaptibot.recorder

import com.adaptibot.recorder.adapter.winapi.WindowsInputCapture
import com.adaptibot.recorder.domain.InputCapture
import com.adaptibot.recorder.domain.Recorder

object RecorderConfiguration {

    fun getFacade(): RecorderFacade = create(WindowsInputCapture())

    internal fun create(inputCapture: InputCapture): RecorderFacade =
        RecorderFacade(Recorder(inputCapture))

}
