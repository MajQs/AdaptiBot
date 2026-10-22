package com.adaptibot.recorder.dto

sealed interface RecordedEventDto {

    /**
     * A left click in logical AWT screen coordinates; [delayBeforeMillis] is the pause since the
     * previous event (0 for the first event).
     */
    data class MouseClick(
        val coordinate: CoordinateDto,
        val delayBeforeMillis: Long
    ) : RecordedEventDto
}

/** Logical screen coordinate in the virtual desktop (may be negative on multi-monitor setups). */
data class CoordinateDto(val x: Int, val y: Int)
