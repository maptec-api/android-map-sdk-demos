package com.maptec.applied.demo.map

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MapOptions
import com.maptec.applied.style.layers.PropertyFactory.backgroundColor

/** SDK 默认地图加载占位色，与 MapOptions 内置 LIGHT_GRAY 一致。 */
const val MAP_BACKGROUND_COLOR = 0xFFF0E9E1.toInt()

/** Demo 统一 MapOptions：含默认 backgroundColor，避免首帧黑闪。 */
fun defaultDemoMapOptions(context: Context): MapOptions {
    return MapOptions.createFromAttributes(context, null).apply {
        backgroundColor(MAP_BACKGROUND_COLOR)
    }
}

@Composable
fun MapViewLifecycleEffect(mapView: MapView) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        val state = lifecycleOwner.lifecycle.currentState
        if (state.isAtLeast(Lifecycle.State.STARTED)) {
            mapView.onStart()
        }
        if (state.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }
}
