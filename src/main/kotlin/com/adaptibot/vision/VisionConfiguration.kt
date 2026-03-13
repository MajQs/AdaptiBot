package com.adaptibot.vision

import com.adaptibot.infrastructure.ScreenCapture
import com.adaptibot.vision.domain.ElementFinder
import com.adaptibot.vision.domain.ImageMatcher

object VisionConfiguration {

    fun getVisionFacade(): VisionFacade {
        return VisionFacade(
            elementFinder = ElementFinder(
                screenCapture = ScreenCapture,
                imageMatcher = ImageMatcher()
            )
        )
    }
}
