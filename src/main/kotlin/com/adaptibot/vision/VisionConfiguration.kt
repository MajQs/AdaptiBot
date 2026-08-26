package com.adaptibot.vision

import com.adaptibot.infrastructure.ScreenCapture
import com.adaptibot.vision.domain.ImageFinder
import com.adaptibot.vision.domain.ImageMatcher
import com.adaptibot.vision.domain.TextFinder
import com.adaptibot.vision.domain.TextRecognizer

object VisionConfiguration {

    val visionFacade: VisionFacade by lazy {
        VisionFacade(
            imageFinder = ImageFinder(
                screenCapture = ScreenCapture,
                imageMatcher = ImageMatcher()
            ),
            textFinder = TextFinder(
                screenCapture = ScreenCapture,
                textRecognizer = TextRecognizer()
            )
        )
    }
}
