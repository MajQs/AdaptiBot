package com.adaptibot.vision.dto

import com.adaptibot.script.value.Coordinate

data class ImageMatchData(
    val coordinate: Coordinate,
    val confidence: Double
)