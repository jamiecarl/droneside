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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jamiecarl.droneside.R
import com.jamiecarl.droneside.data.model.DEFAULT_CLUB_LOGO_URL
import com.jamiecarl.droneside.data.model.Event
import com.jamiecarl.droneside.data.model.resolveImageUrl
import com.jamiecarl.droneside.ui.theme.DsCyan
import com.jamiecarl.droneside.ui.theme.DsCyanSurface
import com.jamiecarl.droneside.ui.theme.DsFaint
import com.jamiecarl.droneside.ui.theme.DsInk
import com.jamiecarl.droneside.ui.theme.DsInk2
import com.jamiecarl.droneside.ui.theme.DsLine
import com.jamiecarl.droneside.ui.theme.DsMuted
import com.jamiecarl.droneside.ui.theme.DsMuted2
import com.jamiecarl.droneside.ui.theme.DsSurface
import com.jamiecarl.droneside.ui.theme.DsSurface2
import com.jamiecarl.droneside.ui.theme.Inter
import com.jamiecarl.droneside.ui.theme.MonoDs
import com.jamiecarl.droneside.util.parseApiDate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Space a tab screen's list leaves at the bottom so the floating tab bar never covers its last row. */
val FloatingNavClearance = 116.dp

fun Modifier.dsCard(
    radius: Dp = 20.dp,
    background: Color = DsSurface,
    border: Color = DsLine
): Modifier = this
    .background(background, RoundedCornerShape(radius))
    .border(1.dp, border, RoundedCornerShape(radius))
    .clip(RoundedCornerShape(radius))

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = DsMuted) {
    Text(
        text = text,
        modifier = modifier,
        fontFamily = MonoDs,
        fontSize = 10.sp,
        letterSpacing = 1.2.sp,
        color = color
    )
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = DsInk, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = DsCyan,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = DsInk,
    background: Color = DsSurface2
) {
    Column(
        modifier = modifier
            .background(background, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        SectionLabel(label)
        Text(value, fontFamily = MonoDs, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = DsMuted,
    background: Color = Color.Transparent,
    border: Color = DsLine
) {
    Text(
        text = text,
        fontFamily = MonoDs,
        fontSize = 10.sp,
        letterSpacing = 1.sp,
        color = color,
        modifier = modifier
            .background(background, RoundedCornerShape(999.dp))
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = DsInk2
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .dsCard(radius = 22.dp)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun SegmentedControl(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .dsCard(radius = 16.dp)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) DsCyanSurface else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) DsCyan else DsMuted
                )
            }
        }
    }
}

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .dsCard(radius = 16.dp, border = if (value.isNotBlank()) DsCyan else DsLine)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(DsIcons.Search, contentDescription = null, tint = DsMuted, modifier = Modifier.size(18.dp))
        Box(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                cursorBrush = SolidColor(DsCyan),
                textStyle = TextStyle(color = DsInk, fontSize = 15.sp, fontFamily = Inter),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, color = DsMuted2, fontSize = 15.sp)
                    inner()
                }
            )
        }
        if (value.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(role = Role.Button, onClickLabel = "Clear search") { onValueChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(DsIcons.Close, contentDescription = "Clear search", tint = DsCyan, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun FullScreenLoading(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = DsCyan, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
            Text(text = message, color = DsMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 14.dp))
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String = "", modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(64.dp).dsCard(radius = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = DsFaint, modifier = Modifier.size(28.dp))
            }
            Text(
                text = title,
                color = DsInk2,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
            )
            if (subtitle.isNotEmpty()) {
                Text(text = subtitle, color = DsMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun PilotAvatar(photoUrl: String?, modifier: Modifier = Modifier, cornerRadius: Dp = 14.dp) {
    AsyncImage(
        model = photoUrl ?: R.drawable.pilot_placeholder,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(com.jamiecarl.droneside.ui.theme.DsRaised)
    )
}

@Composable
fun ClubLogo(logoUrl: String?, modifier: Modifier = Modifier, cornerRadius: Dp = 14.dp) {
    AsyncImage(
        model = resolveImageUrl(logoUrl) ?: DEFAULT_CLUB_LOGO_URL,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(com.jamiecarl.droneside.ui.theme.DsRaised)
    )
}

@Composable
fun DateTile(start: String, highlight: Boolean, modifier: Modifier = Modifier) {
    val date = parseApiDate(start)
    val month = date?.let { SimpleDateFormat("MMM", Locale.getDefault()).format(it).uppercase() } ?: "—"
    val day = date?.let { SimpleDateFormat("dd", Locale.getDefault()).format(it) } ?: "—"
    Column(
        modifier = modifier
            .width(52.dp)
            .height(56.dp)
            .dsCard(radius = 14.dp, background = DsSurface2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(month, fontFamily = MonoDs, fontSize = 10.sp, letterSpacing = 1.sp, color = if (highlight) DsCyan else DsMuted)
        Text(day, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DsInk)
    }
}

/**
 * Event row in the redesign. With [showClub] the second line names the club and the pilot count sits
 * on the right; without it (inside a club's own lists) the second line is a compact "when · pilots" meta.
 */
@Composable
fun EventCard(event: Event, onTap: () -> Unit, showClub: Boolean, modifier: Modifier = Modifier) {
    val start = parseApiDate(event.start)
    val upcoming = start != null && start.after(Date())
    val meta = buildString {
        append(
            if (upcoming) SimpleDateFormat("EEE", Locale.getDefault()).format(start!!).uppercase()
            else "RESULTS"
        )
        append(" · ${event.pilotCount} PILOTS")
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .dsCard()
            .clickable(onClick = onTap)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DateTile(event.start, highlight = upcoming)
        Column(
            modifier = Modifier.weight(1f).padding(start = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(event.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = DsInk, lineHeight = 19.sp)
            if (showClub) {
                Text(event.club.name ?: "Unknown Club", fontSize = 12.sp, color = DsMuted)
            } else {
                Text(meta, fontFamily = MonoDs, fontSize = 11.sp, color = DsMuted)
            }
        }
        if (showClub) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                Text(event.pilotCount.toString(), fontFamily = MonoDs, fontSize = 13.sp, color = DsInk2)
                Text("PILOTS", fontFamily = MonoDs, fontSize = 10.sp, color = DsMuted)
            }
        }
    }
}
