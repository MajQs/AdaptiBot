package com.adaptibot.vision.domain

import com.adaptibot.infrastructure.ScreenCapture
import com.adaptibot.vision.dto.MatchDataDto

internal class TextFinder(
    private val screenCapture: ScreenCapture,
    private val textRecognizer: TextRecognizer,
) {
    fun find(text: String): MatchDataDto? {
        val screenshot = screenCapture.captureFullScreen()
        return textRecognizer.findText(screenshot, text)
    }
}
