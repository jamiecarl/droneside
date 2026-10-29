package com.jamiecarl.droneside.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jamiecarl.droneside.ui.theme.DsBase
import com.jamiecarl.droneside.ui.theme.DsCyan
import com.jamiecarl.droneside.ui.theme.DsLine
import com.jamiecarl.droneside.ui.theme.DsMuted
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView

fun openInMaps(context: Context, latitude: Double, longitude: Double, label: String) {
    val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label)})")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No maps app installed", Toast.LENGTH_SHORT).show()
    }
}

/**
 * A static OpenStreetMap preview centred on the club. It doesn't pan or zoom (that would fight the page
 * scroll); tapping it opens the device's maps app instead.
 */
@Composable
fun ClubMap(latitude: Double, longitude: Double, label: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val mapView = remember {
        // OSM's tile servers require an identifying user agent.
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            overlayManager.tilesOverlay.loadingBackgroundColor = android.graphics.Color.rgb(242, 239, 233)
            overlayManager.tilesOverlay.loadingLineColor = android.graphics.Color.rgb(242, 239, 233)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            setMultiTouchControls(false)
            isClickable = false
            isFocusable = false
            controller.setZoom(14.5)
            controller.setCenter(GeoPoint(latitude, longitude))
        }
    }
    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, DsLine, RoundedCornerShape(14.dp))
    ) {
        AndroidView(
            factory = { mapView },
            update = { it.controller.setCenter(GeoPoint(latitude, longitude)) },
            modifier = Modifier.fillMaxSize()
        )
        // Covers the map so taps open the maps app and drags scroll the page rather than the map.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(role = Role.Button, onClickLabel = "Open in maps") {
                    openInMaps(context, latitude, longitude, label)
                }
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(22.dp)
                .background(DsCyan.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(DsCyan, CircleShape)
                    .border(2.dp, DsBase, CircleShape)
            )
        }
        Text(
            "© OpenStreetMap contributors",
            fontSize = 9.sp,
            color = DsMuted,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(Color(0xCC0A0B0D), RoundedCornerShape(topStart = 6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
