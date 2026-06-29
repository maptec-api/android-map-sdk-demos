package com.maptec.applied.demo.ui.screens.overlays

import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.maptec.applied.demo.R
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.ui.view.BubbleCalloutView
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.infowindow.InfoWindow
import com.maptec.applied.maps.overlay.infowindow.InfoWindowAnchor
import com.maptec.applied.maps.overlay.infowindow.InfoWindowOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkerViewScreen() {
    val context = LocalContext.current
    var maptecMap by remember { mutableStateOf<MaptecMap?>(null) }
    var customInfoWindow by remember { mutableStateOf<InfoWindow?>(null) }
    var bubbleCalloutView by remember { mutableStateOf<BubbleCalloutView?>(null) }
    var autoCloseInfoWindowOnMapTap by remember { mutableStateOf(false) }
    val scaffoldState = rememberBottomSheetScaffoldState()

    LaunchedEffect(Unit) {
        scaffoldState.bottomSheetState.expand()
    }

    DisposableEffect(maptecMap) {
        onDispose {
            customInfoWindow?.remove()
            customInfoWindow = null
            bubbleCalloutView = null
        }
    }

    LaunchedEffect(autoCloseInfoWindowOnMapTap) {
        customInfoWindow?.setAutoCloseOnMapClick(autoCloseInfoWindowOnMapTap)
    }

    BottomSheetScaffold(
        modifier = (if (customInfoWindow != null) Modifier.testTag("markerview_has_bubble") else Modifier)
            .testTag("markerview_screen"),
        scaffoldState = scaffoldState,
        sheetPeekHeight = 56.dp,
        sheetMaxWidth = Dp.Unspecified,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            MarkerViewControlPanel(
                mapReady = maptecMap != null,
                customInfoWindow = customInfoWindow,
                bubbleCalloutView = bubbleCalloutView,
                autoCloseInfoWindowOnMapTap = autoCloseInfoWindowOnMapTap,
                onToggleAutoCloseInfoWindowOnMapTap = { autoCloseInfoWindowOnMapTap = !autoCloseInfoWindowOnMapTap },
                onAddCustomBubble = { line1, line2, cornerRadius, maxWidth, bgColorHex, borderColorHex, borderWidth ->
                    Log.d("MarkerViewScreen", "onAddCustomBubble: map=$maptecMap")
                    val map = maptecMap ?: run {
                        Log.e("MarkerViewScreen", "onAddCustomBubble: maptecMap is null!")
                        return@MarkerViewControlPanel
                    }

                    customInfoWindow?.remove()
                    customInfoWindow = null
                    bubbleCalloutView = null

                    val bubbleView = BubbleCalloutView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                        )
                        tag = "markerview_bubble"
                        line1Text = line1
                        line2Text = line2
                        cornerRadiusPx = cornerRadius
                        maxWidthPx = maxWidth
                        try {
                            bubbleBackgroundColor = android.graphics.Color.parseColor(bgColorHex)
                        } catch (_: Exception) {}
                        try {
                            borderColor = android.graphics.Color.parseColor(borderColorHex)
                        } catch (_: Exception) {}
                        borderWidthPx = borderWidth
                    }
                    Log.d("MarkerViewScreen", "onAddCustomBubble: calling addInfoWindow")
                    val infoWindow = map.getOverlayEngine().addInfoWindow(
                        InfoWindowOptions()
                            .withLatLng(LatLng(1.4, 103.75))
                            .withContentView(bubbleView)
                            .withAnchor(InfoWindowAnchor.BOTTOM)
                            .withAutoCloseOnMapClick(autoCloseInfoWindowOnMapTap),
                    )
                    Log.d("MarkerViewScreen", "onAddCustomBubble: addInfoWindow returned $infoWindow")
                    infoWindow.addOnCloseListener {
                        customInfoWindow = null
                        bubbleCalloutView = null
                    }
                    customInfoWindow = infoWindow
                    bubbleCalloutView = bubbleView
                    Log.d("MarkerViewScreen", "onAddCustomBubble: customInfoWindow set=$customInfoWindow")
                    bubbleCalloutView?.setOnClickListener {
                        Toast.makeText(context, "信息窗口点击事件", Toast.LENGTH_SHORT).show()
                    }
                },
                onAddRichTextBubble = { html, cornerRadius, maxWidth, bgColorHex, borderColorHex, borderWidth ->
                    val map = maptecMap ?: return@MarkerViewControlPanel

                    customInfoWindow?.remove()
                    customInfoWindow = null
                    bubbleCalloutView = null

                    val bubbleView = BubbleCalloutView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                        )
                        tag = "markerview_bubble"
                        setRichText(html)
                        cornerRadiusPx = cornerRadius
                        maxWidthPx = maxWidth
                        try {
                            bubbleBackgroundColor = android.graphics.Color.parseColor(bgColorHex)
                        } catch (_: Exception) {}
                        try {
                            borderColor = android.graphics.Color.parseColor(borderColorHex)
                        } catch (_: Exception) {}
                        borderWidthPx = borderWidth
                    }
                    customInfoWindow = map.getOverlayEngine().addInfoWindow(
                        InfoWindowOptions()
                            .withLatLng(LatLng(1.4, 103.75))
                            .withContentView(bubbleView)
                            .withAnchor(InfoWindowAnchor.BOTTOM)
                            .withAutoCloseOnMapClick(autoCloseInfoWindowOnMapTap),
                    ).also { infoWindow ->
                        infoWindow.addOnCloseListener {
                            customInfoWindow = null
                            bubbleCalloutView = null
                        }
                    }
                    bubbleCalloutView = bubbleView
                    bubbleCalloutView?.setOnClickListener {
                        Toast.makeText(context, "富文本点击事件", Toast.LENGTH_SHORT).show()
                    }
                },
                onClearAll = {
                    customInfoWindow?.remove()
                    customInfoWindow = null
                    bubbleCalloutView = null
                },
            )
        },
    ) { padding ->
        Mapview(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            onMapReady = { _, map ->
                Log.d("MarkerViewScreen", "onMapReady: $map")
                maptecMap = map
            },
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkerViewControlPanel(
    mapReady: Boolean,
    customInfoWindow: InfoWindow?,
    bubbleCalloutView: BubbleCalloutView?,
    autoCloseInfoWindowOnMapTap: Boolean,
    onToggleAutoCloseInfoWindowOnMapTap: () -> Unit,
    onAddCustomBubble: (String, String, Float, Int, String, String, Float) -> Unit,
    onAddRichTextBubble: (String, Float, Int, String, String, Float) -> Unit,
    onClearAll: () -> Unit,
) {
    val context = LocalContext.current
    var line1 by remember { mutableStateOf("24 min") }
    var line2 by remember { mutableStateOf("7.9 km") }
    var richTextHtml by remember { mutableStateOf("<b>标题</b> <p><font color='red'>详情</font></p>") }
    var isRichTextMode by remember { mutableStateOf(false) }
    var cornerRadius by remember { mutableFloatStateOf(8f) }
    var maxWidth by remember { mutableIntStateOf(300) }
    var backgroundColorHex by remember { mutableStateOf("#424242") }
    var borderColorHex by remember { mutableStateOf("#E0E0E0") }
    var borderWidth by remember { mutableFloatStateOf(1f) }

    fun applyToBubble() {
        bubbleCalloutView?.apply {
            if (isRichTextMode) {
                setRichText(richTextHtml)
            } else {
                line1Text = line1
                line2Text = line2
            }
            cornerRadiusPx = cornerRadius
            maxWidthPx = maxWidth
            try {
                bubbleBackgroundColor = android.graphics.Color.parseColor(backgroundColorHex)
            } catch (_: Exception) {}
            try {
                borderColor = android.graphics.Color.parseColor(borderColorHex)
            } catch (_: Exception) {}
            borderWidthPx = borderWidth
        }
        customInfoWindow?.updatePosition()
    }

    LaunchedEffect(line1, line2, richTextHtml, cornerRadius, maxWidth, backgroundColorHex, borderColorHex, borderWidth) {
        applyToBubble()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    Log.d("MarkerViewScreen", "add button onClick fired, mapReady=$mapReady")
                    Toast.makeText(context, "add btn clicked", Toast.LENGTH_SHORT).show()
                    isRichTextMode = false
                    onAddCustomBubble(line1, line2, cornerRadius, maxWidth, backgroundColorHex, borderColorHex, borderWidth)
                },
                enabled = mapReady,
                modifier = Modifier.testTag("markerview_btn_add"),
            ) {
                Text(stringResource(R.string.markerview_add))
            }
            Button(
                onClick = onClearAll,
                enabled = customInfoWindow != null,
                modifier = Modifier.testTag("markerview_btn_remove"),
            ) {
                Text(stringResource(R.string.markerview_remove))
            }
            Button(
                onClick = onToggleAutoCloseInfoWindowOnMapTap,
                modifier = Modifier.weight(1f).testTag("markerview_btn_auto_close"),
                enabled = mapReady,
            ) {
                Text(if (autoCloseInfoWindowOnMapTap) stringResource(R.string.markerview_auto_close_on) else stringResource(R.string.markerview_auto_close_off))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = line1,
                onValueChange = { line1 = it },
                label = { Text(stringResource(R.string.markerview_line1_label)) },
                modifier = Modifier.weight(1f).testTag("markerview_input_line1"),
                enabled = bubbleCalloutView != null && !isRichTextMode,
            )
            OutlinedTextField(
                value = line2,
                onValueChange = { line2 = it },
                label = { Text(stringResource(R.string.markerview_line2_label)) },
                modifier = Modifier.weight(1f).testTag("markerview_input_line2"),
                enabled = bubbleCalloutView != null && !isRichTextMode,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.markerview_corner_radius_format, cornerRadius.toInt()))
                Slider(
                    value = cornerRadius,
                    onValueChange = { cornerRadius = it },
                    valueRange = 0f..50f,
                    modifier = Modifier.fillMaxWidth().testTag("markerview_slider_corner_radius"),
                    enabled = bubbleCalloutView != null,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.markerview_max_width_format, maxWidth))
                Slider(
                    value = maxWidth.toFloat(),
                    onValueChange = { maxWidth = it.toInt() },
                    valueRange = 50f..800f,
                    modifier = Modifier.fillMaxWidth().testTag("markerview_slider_max_width"),
                    enabled = bubbleCalloutView != null,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = backgroundColorHex,
                onValueChange = { backgroundColorHex = it },
                label = { Text(stringResource(R.string.markerview_bg_color_label)) },
                modifier = Modifier.weight(1f).testTag("markerview_input_bg_color"),
                enabled = bubbleCalloutView != null,
            )
            OutlinedTextField(
                value = borderColorHex,
                onValueChange = { borderColorHex = it },
                label = { Text(stringResource(R.string.markerview_border_color_label)) },
                modifier = Modifier.weight(1f).testTag("markerview_input_border_color"),
                enabled = bubbleCalloutView != null,
            )
        }
        Text(stringResource(R.string.markerview_border_width_format, borderWidth.toInt()))
        Slider(
            value = borderWidth,
            onValueChange = { borderWidth = it },
            valueRange = 0f..8f,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag("markerview_slider_border_width"),
            enabled = bubbleCalloutView != null,
        )

        Text(stringResource(R.string.markerview_rich_text_label), modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = richTextHtml,
            onValueChange = { richTextHtml = it },
            label = { Text(stringResource(R.string.markerview_rich_text_hint)) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("markerview_input_rich_text"),
            maxLines = 5,
            enabled = bubbleCalloutView == null || isRichTextMode,
        )
        Button(
            onClick = {
                isRichTextMode = true
                onAddRichTextBubble(richTextHtml, cornerRadius, maxWidth, backgroundColorHex, borderColorHex, borderWidth)
            },
            modifier = Modifier.fillMaxWidth().testTag("markerview_btn_add_rich"),
            enabled = mapReady,
        ) {
            Text(stringResource(R.string.markerview_add_rich_text))
        }
    }
}
