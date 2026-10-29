package com.jamiecarl.droneside.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

private val RefLineColor = Color(0x5A00D4FF)
private val LineColor = Color(0xFF3E8FA6)
private val DotColor = Color(0xFF00D4FF)

/** Port of the original CanvasView-based sparkline: faster laps read higher on the chart. */
@Composable
fun LapSparkline(
    laps: List<Double>,
    heightDp: Int = 46,
    modifier: Modifier = Modifier,
    holeColor: Color = com.jamiecarl.droneside.ui.theme.DsSurface
) {
    Canvas(modifier = modifier.fillMaxWidth().height(heightDp.dp)) {
        if (laps.size < 2) return@Canvas

        val w = size.width
        val h = size.height
        val min = laps.min()
        val max = laps.max()
        val range = (max - min).takeIf { it != 0.0 } ?: 1.0
        val padX = 10f
        val padY = 6f
        val usableW = (w - padX * 2).coerceAtLeast(1f)
        val usableH = (h - padY * 2).coerceAtLeast(1f)

        val points = laps.mapIndexed { i, lap ->
            val x = padX + if (laps.size == 1) 0f else (i.toFloat() / (laps.size - 1)) * usableW
            val y = padY + ((lap - min) / range).toFloat() * usableH
            Offset(x, y)
        }

        val bestIndex = laps.indexOf(min)

        drawLine(
            color = RefLineColor,
            start = Offset(0f, points[bestIndex].y),
            end = Offset(w, points[bestIndex].y),
            strokeWidth = 1.dp.toPx()
        )

        for (i in 0 until points.size - 1) {
            drawLine(
                color = LineColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        points.forEachIndexed { i, point ->
            if (i == bestIndex) {
                drawCircle(color = DotColor, radius = 3.5.dp.toPx(), center = point)
            } else {
                drawCircle(color = holeColor, radius = 3.dp.toPx(), center = point)
                drawCircle(
                    color = DotColor,
                    radius = 3.dp.toPx(),
                    center = point,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                )
            }
        }
    }
}
