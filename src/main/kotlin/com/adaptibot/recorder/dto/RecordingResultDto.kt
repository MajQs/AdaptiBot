package com.adaptibot.recorder.dto

/** Outcome of one recording session, delivered exactly once to the `onCompleted` callback. */
sealed interface RecordingResultDto {

    /** Recording ended with data (possibly empty) that the caller may use. */
    data class Completed(val recording: RecordingDto) : RecordingResultDto

    /** Recording failed or was discarded; partial data is never exposed. Single user-facing outcome. */
    data object Failed : RecordingResultDto
}
