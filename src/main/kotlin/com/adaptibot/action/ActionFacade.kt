package com.adaptibot.action

import com.adaptibot.action.domain.ActionHandler
import com.adaptibot.script.value.Action

/**
 * The module is responsible for **physically executing automation actions** at the operating system level:
 * mouse operations (clicks, drag-and-drop, scroll), keyboard simulation, and system-level actions
 * (pause, launching applications). It acts as the bridge between the script logic and the actual
 * control of the user interface.
 *
 * [ActionFacade] receives an [Action] object from the script and routes it to the appropriate handler.
 * New action types are registered exclusively through [ActionConfiguration] — without modifying this class.
 *
 * @throws ActionExecutionException when the action cannot be executed
 *         (e.g. image not found on screen, coordinates out of screen bounds, empty key list).
 * @see ActionConfiguration
 */
class ActionFacade internal constructor(
    private val actionHandlerPerType: Map<Class<out Action>, ActionHandler<in Action>>,
) {

    /**
     * Executes the given action by delegating it to the appropriate handler based on its type.
     *
     * @param action Action to execute (mouse, keyboard, system). No matching handler = no effect.
     * @throws ActionExecutionException when the handler is unable to carry out the action at runtime.
     */
    fun execute(action: Action) {
        actionHandlerPerType.entries
            .firstOrNull { (key, _) -> key.isInstance(action) }
            ?.value?.handle(action)
    }
}