package com.adaptibot.recorder.dto

sealed interface RecordingResultDto {

    data class Completed(val recording: RecordingDto) : RecordingResultDto

    data object Failed : RecordingResultDto
}
