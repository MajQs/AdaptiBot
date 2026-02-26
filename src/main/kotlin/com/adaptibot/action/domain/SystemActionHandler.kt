package com.adaptibot.action.domain

import com.adaptibot.model.Action.System

internal class SystemActionHandler : ActionHandler<System> {
    override fun handle(action: System) {
        return when (action) {
            is System.Wait -> {
                Thread.sleep(action.milliseconds)
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