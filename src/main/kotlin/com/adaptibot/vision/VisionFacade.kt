package com.adaptibot.vision

import com.adaptibot.script.Coordinate
import com.adaptibot.script.ImagePattern
import com.adaptibot.script.PixelColor
import com.adaptibot.vision.domain.ImageFinder
import com.adaptibot.vision.domain.PixelColorReader
import com.adaptibot.vision.dto.ImageMatchData

/**
 * The module is responsible for **reading the visual state of the screen**:
 * locating UI elements by image pattern matching (using OpenCV template matching)
 * and sampling pixel colors at given coordinates.
 *
 * @see VisionConfiguration
 */
class VisionFacade internal constructor(
    private val elementFinder: ImageFinder,
    private val pixelColorReader: PixelColorReader
) {

    /**
     * Searches the current screenshot for the best match of the given image pattern.
     * @return [ImageMatchData] with the match coordinates and confidence score, or `null` if no match was found
     */
    fun getImageMatch(pattern: ImagePattern): ImageMatchData? =
        elementFinder.find(pattern)
            ?.let {
                ImageMatchData(
                    coordinate = it.coordinate,
                    confidence = it.confidence
                )
            }

    /**
     * Reads the RGBA color of the pixel at the given screen coordinates.
     */
    fun getPixelColor(point: Coordinate): PixelColor = pixelColorReader.read(point)
}
