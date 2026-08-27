package com.adaptibot.vision

import com.adaptibot.infrastructure.ScreenCapture
import com.adaptibot.vision.adapter.OpenCvImageMatcher
import com.adaptibot.vision.adapter.TesseractTextMatcher
import com.adaptibot.vision.domain.SearchAreaResolver
import com.adaptibot.vision.domain.VisionFinder

object VisionConfiguration {

    val visionFacade: VisionFacade by lazy {
        VisionFacade(
            visionFinder = VisionFinder(
                imageMatcher = OpenCvImageMatcher(),
                textMatcher = TesseractTextMatcher(),
                screenCapture = ScreenCapture,
                searchAreaResolver = SearchAreaResolver(ScreenCapture)
            )
        )
    }
}
