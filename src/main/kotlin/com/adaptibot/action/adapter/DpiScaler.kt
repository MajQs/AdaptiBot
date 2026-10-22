package com.adaptibot.action.adapter

import com.adaptibot.script.value.Coordinate
import com.adaptibot.infrastructure.DisplayCoordinateMapper
import com.adaptibot.infrastructure.LogicalScreenPoint

internal object DpiScaler {

    fun toPhysical(coordinate: Coordinate): Coordinate =
        DisplayCoordinateMapper.current()
            .toPhysical(LogicalScreenPoint(coordinate.x, coordinate.y))
            .let { Coordinate(it.x, it.y) }
}
