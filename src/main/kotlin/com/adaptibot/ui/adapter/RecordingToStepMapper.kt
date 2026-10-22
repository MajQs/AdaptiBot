package com.adaptibot.ui.adapter

import com.adaptibot.recorder.dto.RecordedEventDto
import com.adaptibot.recorder.dto.RecordingDto
import com.adaptibot.script.step.ActionStep
import com.adaptibot.script.step.GroupStep
import com.adaptibot.script.step.StepContainer
import com.adaptibot.script.value.Action
import com.adaptibot.script.value.Coordinate
import com.adaptibot.script.value.MouseButton
import com.adaptibot.script.value.MouseClickType
import com.adaptibot.script.value.Target

/** Pure mapping from a completed recording to one "Recording" group of left single clicks. */
internal object RecordingToStepMapper {

    private const val GROUP_LABEL = "Recording"
    private const val CLICK_HOLD_MILLIS = 100L

    fun toGroupStep(recording: RecordingDto): GroupStep = GroupStep(
        label = GROUP_LABEL,
        container = StepContainer(steps = recording.events.map { it.toActionStep() })
    )

    private fun RecordedEventDto.toActionStep(): ActionStep = when (this) {
        is RecordedEventDto.MouseClick -> ActionStep(
            action = Action.Mouse.Click(
                delayBefore = delayBeforeMillis,
                target = Target.AtCoordinate(Coordinate(x = coordinate.x, y = coordinate.y)),
                button = MouseButton.LEFT,
                type = MouseClickType.SINGLE,
                holdDuration = CLICK_HOLD_MILLIS
            )
        )
    }
}
