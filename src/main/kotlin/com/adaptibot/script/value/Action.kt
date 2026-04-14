package com.adaptibot.script.value

import kotlinx.serialization.Serializable

@Serializable
sealed class Action {
    abstract val delayBefore: Long

    @Serializable
    sealed class Mouse : Action() {

        @Serializable
        data class Click(
            override val delayBefore: Long = 0,
            val target: Target? = null,
            val button: MouseButton = MouseButton.LEFT,
            val type: MouseClickType = MouseClickType.SINGLE,
            val holdDuration: Long = 100L
        ) : Mouse()

        @Serializable
        data class Drag(
            override val delayBefore: Long = 0,
            val from: Target? = null,
            val to: Target
        ) : Mouse()

        @Serializable
        data class MoveTo(
            val target: Target,
            override val delayBefore: Long = 0
        ) : Mouse()

        @Serializable
        data class Scroll(
            val direction: MouseScrollDirection,
            val amount: Int,
            override val delayBefore: Long = 0
        ) : Mouse()
    }
    
    @Serializable
    sealed class Keyboard : Action() {
        @Serializable
        data class TypeText(
            val text: String,
            override val delayBefore: Long = 0
        ) : Keyboard()

        @Serializable
        data class PressKeys(
            val keys: List<KeyboardKey>,
            override val delayBefore: Long = 0
        ) : Keyboard()
    }
    
    @Serializable
    sealed class System : Action() {
        @Serializable
        data class Wait(
            val milliseconds: Long,
            override val delayBefore: Long = 0
        ) : System()

        @Serializable
        data class LaunchApplication(
            val path: String,
            val args: List<String> = emptyList(),
            override val delayBefore: Long = 0
        ) : System()

        @Serializable
        data class CloseApplication(
            val processName: String,
            override val delayBefore: Long = 0
        ) : System()
    }
}
