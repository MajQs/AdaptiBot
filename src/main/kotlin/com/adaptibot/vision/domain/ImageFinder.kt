package com.adaptibot.vision.domain

import com.adaptibot.infrastructure.ScreenCapture
import com.adaptibot.script.value.ImagePattern
import com.adaptibot.serialization.ImageEncoder

internal class ImageFinder(
    private val screenCapture: ScreenCapture,
    private val imageMatcher: ImageMatcher,
) {


    fun find(imagePattern: ImagePattern): MatchResult? {
        val screenshot = screenCapture.captureFullScreen()
        val template = ImageEncoder.decodeFromBase64(imagePattern.base64Data)
        return imageMatcher.findBestMatch(screenshot, template)
    }
}