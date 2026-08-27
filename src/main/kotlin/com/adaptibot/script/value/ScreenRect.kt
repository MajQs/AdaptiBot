package com.adaptibot.script.value

import kotlinx.serialization.Serializable

/** Rectangle in absolute virtual-desktop coordinates; [x] and [y] may be negative. */
@Serializable
data class ScreenRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
) {

    init {
        require(width >= 0 && height >= 0) { "ScreenRect dimensions must not be negative: ${width}x$height" }
    }

    val right: Int get() = x + width
    val bottom: Int get() = y + height
    val isEmpty: Boolean get() = width == 0 || height == 0

    fun contains(coordinate: Coordinate): Boolean =
        coordinate.x >= x && coordinate.x < right && coordinate.y >= y && coordinate.y < bottom

    fun intersect(other: ScreenRect): ScreenRect {
        val left = maxOf(x, other.x)
        val top = maxOf(y, other.y)
        val newRight = minOf(right, other.right)
        val newBottom = minOf(bottom, other.bottom)

        if (newRight <= left || newBottom <= top) return ScreenRect(left, top, 0, 0)
        return ScreenRect(left, top, newRight - left, newBottom - top)
    }

    fun union(other: ScreenRect): ScreenRect {
        if (isEmpty) return other
        if (other.isEmpty) return this

        val left = minOf(x, other.x)
        val top = minOf(y, other.y)
        return ScreenRect(left, top, maxOf(right, other.right) - left, maxOf(bottom, other.bottom) - top)
    }

    fun expand(margin: Int): ScreenRect =
        ScreenRect(x - margin, y - margin, width + 2 * margin, height + 2 * margin)

    override fun toString(): String = "${width}x$height@($x,$y)"

    companion object {
        val EMPTY = ScreenRect(0, 0, 0, 0)
    }
}

