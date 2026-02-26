package com.adaptibot.action

sealed class ActionExecutionException(message: String) : Exception(message) {

    class ImageNotFound(
        bestConfidence: Double,
        threshold: Double
    ) : ActionExecutionException(
        "Image not found. Best match: ${formatPercent(bestConfidence)}, required: ${formatPercent(threshold)}. " +
                "Consider lowering the match threshold."
    )

    class CoordinateOutOfBounds(
        x: Int,
        y: Int,
        screenWidth: Int,
        screenHeight: Int
    ) : ActionExecutionException(
        "Coordinate ($x, $y) is outside screen bounds (${screenWidth}x${screenHeight})"
    )
}

private fun formatPercent(value: Double) = "%.1f%%".format(value * 100)
