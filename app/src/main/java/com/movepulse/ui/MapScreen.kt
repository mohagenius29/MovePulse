package com.movepulse.ui

import android.content.Context
import android.location.Location
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.movepulse.data.RoutePoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private fun routeMeters(points: List<RoutePoint>): Float {
    val out = FloatArray(1)
    var total = 0f
    for (i in 1 until points.size) {
        Location.distanceBetween(points[i - 1].lat, points[i - 1].lon, points[i].lat, points[i].lon, out)
        total += out[0]
    }
    return total
}

@Composable
fun MapScreen(points: List<RoutePoint>) {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val mapView = remember {
        val cfg = Configuration.getInstance()
        cfg.load(ctx, ctx.getSharedPreferences("osm", Context.MODE_PRIVATE))
        cfg.userAgentValue = ctx.packageName
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(16.0)
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView },
            update = { view ->
                val geo = points.map { GeoPoint(it.lat, it.lon) }
                view.overlays.clear()
                if (geo.size >= 2) {
                    view.overlays.add(Polyline().apply {
                        setPoints(geo)
                        outlinePaint.color = 0xFF2E7D32.toInt()
                        outlinePaint.strokeWidth = 12f
                    })
                }
                if (geo.isNotEmpty()) {
                    view.overlays.add(Marker(view).apply {
                        position = geo.first()
                        title = "Start"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    })
                    if (geo.size >= 2) {
                        view.overlays.add(Marker(view).apply {
                            position = geo.last()
                            title = "Latest"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        })
                    }
                }
                if (view.tag != geo.size) {
                    view.tag = geo.size
                    view.post {
                        if (geo.size >= 2) {
                            view.zoomToBoundingBox(BoundingBox.fromGeoPoints(geo), false, 100)
                        } else if (geo.size == 1) {
                            view.controller.setZoom(18.0)
                            view.controller.setCenter(geo[0])
                        }
                    }
                }
                view.invalidate()
            },
        )

        Column(
            Modifier.align(Alignment.TopCenter).padding(12.dp)
        ) {
            val text = if (points.size < 2) {
                "No route yet. Go for an outdoor walk or run with GPS on."
            } else {
                "Today's route: %.2f km".format(routeMeters(points) / 1000f)
            }
            androidx.compose.material3.Card {
                Text(text, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
