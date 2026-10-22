package com.adaptibot.recorder.domain

internal data class ScreenPoint(val x: Int, val y: Int)

internal sealed interface RecordedEvent {

    /**
     * Accepted physical left-button-down. [nativeTimestampMillis] is the unsigned 32-bit
     * `MSLLHOOKSTRUCT.time` value (0..0xFFFFFFFF), which wraps around.
     */
    data class MouseClick(
        val point: ScreenPoint,
        val nativeTimestampMillis: Long
    ) : RecordedEvent
}
