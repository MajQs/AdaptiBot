package com.adaptibot.vision.dto

import com.adaptibot.model.Coordinate

sealed class ElementLookupResult {

    data class Found(
        val coordinate: Coordinate,
        val confidence: Double
    ) : ElementLookupResult()

    data class ImageNotFound(
        val bestConfidence: Double,
        val threshold: Double
    ) : ElementLookupResult()

}