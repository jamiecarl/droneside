package com.jamiecarl.droneside.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** 24px stroke icons drawn to match the redesign; colour comes from the `Icon` tint. */
private fun strokeIcon(name: String, vararg paths: String, filled: Boolean = false): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        paths.forEach { data ->
            addPath(
                pathData = addPathNodes(data),
                fill = if (filled) SolidColor(Color.Black) else null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
    }.build()

private const val STAR = "M12 3l2.8 5.7 6.2.9-4.5 4.4 1.1 6.2L12 17.3 6.4 20.2l1.1-6.2L3 9.6l6.2-.9z"

object DsIcons {
    val Home = strokeIcon("home", "M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z")
    val Search = strokeIcon("search", "M18 11a7 7 0 1 1-14 0a7 7 0 1 1 14 0", "M20 20l-3.5-3.5")
    val Users = strokeIcon(
        "users",
        "M12.5 8a3.5 3.5 0 1 1-7 0a3.5 3.5 0 1 1 7 0",
        "M2.5 20a6.5 6.5 0 0 1 13 0",
        "M16 4.5a3.5 3.5 0 0 1 0 7M18 14.5a6.5 6.5 0 0 1 3.5 5.5"
    )
    val User = strokeIcon("user", "M15.5 8a3.5 3.5 0 1 1-7 0a3.5 3.5 0 1 1 7 0", "M5 20a7 7 0 0 1 14 0")
    val ChevronRight = strokeIcon("chevron-right", "M9 6l6 6-6 6")
    val ChevronLeft = strokeIcon("chevron-left", "M15 6l-6 6 6 6")
    val ChevronDown = strokeIcon("chevron-down", "M6 9l6 6 6-6")
    val Refresh = strokeIcon("refresh", "M21 12a9 9 0 1 1-3-6.7", "M21 4v5h-5")
    val Live = strokeIcon(
        "live",
        "M13 12a1 1 0 1 1-2 0a1 1 0 1 1 2 0",
        "M8.5 8.5a5 5 0 0 0 0 7M15.5 8.5a5 5 0 0 1 0 7",
        "M5.6 5.6a9 9 0 0 0 0 12.8M18.4 5.6a9 9 0 0 1 0 12.8"
    )
    val Clock = strokeIcon("clock", "M21 12a9 9 0 1 1-18 0a9 9 0 1 1 18 0", "M12 7v5l3 2")
    val Flag = strokeIcon("flag", "M5 21V4", "M5 4h11l-2 4 2 4H5")
    val Star = strokeIcon("star", STAR)
    val StarFilled = strokeIcon("star-filled", STAR, filled = true)
    val Close = strokeIcon("close", "M6 6l12 12M18 6L6 18")
    val Calendar = strokeIcon(
        "calendar",
        "M5 5h14a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1z",
        "M4 10h16",
        "M8 3v4M16 3v4"
    )
    val MapPin = strokeIcon(
        "map-pin",
        "M12 21s-7-6.2-7-11.5a7 7 0 0 1 14 0C19 14.8 12 21 12 21z",
        "M14.5 9.5a2.5 2.5 0 1 1-5 0a2.5 2.5 0 1 1 5 0"
    )
}
