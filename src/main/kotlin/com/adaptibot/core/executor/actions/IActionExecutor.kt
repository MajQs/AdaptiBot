package com.adaptibot.core.executor.actions

import com.adaptibot.common.model.Action
import com.adaptibot.common.model.Coordinate

interface IActionExecutor {
    fun execute(action: Action, resolvedCoordinate: Coordinate?): Boolean
}

