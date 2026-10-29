package com.jamiecarl.droneside.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jamiecarl.droneside.data.model.Event
import com.jamiecarl.droneside.ui.theme.DsCyan
import com.jamiecarl.droneside.ui.theme.DsFaint
import com.jamiecarl.droneside.ui.theme.DsInk
import com.jamiecarl.droneside.ui.theme.DsLine
import com.jamiecarl.droneside.ui.theme.DsMuted
import com.jamiecarl.droneside.ui.theme.DsMuted2
import com.jamiecarl.droneside.ui.theme.DsSurface
import com.jamiecarl.droneside.ui.theme.MonoDs

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        fontFamily = MonoDs,
        fontSize = 10.sp,
        letterSpacing = 1.sp,
        color = DsMuted2
    )
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = DsInk) {
    Column(
        modifier = modifier
            .background(DsSurface.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(text = label, fontFamily = MonoDs, fontSize = 10.sp, letterSpacing = 1.sp, color = DsMuted2)
        Text(
            text = value,
            fontFamily = MonoDs,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
fun FullScreenLoading(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = DsCyan)
            Text(text = message, color = DsMuted, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
fun EmptyState(icon: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = icon, fontFamily = FontAwesome, fontSize = 48.sp, color = DsFaint)
            Text(
                text = title,
                color = DsMuted,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Text(text = subtitle, color = DsMuted2, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun PilotAvatar(photoUrl: String?, modifier: Modifier = Modifier) {
    AsyncImage(
        model = photoUrl ?: com.jamiecarl.droneside.R.drawable.pilot_placeholder,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
    )
}

@Composable
fun ClubLogo(logoUrl: String?, modifier: Modifier = Modifier) {
    AsyncImage(
        model = com.jamiecarl.droneside.data.model.resolveImageUrl(logoUrl)
            ?: com.jamiecarl.droneside.data.model.DEFAULT_CLUB_LOGO_URL,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(10.dp))
    )
}

@Composable
fun ClubEventHeaderCard(
    event: Event,
    formattedDate: String,
    onTap: () -> Unit,
    onClubTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DsSurface, RoundedCornerShape(16.dp))
            .border(1.dp, DsLine, RoundedCornerShape(16.dp))
            .clickable(onClick = onTap)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        ClubLogo(
            logoUrl = event.club.logoUrl,
            modifier = Modifier
                .size(40.dp)
                .let { if (onClubTap != null) it.clickable(onClick = onClubTap) else it }
        )
        Column(modifier = Modifier.padding(start = 12.dp).fillMaxWidth(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(text = event.name, color = DsInk, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(text = event.club.name ?: "Unknown Club", color = DsMuted2, fontSize = 12.sp)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formattedDate, fontFamily = MonoDs, fontSize = 12.sp, color = DsCyan)
                Text(text = "PILOTS ${event.pilotCount}", fontFamily = MonoDs, fontSize = 12.sp, color = DsMuted)
            }
        }
    }
}
