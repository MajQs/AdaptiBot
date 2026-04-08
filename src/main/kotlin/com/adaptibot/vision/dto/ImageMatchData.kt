package com.adaptibot.vision.dto

import com.adaptibot.script.Coordinate

data class ImageMatchData(
    val coordinate: Coordinate,
    val confidence: Double
)