package com.adaptibot.vision.domain

import com.adaptibot.infrastructure.ScreenCapture
import com.adaptibot.script.value.ImagePattern
import com.adaptibot.serialization.ImageEncoder
import com.adaptibot.vision.adapter.OpenCvImageMatcher
import com.adaptibot.vision.dto.MatchDataDto

internal class ImageFinder(
    private val screenCapture: ScreenCapture,
    private val imageMatcher: OpenCvImageMatcher,
) {
    fun find(imagePattern: ImagePattern): MatchDataDto? {
        val screenshot = screenCapture.captureFullScreen()
        val template = ImageEncoder.decodeFromBase64(imagePattern.base64Data)
        return imageMatcher.findBestMatch(screenshot, template)
    }
}