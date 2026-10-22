package com.adaptibot.recorder.dto

sealed interface RecordedEventDto {

    data class MouseClick(
        val coordinate: CoordinateDto,
        val delayBeforeMillis: Long
    ) : RecordedEventDto
}

data class CoordinateDto(val x: Int, val y: Int)
