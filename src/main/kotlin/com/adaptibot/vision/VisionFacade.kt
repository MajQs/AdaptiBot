package com.adaptibot.vision

import com.adaptibot.model.Coordinate
import com.adaptibot.model.ImagePattern
import com.adaptibot.model.PixelColor
import com.adaptibot.vision.domain.ImageFinder
import com.adaptibot.vision.domain.PixelColorReader
import com.adaptibot.vision.dto.ImageMatchData

class VisionFacade internal constructor(
    private val elementFinder: ImageFinder,
    private val pixelColorReader: PixelColorReader
) {

    fun getImageMatch(pattern: ImagePattern): ImageMatchData? =
        elementFinder.find(pattern)
            ?.let {
                ImageMatchData(
                    coordinate = it.coordinate,
                    confidence = it.confidence
                )
            }

    fun getPixelColor(point: Coordinate): PixelColor = pixelColorReader.read(point)
}
