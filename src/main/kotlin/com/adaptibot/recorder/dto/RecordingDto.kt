package com.adaptibot.recorder.dto

/** Ordered recorded [events] and the reason the recording ended. */
data class RecordingDto(
    val events: List<RecordedEventDto>,
    val completionReason: CompletionReason
)

enum class CompletionReason {
    USER_STOPPED,
    TIME_LIMIT_REACHED,
    CLICK_LIMIT_REACHED
}
