package com.adaptibot.infrastructure

import java.awt.GraphicsConfiguration
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import kotlin.math.ceil

/** A screen point expressed in native device pixels. */
internal data class PhysicalScreenPoint(val x: Int, val y: Int)

/** A screen point expressed in AWT user-space pixels. */
internal data class LogicalScreenPoint(val x: Int, val y: Int)

/**
 * Converts between native device pixels and the logical coordinates used by AWT bounds.
 *
 * A low-level Windows mouse hook reports physical pixels. AWT script coordinates use the
 * logical bounds of the monitor containing the point, so the conversion must use that
 * monitor's transform rather than the primary monitor's transform for every display.
 */
internal class DisplayCoordinateMapper private constructor(
    private val displays: List<Display>
) {

    fun toLogical(point: PhysicalScreenPoint): LogicalScreenPoint {
        val display = displays.firstOrNull { it.containsPhysical(point) }
            ?: displays.minByOrNull { it.physicalDistanceSquared(point) }
            ?: return LogicalScreenPoint(point.x, point.y)
        return display.toLogical(point)
    }

    fun toPhysical(point: LogicalScreenPoint): PhysicalScreenPoint {
        val display = displays.firstOrNull { it.containsLogical(point) }
            ?: displays.minByOrNull { it.logicalDistanceSquared(point) }
            ?: return PhysicalScreenPoint(point.x, point.y)
        return display.toPhysical(point)
    }

    private data class Display(
        val logicalBounds: Rectangle,
        val scaleX: Double,
        val scaleY: Double,
        val physicalWidth: Int,
        val physicalHeight: Int,
    ) {
        private val physicalX: Int get() = logicalBounds.x
        private val physicalY: Int get() = logicalBounds.y
        private val physicalRight: Int get() = physicalX + physicalWidth
        private val physicalBottom: Int get() = physicalY + physicalHeight
        private val logicalRight: Int get() = logicalBounds.x + logicalBounds.width
        private val logicalBottom: Int get() = logicalBounds.y + logicalBounds.height

        fun containsPhysical(point: PhysicalScreenPoint): Boolean =
            point.x in physicalX until physicalRight && point.y in physicalY until physicalBottom

        fun containsLogical(point: LogicalScreenPoint): Boolean =
            point.x in logicalBounds.x until logicalRight && point.y in logicalBounds.y until logicalBottom

        fun toLogical(point: PhysicalScreenPoint): LogicalScreenPoint = LogicalScreenPoint(
            x = (logicalBounds.x + clipRound((point.x - physicalX) / scaleX))
                .coerceIn(logicalBounds.x, logicalRight - 1),
            y = (logicalBounds.y + clipRound((point.y - physicalY) / scaleY))
                .coerceIn(logicalBounds.y, logicalBottom - 1),
        )

        fun toPhysical(point: LogicalScreenPoint): PhysicalScreenPoint = PhysicalScreenPoint(
            x = physicalX + clipRound((point.x - logicalBounds.x) * scaleX),
            y = physicalY + clipRound((point.y - logicalBounds.y) * scaleY),
        )

        fun physicalDistanceSquared(point: PhysicalScreenPoint): Long =
            distanceSquared(point.x, physicalX, physicalRight) +
                distanceSquared(point.y, physicalY, physicalBottom)

        fun logicalDistanceSquared(point: LogicalScreenPoint): Long =
            distanceSquared(point.x, logicalBounds.x, logicalRight) +
                distanceSquared(point.y, logicalBounds.y, logicalBottom)

        private fun distanceSquared(value: Int, start: Int, endExclusive: Int): Long {
            val distance = when {
                value < start -> start - value
                value >= endExclusive -> value - endExclusive + 1
                else -> 0
            }
            return distance.toLong() * distance
        }
    }

    companion object {
        fun current(): DisplayCoordinateMapper {
            val environment = GraphicsEnvironment.getLocalGraphicsEnvironment()
            val primaryId = environment.defaultScreenDevice.iDstring
            val displays = environment.screenDevices
                .map { it.defaultConfiguration }
                .sortedBy { if (it.device.iDstring == primaryId) 0 else 1 }
                .map(::displayFrom)
            return DisplayCoordinateMapper(displays)
        }

        private fun displayFrom(configuration: GraphicsConfiguration): Display {
            val bounds = configuration.bounds
            val transform = configuration.defaultTransform
            return Display(
                logicalBounds = bounds,
                scaleX = transform.scaleX,
                scaleY = transform.scaleY,
                physicalWidth = clipRound(bounds.width * transform.scaleX),
                physicalHeight = clipRound(bounds.height * transform.scaleY),
            )
        }

        /** Matches OpenJDK's ClipRound implementation: ceil(value - 0.5). */
        private fun clipRound(value: Double): Int = (ceil(value - 0.5)).toInt()
    }
}
