package com.adaptibot.recorder.dto

sealed interface RecordedEventDto {

    /**
     * A physical left click at raw screen [coordinate]; [delayBeforeMillis] is the pause since the
     * previous event (0 for the first event).
     */
    data class MouseClick(
        val coordinate: CoordinateDto,
        val delayBeforeMillis: Long
    ) : RecordedEventDto
}

/** Raw screen coordinate in virtual-desktop pixels (may be negative on multi-monitor setups). */
data class CoordinateDto(val x: Int, val y: Int)
