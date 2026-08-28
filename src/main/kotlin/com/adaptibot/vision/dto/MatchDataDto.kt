package com.adaptibot.vision.dto

import com.adaptibot.script.value.Coordinate

data class MatchDataDto(
    val coordinate: Coordinate,
    val confidence: Double,
    val width: Int,
    val height: Int
)

