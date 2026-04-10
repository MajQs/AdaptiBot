package com.adaptibot

import com.adaptibot.script.value.Action
import com.adaptibot.script.step.ActionStep
import com.adaptibot.script.value.Condition
import com.adaptibot.script.step.ConditionalStep
import com.adaptibot.script.step.ElseBlock
import com.adaptibot.script.step.IfBlock
import com.adaptibot.script.value.Coordinate
import com.adaptibot.script.step.GroupBlock
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
            steps = createSampleSteps(),
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
                action = Action.Mouse.MoveTo(
                    target = Target.AtCoordinate(coordinate = Coordinate(100, 200))
                )
            ),
            ActionStep(
                label = "Click",
                action = Action.Mouse.Click(
                    target = Target.AtCoordinate(coordinate = Coordinate(100, 200))
                )
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
            steps = listOf(
                ConditionalStep(
                    label = "Check if element exists",
                    condition = Condition.ElementExists(
                        matcher = VisualMatcher.ImagePresent(ImagePattern("", 0.8))
                    ),
                    ifBlock = IfBlock(
                        steps = listOf(
                            ActionStep(
                                action = Action.Mouse.Click(
                                    target = Target.AtCoordinate(coordinate = Coordinate(50, 50))
                                )
                            )
                        )
                    ),
                    elseBlock = ElseBlock(
                        steps = listOf(
                            ActionStep(
                                action = Action.System.Wait(500)
                            )
                        )
                    )
                )
            ),
            settings = ScriptSettings()
        )
    }

    fun createGroupScript(): Script {
        return Script.restore(
            id = ScriptId(),
            name = "Group Test",
            description = "Script with grouped actions",
            steps = listOf(
                GroupBlock(
                    label = "Login Group",
                    steps = listOf(
                        ActionStep(
                            action = Action.Mouse.Click(
                                target = Target.AtCoordinate(coordinate = Coordinate(300, 400))
                            )
                        ),
                        ActionStep(
                            action = Action.Keyboard.TypeText("username")
                        ),
                        ActionStep(
                            action = Action.Keyboard.PressKeys(listOf(KeyboardKey.ENTER))
                        ),
                        ActionStep(
                            action = Action.Keyboard.TypeText("password")
                        )
                    )
                )
            ),
            settings = ScriptSettings()
        )
    }
}
