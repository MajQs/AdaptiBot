package com.adaptibot.vision

import com.adaptibot.model.ElementIdentifier
import com.adaptibot.vision.domain.ElementFinder
import com.adaptibot.vision.dto.ElementLookupResult

class VisionFacade internal constructor(
    private val elementFinder: ElementFinder
) {

    fun findElement(element: ElementIdentifier): ElementLookupResult =
        elementFinder.find(element)

}