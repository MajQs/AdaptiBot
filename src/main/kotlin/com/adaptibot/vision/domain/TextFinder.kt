package com.adaptibot.vision.domain

import com.adaptibot.infrastructure.ScreenCapture

internal class TextFinder(
    private val screenCapture: ScreenCapture,
    private val textRecognizer: TextRecognizer,
) {

    fun find(text: String): MatchResult? {
        val screenshot = screenCapture.captureFullScreen()
        return textRecognizer.findText(screenshot, text)
    }
}

