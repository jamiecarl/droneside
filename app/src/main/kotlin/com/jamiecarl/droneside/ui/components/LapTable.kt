package com.jamiecarl.droneside.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jamiecarl.droneside.data.model.Lap
import com.jamiecarl.droneside.ui.theme.DsCyan
import com.jamiecarl.droneside.ui.theme.DsFaint
import com.jamiecarl.droneside.ui.theme.DsInk2
import com.jamiecarl.droneside.ui.theme.DsLine
import com.jamiecarl.droneside.ui.theme.DsMuted
import com.jamiecarl.droneside.ui.theme.DsSurface2
import com.jamiecarl.droneside.ui.theme.MonoDs
import java.util.Locale

@Composable
fun LapTable(laps: List<Lap>, modifier: Modifier = Modifier) {
    val best = laps.filter { it.lapNumber > 0 }.minOfOrNull { it.lengthSeconds }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DsSurface2, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            SectionLabel("LAP", modifier = Modifier.weight(1f))
            SectionLabel("TIME")
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DsLine))
        laps.forEach { lap ->
            val isBest = lap.lengthSeconds == best && lap.lapNumber > 0
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Text("#${lap.lapNumber}", fontFamily = MonoDs, fontSize = 12.sp, color = DsMuted, modifier = Modifier.weight(1f))
                Text(
                    String.format(Locale.US, "%.2f", lap.lengthSeconds),
                    fontFamily = MonoDs,
                    fontSize = 12.sp,
                    color = if (isBest) DsCyan else DsInk2,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
fun InlinePlaceholder(icon: ImageVector?, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = DsFaint, modifier = Modifier.size(24.dp))
        } else {
            CircularProgressIndicator(color = DsCyan, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        }
        Text(title, color = DsInk2, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
        Text(subtitle, color = DsMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}
