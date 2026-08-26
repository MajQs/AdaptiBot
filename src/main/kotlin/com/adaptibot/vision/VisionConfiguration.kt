package com.adaptibot.vision

import com.adaptibot.infrastructure.ScreenCapture
import com.adaptibot.vision.domain.ImageFinder
import com.adaptibot.vision.adapter.OpenCvImageMatcher
import com.adaptibot.vision.domain.TextFinder
import com.adaptibot.vision.adapter.TesseractTextRecognizer

object VisionConfiguration {

    val visionFacade: VisionFacade by lazy {
        VisionFacade(
            imageFinder = ImageFinder(
                screenCapture = ScreenCapture,
                imageMatcher = OpenCvImageMatcher()
            ),
            textFinder = TextFinder(
                screenCapture = ScreenCapture,
                textRecognizer = TesseractTextRecognizer()
            )
        )
    }
}
