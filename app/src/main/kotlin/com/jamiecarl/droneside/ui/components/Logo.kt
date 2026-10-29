package com.jamiecarl.droneside.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jamiecarl.droneside.R
import com.jamiecarl.droneside.ui.theme.DsBase
import com.jamiecarl.droneside.ui.theme.DsCyan
import com.jamiecarl.droneside.ui.theme.DsInk
import com.jamiecarl.droneside.ui.theme.DsLine

/** The Lap D mark on its dark tile, as on the launcher. */
@Composable
fun DroneSideMark(size: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size * 0.24f)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(DsBase)
            .border(1.dp, DsLine, shape)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "DroneSide",
            // The launcher art leaves room for the system's mask; nothing masks it here, so fill the tile more.
            modifier = Modifier.fillMaxSize().scale(1.2f)
        )
    }
}

/** "Drone" in ink, "Side" in cyan. */
@Composable
fun DroneSideWordmark(fontSize: TextUnit = 28.sp, modifier: Modifier = Modifier) {
    Text(
        buildAnnotatedString {
            append("Drone")
            withStyle(SpanStyle(color = DsCyan)) { append("Side") }
        },
        color = DsInk,
        fontSize = fontSize,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.03 * fontSize.value).sp,
        modifier = modifier
    )
}
