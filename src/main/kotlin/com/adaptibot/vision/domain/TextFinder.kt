package com.adaptibot.vision.domain

import com.adaptibot.infrastructure.ScreenCapture
import com.adaptibot.vision.adapter.TesseractTextRecognizer
import com.adaptibot.vision.dto.MatchDataDto

internal class TextFinder(
    private val screenCapture: ScreenCapture,
    private val textRecognizer: TesseractTextRecognizer,
) {
    fun find(text: String): MatchDataDto? {
        val screenshot = screenCapture.captureFullScreen()
        return textRecognizer.findText(screenshot, text)
    }
}
