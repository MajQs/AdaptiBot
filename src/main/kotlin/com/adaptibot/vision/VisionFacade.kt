package com.adaptibot.vision

import com.adaptibot.vision.domain.VisionFinder
import com.adaptibot.vision.dto.MatchDataDto

/**
 * The module is responsible for **reading the visual state of the screen**:
 * locating UI elements by image pattern matching (using OpenCV template matching)
 * and recognising text via OCR.
 *
 * @see VisionConfiguration
 */
class VisionFacade internal constructor(
    private val visionFinder: VisionFinder,
) {

    /**
     * Searches the current screenshot for the given [query].
     * @return [MatchDataDto] with the match coordinates and confidence score, or `null` if not found.
     */
    fun find(query: VisionQuery): MatchDataDto? = visionFinder.find(query)
}
