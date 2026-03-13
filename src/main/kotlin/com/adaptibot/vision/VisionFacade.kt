package com.adaptibot.vision

import com.adaptibot.model.ImagePattern
import com.adaptibot.vision.domain.ElementFinder
import com.adaptibot.vision.dto.ElementLookupResult

class VisionFacade internal constructor(
    private val elementFinder: ElementFinder
) {

    fun findImage(ImagePattern: ImagePattern): ElementLookupResult =
        elementFinder.find(ImagePattern)

}