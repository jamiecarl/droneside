package com.jamiecarl.droneside.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jamiecarl.droneside.data.model.Lap
import com.jamiecarl.droneside.ui.theme.DsInk2
import com.jamiecarl.droneside.ui.theme.DsLine
import com.jamiecarl.droneside.ui.theme.DsMuted
import com.jamiecarl.droneside.ui.theme.DsMuted2
import com.jamiecarl.droneside.ui.theme.DsSurface2
import com.jamiecarl.droneside.ui.theme.MonoDs
import java.util.Locale

@Composable
fun LapTable(laps: List<Lap>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DsSurface2, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text("LAP", fontFamily = MonoDs, fontSize = 10.sp, color = DsMuted2, modifier = Modifier.weight(1f))
            Text("TIME", fontFamily = MonoDs, fontSize = 10.sp, color = DsMuted2, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DsLine))
        laps.forEach { lap ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("#${lap.lapNumber}", fontFamily = MonoDs, fontSize = 12.sp, color = DsMuted, modifier = Modifier.weight(1f))
                Text(
                    String.format(Locale.US, "%.2f", lap.lengthSeconds),
                    fontFamily = MonoDs,
                    fontSize = 12.sp,
                    color = DsInk2,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun InlinePlaceholder(icon: String, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(icon, fontFamily = FontAwesome, fontSize = 28.sp, color = com.jamiecarl.droneside.ui.theme.DsFaint)
        Text(title, color = DsMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        Text(subtitle, color = DsMuted2, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}
