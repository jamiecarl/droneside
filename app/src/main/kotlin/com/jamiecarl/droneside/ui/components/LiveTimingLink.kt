package com.jamiecarl.droneside.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jamiecarl.droneside.ui.theme.DsInk
import com.jamiecarl.droneside.ui.theme.DsLive
import com.jamiecarl.droneside.ui.theme.DsLiveBorder
import com.jamiecarl.droneside.ui.theme.DsLiveInk
import com.jamiecarl.droneside.ui.theme.DsLiveSurface
import com.jamiecarl.droneside.ui.theme.MonoDs

/** Green call-to-action that opens the Live tab on a club's AUFPV timing. */
@Composable
fun LiveTimingLink(title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .dsCard(radius = 18.dp, background = DsLiveSurface, border = DsLiveBorder)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(DsIcons.Live, contentDescription = null, tint = DsLive, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, color = DsInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontFamily = MonoDs, fontSize = 11.sp, color = DsLiveInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(DsIcons.ChevronRight, contentDescription = null, tint = DsLiveInk, modifier = Modifier.size(18.dp))
    }
}
