package com.adaptibot.script

import kotlinx.serialization.Serializable

@Serializable
sealed class Action {

    @Serializable
    sealed class Mouse : Action() {

        @Serializable
        data class Click(
            val target: Target? = null,
            val button: MouseButton = MouseButton.LEFT,
            val type: MouseClickType = MouseClickType.SINGLE,
            val holdDuration: Long = 100L
        ) : Mouse()

        @Serializable
        data class Drag(
            val from: Target? = null,
            val to: Target
        ) : Mouse()

        @Serializable
        data class MoveTo(val target: Target) : Mouse()

        @Serializable
        data class Scroll(
            val direction: MouseScrollDirection,
            val amount: Int,
        ) : Mouse()
    }
    
    @Serializable
    sealed class Keyboard : Action() {
        @Serializable
        data class TypeText(val text: String) : Keyboard()

        @Serializable
        data class PressKeys(val keys: List<KeyboardKey>) : Keyboard()
    }
    
    @Serializable
    sealed class System : Action() {
        @Serializable
        data class Wait(val milliseconds: Long) : System()
        
        @Serializable
        data class LaunchApplication(val path: String, val args: List<String> = emptyList()) : System()
        
        @Serializable
        data class CloseApplication(val processName: String) : System()
    }
}
