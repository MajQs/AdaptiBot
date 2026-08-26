package com.adaptibot.vision.domain

import com.adaptibot.infrastructure.ScreenCapture
import com.adaptibot.serialization.ImageEncoder
import com.adaptibot.vision.VisionQuery
import com.adaptibot.vision.adapter.OpenCvImageMatcher
import com.adaptibot.vision.dto.MatchDataDto

internal class VisionFinder(
    private val screenCapture: ScreenCapture,
    private val textMatcher: TextMatcher,
    private val imageMatcher: OpenCvImageMatcher,
) {

    fun find(query: VisionQuery): MatchDataDto? {
        val screenshot = screenCapture.captureFullScreen()
       return when (query) {
            is VisionQuery.ByImage ->  imageMatcher.match(screenshot, ImageEncoder.decodeFromBase64(query.pattern.base64Data))
            is VisionQuery.ByText -> textMatcher.match(screenshot, query.text)
        }
    }
}