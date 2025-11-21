package com.adaptibot

import com.adaptibot.common.model.*

object TestUtils {
    
    fun createTestScript(name: String = "Test Script"): Script {
        return Script(
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
            Step.ActionStep(
                id = StepId("step_1"),
                label = "Move to coordinates",
                action = Action.Mouse.MoveTo(
                    target = ElementIdentifier.ByCoordinate(
                        coordinate = Coordinate(100, 200)
                    )
                )
            ),
            Step.ActionStep(
                id = StepId("step_2"),
                label = "Click",
                action = Action.Mouse.LeftClick(
                    target = ElementIdentifier.ByCoordinate(
                        coordinate = Coordinate(100, 200)
                    )
                )
            ),
            Step.ActionStep(
                id = StepId("step_3"),
                label = "Wait 1 second",
                action = Action.System.Wait(1000)
            ),
            Step.ActionStep(
                id = StepId("step_4"),
                label = "Type text",
                action = Action.Keyboard.TypeText("Hello AdaptiBot!")
            )
        )
    }
    
    fun createConditionalScript(): Script {
        return Script(
            name = "Conditional Test",
            description = "Script with conditional logic",
            steps = listOf(
                Step.ConditionalBlock(
                    id = StepId("cond_1"),
                    label = "Check if element exists",
                    condition = Condition.ElementExists(
                        identifier = ElementIdentifier.ByCoordinate(
                            coordinate = Coordinate(50, 50)
                        )
                    ),
                    thenSteps = listOf(
                        Step.ActionStep(
                            id = StepId("step_then"),
                            action = Action.Mouse.LeftClick(
                                target = ElementIdentifier.ByCoordinate(
                                    coordinate = Coordinate(50, 50)
                                )
                            )
                        )
                    ),
                    elseSteps = listOf(
                        Step.ActionStep(
                            id = StepId("step_else"),
                            action = Action.System.Wait(500)
                        )
                    )
                )
            )
        )
    }
    
    fun createGroupScript(): Script {
        return Script(
            name = "Group Test",
            description = "Script with grouped actions",
            steps = listOf(
                Step.GroupBlock(
                    id = StepId("group_1"),
                    label = "Login Group",
                    name = "Login Procedure",
                    steps = listOf(
                        Step.ActionStep(
                            id = StepId("login_1"),
                            action = Action.Mouse.LeftClick(
                                target = ElementIdentifier.ByCoordinate(
                                    coordinate = Coordinate(300, 400)
                                )
                            )
                        ),
                        Step.ActionStep(
                            id = StepId("login_2"),
                            action = Action.Keyboard.TypeText("username")
                        ),
                        Step.ActionStep(
                            id = StepId("login_3"),
                            action = Action.Keyboard.PressKey("Tab")
                        ),
                        Step.ActionStep(
                            id = StepId("login_4"),
                            action = Action.Keyboard.TypeText("password")
                        )
                    )
                )
            )
        )
    }
}

