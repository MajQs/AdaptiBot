package com.adaptibot.action

import com.adaptibot.action.domain.ActionHandler
import com.adaptibot.model.Action

class ActionFacade internal constructor(
    private val actionHandlerPerType: Map<Class<out Action>, ActionHandler<in Action>>,
) {

    fun execute(action: Action) {
        actionHandlerPerType.entries
            .firstOrNull { (key, _) -> key.isInstance(action) }
            ?.value?.handle(action)
    }
}