package com.adaptibot.action.domain

import com.adaptibot.common.InterruptibleSleep
import com.adaptibot.script.value.Action.System

internal class SystemActionHandler : ActionHandler<System> {
    override fun handle(action: System) {
        return when (action) {
            is System.Wait -> {
                InterruptibleSleep.sleep(action.milliseconds)
            }

            is System.LaunchApplication -> {
                // TODO: Implement process launch
            }

            is System.CloseApplication -> {
                // TODO: Implement process termination
            }
        }
    }
}