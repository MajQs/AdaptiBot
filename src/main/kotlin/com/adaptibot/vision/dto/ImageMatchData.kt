package com.adaptibot.vision.dto

import com.adaptibot.model.Coordinate

data class ImageMatchData(
    val coordinate: Coordinate,
    val confidence: Double
)