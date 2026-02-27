package com.adaptibot.vision

import com.adaptibot.vision.adapter.ScreenCapture
import com.adaptibot.vision.domain.ElementFinder
import com.adaptibot.vision.domain.ImageMatcher

object VisionConfiguration {

    fun getVisionFacade() = VisionFacade(
        elementFinder = ElementFinder(
            screenCapture = ScreenCapture,
            imageMatcher = ImageMatcher()
        )
    )

}

