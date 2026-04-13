package com.adaptibot

import com.adaptibot.script.value.Action
import com.adaptibot.script.step.ActionStep
import com.adaptibot.script.step.ConditionalStep
import com.adaptibot.script.step.GroupStep
import com.adaptibot.script.step.StepContainer
import com.adaptibot.script.value.Condition
import com.adaptibot.script.value.Coordinate
import com.adaptibot.script.value.ImagePattern
import com.adaptibot.script.value.KeyboardKey
import com.adaptibot.script.value.Target
import com.adaptibot.script.Script
import com.adaptibot.script.ScriptId
import com.adaptibot.script.ScriptSettings
import com.adaptibot.script.step.Step
import com.adaptibot.script.value.VisualMatcher

object TestUtils {

    fun createTestScript(name: String = "Test Script"): Script {
        return Script.restore(
            id = ScriptId(),
            name = name,
            description = "Test script for development",
            rootContainer = StepContainer(steps = createSampleSteps().toMutableList()),
            settings = ScriptSettings(
                defaultDelayBefore = 100,
                defaultDelayAfter = 100,
                observerCheckDelay = 500,
                defaultImageMatchThreshold = 0.8
            )
        )
    }

    private fun createSampleSteps(): List<Step> {
        return listOf(
            ActionStep(
                label = "Move to coordinates",
                action = Action.Mouse.MoveTo(target = Target.AtCoordinate(coordinate = Coordinate(100, 200)))
            ),
            ActionStep(
                label = "Click",
                action = Action.Mouse.Click(target = Target.AtCoordinate(coordinate = Coordinate(100, 200)))
            ),
            ActionStep(
                label = "Wait 1 second",
                action = Action.System.Wait(1000)
            ),
            ActionStep(
                label = "Type text",
                action = Action.Keyboard.TypeText("Hello AdaptiBot!")
            )
        )
    }

    fun createConditionalScript(): Script {
        return Script.restore(
            id = ScriptId(),
            name = "Conditional Test",
            description = "Script with conditional logic",
            rootContainer = StepContainer(steps = mutableListOf(
                ConditionalStep(
                    label = "Check if element exists",
                    condition = Condition.ElementExists(
                        matcher = VisualMatcher.ImagePresent(ImagePattern("", 0.8))
                    ),
                    trueContainer = StepContainer(steps = mutableListOf(
                        ActionStep(action = Action.Mouse.Click(target = Target.AtCoordinate(coordinate = Coordinate(50, 50))))
                    )),
                    elseContainer = StepContainer(steps = mutableListOf(
                        ActionStep(action = Action.System.Wait(500))
                    ))
                )
            )),
            settings = ScriptSettings()
        )
    }

    fun createGroupScript(): Script {
        return Script.restore(
            id = ScriptId(),
            name = "Group Test",
            description = "Script with grouped actions",
            rootContainer = StepContainer(steps = mutableListOf(
                GroupStep(
                    label = "Login Group",
                    container = StepContainer(steps = mutableListOf(
                        ActionStep(action = Action.Mouse.Click(target = Target.AtCoordinate(coordinate = Coordinate(300, 400)))),
                        ActionStep(action = Action.Keyboard.TypeText("username")),
                        ActionStep(action = Action.Keyboard.PressKeys(listOf(KeyboardKey.ENTER))),
                        ActionStep(action = Action.Keyboard.TypeText("password"))
                    ))
                )
            )),
            settings = ScriptSettings()
        )
    }
}
