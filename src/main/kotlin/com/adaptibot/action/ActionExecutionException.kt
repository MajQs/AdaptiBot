package com.adaptibot.action

sealed class ActionExecutionException(message: String) : Exception(message) {

    class ImageNotFound(
        bestConfidence: Double,
        threshold: Double
    ) : ActionExecutionException(
        "Image not found. Best match: ${formatPercent(bestConfidence)}, required: ${formatPercent(threshold)}. " +
                "Consider lowering the match threshold."
    )

    class TextNotFound(
        text: String
    ) : ActionExecutionException(
        "Text not found on screen: \"$text\". Make sure the text is visible"
    )

    class CoordinateOutOfBounds(
        x: Int,
        y: Int,
        screenWidth: Int,
        screenHeight: Int
    ) : ActionExecutionException(
        "Coordinate ($x, $y) is outside screen bounds (${screenWidth}x${screenHeight})"
    )

    class EmptyKeyList : ActionExecutionException(
        "Key list must not be empty. Provide at least one key to press."
    )
}

private fun formatPercent(value: Double) = "%.1f%%".format(value * 100)
