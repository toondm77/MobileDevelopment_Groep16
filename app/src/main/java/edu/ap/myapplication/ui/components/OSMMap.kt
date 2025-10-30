package edu.ap.myapplication.ui.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import edu.ap.myapplication.model.CityTrip
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun OSMMap(
    trips: List<CityTrip>,
    modifier: Modifier = Modifier,
    currentLat: Double?,
    currentLng: Double?,
    calculateDistance: (Double, Double, Double, Double) -> Float,
    fixedHeightDp: Dp = 220.dp
) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            Configuration.getInstance().userAgentValue = ctx.packageName
            val map = MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                this.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            }
            val heightPx = (fixedHeightDp.value * ctx.resources.displayMetrics.density).toInt()
            map.layoutParams = android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, heightPx)
            map
        },
        modifier = modifier,
        update = { mapView ->
            val heightPx = (fixedHeightDp.value * mapView.context.resources.displayMetrics.density).toInt()
            mapView.layoutParams = android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, heightPx)

            val overlays = mapView.overlays
            overlays.clear()

            val markerPoints = mutableListOf<GeoPoint>()

            trips.forEach { trip ->
                val lat = trip.latitude
                val lng = trip.longitude
                if (lat != null && lng != null) {
                    val geo = GeoPoint(lat, lng)
                    markerPoints.add(geo)

                    val marker = Marker(mapView).apply {
                        position = geo
                        title = trip.title
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { _, _ ->
                            if (currentLat != null && currentLng != null) {
                                val dist = calculateDistance(currentLat, currentLng, lat, lng)
                                val distStr = if (dist >= 1000f) String.format(Locale.getDefault(), "%.1f km", dist / 1000f) else String.format(Locale.getDefault(), "%.0f m", dist)
                                Toast.makeText(context, "Afstand tot ${trip.title}: $distStr", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Locatiegegevens niet beschikbaar", Toast.LENGTH_SHORT).show()
                            }
                            true
                        }
                    }

                    overlays.add(marker)
                }
            }

            when {
                markerPoints.isEmpty() -> {
                    if (currentLat != null && currentLng != null) {
                        mapView.controller.setZoom(12.0)
                        mapView.controller.setCenter(GeoPoint(currentLat, currentLng))
                    } else {
                        mapView.controller.setZoom(3.0)
                    }
                }
                markerPoints.size == 1 -> {
                    mapView.controller.setCenter(markerPoints.first())
                    mapView.controller.setZoom(12.0)
                }
                else -> {
                    var north = Double.NEGATIVE_INFINITY
                    var south = Double.POSITIVE_INFINITY
                    var east = Double.NEGATIVE_INFINITY
                    var west = Double.POSITIVE_INFINITY
                    markerPoints.forEach { p ->
                        north = maxOf(north, p.latitude)
                        south = minOf(south, p.latitude)
                        east = maxOf(east, p.longitude)
                        west = minOf(west, p.longitude)
                    }
                    val bbox = BoundingBox(north, east, south, west)
                    mapView.zoomToBoundingBox(bbox, true, 100)
                }
            }

            mapView.invalidate()
        }
    )
}
