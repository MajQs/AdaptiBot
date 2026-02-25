package com.adaptibot.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Action {

    @Serializable
    sealed class Mouse : Action() {

        @Serializable
        data class Click(
            val target: ElementIdentifier? = null,
            val button: MouseButton = MouseButton.LEFT,
            @SerialName("clickType") val type: ClickType = ClickType.SINGLE,
            val holdDuration: Long = 0L
        ) : Mouse()

        @Serializable
        data class Drag(
            val from: ElementIdentifier? = null,
            val to: ElementIdentifier
        ) : Mouse()

        @Serializable
        data class MoveTo(val target: ElementIdentifier) : Mouse()

        @Serializable
        data class Scroll(
            val direction: ScrollDirection,
            val amount: Int,
        ) : Mouse()
    }
    
    @Serializable
    sealed class Keyboard : Action() {
        @Serializable
        data class TypeText(val text: String) : Keyboard()
        
        @Serializable
        data class PressKey(val key: String) : Keyboard()
        
        @Serializable
        data class PressKeyCombination(val keys: List<String>) : Keyboard()
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
    
    @Serializable
    sealed class Flow : Action() {
        @Serializable
        object Stop : Flow()
        
        @Serializable
        data class JumpTo(val targetStepId: StepId) : Flow()
        
        @Serializable
        object Continue : Flow()
    }

}

@Serializable
enum class ScrollDirection {
    UP, DOWN, LEFT, RIGHT
}

@Serializable
enum class MouseButton {
    LEFT, RIGHT, MIDDLE
}

@Serializable
enum class ClickType {
    SINGLE, DOUBLE, TRIPLE
}

