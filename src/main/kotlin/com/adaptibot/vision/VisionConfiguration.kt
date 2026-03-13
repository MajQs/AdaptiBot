package com.adaptibot.vision

import com.adaptibot.infrastructure.ScreenCapture
import com.adaptibot.vision.domain.ImageFinder
import com.adaptibot.vision.domain.ImageMatcher
import com.adaptibot.vision.domain.PixelColorReader

object VisionConfiguration {

    fun getVisionFacade(): VisionFacade {
        return VisionFacade(
            elementFinder = ImageFinder(
                screenCapture = ScreenCapture,
                imageMatcher = ImageMatcher()
            ),
            pixelColorReader = PixelColorReader()
        )
    }
}
