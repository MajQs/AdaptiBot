package com.adaptibot.recorder.dto

data class RecordingDto(
    val events: List<RecordedEventDto>,
    val completionReason: CompletionReason
)

enum class CompletionReason {
    USER_STOPPED,
    TIME_LIMIT_REACHED,
    CLICK_LIMIT_REACHED
}
