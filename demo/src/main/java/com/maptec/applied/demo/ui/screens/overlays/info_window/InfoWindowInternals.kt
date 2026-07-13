package com.maptec.applied.demo.ui.screens.overlays.info_window

import android.R.attr.tag
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.ui.screens.common.ColorPickerField
import com.maptec.applied.demo.ui.screens.common.DemoPanelButton
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.ui.screens.common.DemoPanelSlider
import com.maptec.applied.demo.ui.screens.common.DemoPanelSwitch
import com.maptec.applied.demo.ui.view.BubbleCalloutView
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.infowindow.InfoWindow
import com.maptec.applied.maps.overlay.infowindow.InfoWindowAnchor
import com.maptec.applied.maps.overlay.infowindow.InfoWindowOptions

private val DefaultInfoWindowLatLng = LatLng(1.360879, 103.732578)

internal enum class InfoWindowMode {
    BASIC,
    STYLE,
    RICH_TEXT,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InfoWindowScreen(
    mode: InfoWindowMode,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var maptecMap by remember { mutableStateOf<MaptecMap?>(null) }
    var customInfoWindow by remember { mutableStateOf<InfoWindow?>(null) }
    var bubbleCalloutView by remember { mutableStateOf<BubbleCalloutView?>(null) }
    var autoCloseInfoWindowOnMapTap by remember { mutableStateOf(false) }

    var line1 by remember { mutableStateOf("24 min") }
    var line2 by remember { mutableStateOf("7.9 km") }
    var richTextHtml by remember { mutableStateOf("<b>标题</b> <p><font color='red'>详情</font></p>") }
    var cornerRadius by remember { mutableFloatStateOf(8f) }
    var maxWidth by remember { mutableIntStateOf(300) }
    var backgroundColorHex by remember { mutableStateOf("#424242") }
    var borderColorHex by remember { mutableStateOf("#E0E0E0") }
    var borderWidth by remember { mutableFloatStateOf(1f) }

    val isRichTextMode = mode == InfoWindowMode.RICH_TEXT

    fun addCustomBubble() {
        val map = maptecMap ?: run {
            Log.e("InfoWindowScreen", "addCustomBubble: maptecMap is null!")
            return
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
                bubbleBackgroundColor = android.graphics.Color.parseColor(backgroundColorHex)
            } catch (_: Exception) {
            }
            try {
                borderColor = android.graphics.Color.parseColor(borderColorHex)
            } catch (_: Exception) {
            }
            borderWidthPx = borderWidth
        }
        Log.d("InfoWindowScreen", "addCustomBubble: calling addInfoWindow")
        val infoWindow = map.getOverlayEngine().addInfoWindow(
            InfoWindowOptions()
                .withLatLng(DefaultInfoWindowLatLng)
                .withContentView(bubbleView)
                .withAnchor(InfoWindowAnchor.BOTTOM)
                .withAutoCloseOnMapClick(autoCloseInfoWindowOnMapTap),
        )
        Log.d("InfoWindowScreen", "addCustomBubble: addInfoWindow returned $infoWindow")
        infoWindow.addOnCloseListener {
            customInfoWindow = null
            bubbleCalloutView = null
        }
        customInfoWindow = infoWindow
        bubbleCalloutView = bubbleView
        bubbleCalloutView?.setOnClickListener {
            Toast.makeText(context, "信息窗口点击事件", Toast.LENGTH_SHORT).show()
        }
    }

    fun addRichTextBubble() {
        val map = maptecMap ?: return

        customInfoWindow?.remove()
        customInfoWindow = null
        bubbleCalloutView = null

        val bubbleView = BubbleCalloutView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            )
            tag = "markerview_bubble"
            setRichText(richTextHtml)
            cornerRadiusPx = cornerRadius
            maxWidthPx = maxWidth
            try {
                bubbleBackgroundColor = android.graphics.Color.parseColor(backgroundColorHex)
            } catch (_: Exception) {
            }
            try {
                borderColor = android.graphics.Color.parseColor(borderColorHex)
            } catch (_: Exception) {
            }
            borderWidthPx = borderWidth
        }
        customInfoWindow = map.getOverlayEngine().addInfoWindow(
            InfoWindowOptions()
                .withLatLng(DefaultInfoWindowLatLng)
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
    }

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
            } catch (_: Exception) {
            }
            try {
                borderColor = android.graphics.Color.parseColor(borderColorHex)
            } catch (_: Exception) {
            }
            borderWidthPx = borderWidth
        }
        customInfoWindow?.updatePosition()
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

    LaunchedEffect(maptecMap) {
        if (maptecMap == null) return@LaunchedEffect
        when (mode) {
            InfoWindowMode.BASIC, InfoWindowMode.STYLE -> addCustomBubble()
            InfoWindowMode.RICH_TEXT -> addRichTextBubble()
        }
    }

    LaunchedEffect(line1, line2, richTextHtml, cornerRadius, maxWidth, backgroundColorHex, borderColorHex, borderWidth) {
        applyToBubble()
    }

    DemoPanelScaffold(
        modifier = (if (customInfoWindow != null) Modifier.testTag("markerview_has_bubble") else Modifier)
            .testTag("markerview_screen")
            .then(modifier),
        sheetContent = {
            DemoPanelColumn {
                InfoWindowControlPanel(
                    mode = mode,
                    mapReady = maptecMap != null,
                    customInfoWindow = customInfoWindow,
                    bubbleCalloutView = bubbleCalloutView,
                    autoCloseInfoWindowOnMapTap = autoCloseInfoWindowOnMapTap,
                    onToggleAutoCloseInfoWindowOnMapTap = { autoCloseInfoWindowOnMapTap = !autoCloseInfoWindowOnMapTap },
                    line1 = line1,
                    onLine1Change = { line1 = it },
                    line2 = line2,
                    onLine2Change = { line2 = it },
                    richTextHtml = richTextHtml,
                    onRichTextHtmlChange = { richTextHtml = it },
                    cornerRadius = cornerRadius,
                    onCornerRadiusChange = { cornerRadius = it },
                    maxWidth = maxWidth,
                    onMaxWidthChange = { maxWidth = it },
                    backgroundColorHex = backgroundColorHex,
                    onBackgroundColorHexChange = { backgroundColorHex = it },
                    borderColorHex = borderColorHex,
                    onBorderColorHexChange = { borderColorHex = it },
                    borderWidth = borderWidth,
                    onBorderWidthChange = { borderWidth = it },
                    onAddCustomBubble = { addCustomBubble() },
                    onAddRichTextBubble = { addRichTextBubble() },
                    onClearAll = {
                        customInfoWindow?.remove()
                        customInfoWindow = null
                        bubbleCalloutView = null
                    },
                )
            }
        },
        content = {
            Mapview(
                modifier = Modifier.fillMaxSize().testTag("mapView"),
                onMapReady = { _, map ->
                    Log.d("InfoWindowScreen", "onMapReady: $map")
                    maptecMap = map
                },
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoWindowControlPanel(
    mode: InfoWindowMode,
    mapReady: Boolean,
    customInfoWindow: InfoWindow?,
    bubbleCalloutView: BubbleCalloutView?,
    autoCloseInfoWindowOnMapTap: Boolean,
    onToggleAutoCloseInfoWindowOnMapTap: () -> Unit,
    line1: String,
    onLine1Change: (String) -> Unit,
    line2: String,
    onLine2Change: (String) -> Unit,
    richTextHtml: String,
    onRichTextHtmlChange: (String) -> Unit,
    cornerRadius: Float,
    onCornerRadiusChange: (Float) -> Unit,
    maxWidth: Int,
    onMaxWidthChange: (Int) -> Unit,
    backgroundColorHex: String,
    onBackgroundColorHexChange: (String) -> Unit,
    borderColorHex: String,
    onBorderColorHexChange: (String) -> Unit,
    borderWidth: Float,
    onBorderWidthChange: (Float) -> Unit,
    onAddCustomBubble: () -> Unit,
    onAddRichTextBubble: () -> Unit,
    onClearAll: () -> Unit,
) {
    val isRichTextMode = mode == InfoWindowMode.RICH_TEXT

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (mode != InfoWindowMode.RICH_TEXT) {
                DemoPanelButton(
                    onClick = onAddCustomBubble,
                    enabled = mapReady,
                    modifier = Modifier.weight(1f).height(40.dp).testTag("markerview_btn_add"),
                ) {
                    Text(stringResource(R.string.markerview_add))
                }
            }
            DemoPanelButton(
                onClick = onClearAll,
                enabled = customInfoWindow != null,
                modifier = Modifier.weight(1f).height(40.dp).testTag("markerview_btn_remove"),
            ) {
                Text(stringResource(R.string.markerview_remove))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (autoCloseInfoWindowOnMapTap) {
                    stringResource(R.string.markerview_auto_close_on)
                } else {
                    stringResource(R.string.markerview_auto_close_off)
                },
                modifier = Modifier.weight(1f),
            )
            DemoPanelSwitch(
                checked = autoCloseInfoWindowOnMapTap,
                onCheckedChange = { onToggleAutoCloseInfoWindowOnMapTap() },
                enabled = mapReady,
                modifier = Modifier.testTag("markerview_btn_auto_close"),
            )
        }
        if (mode != InfoWindowMode.RICH_TEXT) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = line1,
                    onValueChange = onLine1Change,
                    label = { Text(stringResource(R.string.markerview_line1_label)) },
                    modifier = Modifier.fillMaxWidth().testTag("markerview_input_line1"),
                    enabled = bubbleCalloutView != null && !isRichTextMode,
                )
                OutlinedTextField(
                    value = line2,
                    onValueChange = onLine2Change,
                    label = { Text(stringResource(R.string.markerview_line2_label)) },
                    modifier = Modifier.fillMaxWidth().testTag("markerview_input_line2"),
                    enabled = bubbleCalloutView != null && !isRichTextMode,
                )
            }
        }
        if (mode == InfoWindowMode.STYLE || mode == InfoWindowMode.RICH_TEXT) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.markerview_corner_radius_format, cornerRadius.toInt()))
                    DemoPanelSlider(
                        value = cornerRadius,
                        onValueChange = onCornerRadiusChange,
                        valueRange = 0f..50f,
                        modifier = Modifier.fillMaxWidth().testTag("markerview_slider_corner_radius"),
                        enabled = bubbleCalloutView != null,
                    )
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.markerview_max_width_format, maxWidth))
                    DemoPanelSlider(
                        value = maxWidth.toFloat(),
                        onValueChange = { onMaxWidthChange(it.toInt()) },
                        valueRange = 50f..800f,
                        modifier = Modifier.fillMaxWidth().testTag("markerview_slider_max_width"),
                        enabled = bubbleCalloutView != null,
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ColorPickerField(
                    value = backgroundColorHex,
                    onValueChange = onBackgroundColorHexChange,
                    label = stringResource(R.string.markerview_bg_color_label),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "markerview_input_bg_color",
                    enabled = bubbleCalloutView != null,
                )
                ColorPickerField(
                    value = borderColorHex,
                    onValueChange = onBorderColorHexChange,
                    label = stringResource(R.string.markerview_border_color_label),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "markerview_input_border_color",
                    enabled = bubbleCalloutView != null,
                )
            }
            Text(stringResource(R.string.markerview_border_width_format, borderWidth.toInt()))
            DemoPanelSlider(
                value = borderWidth,
                onValueChange = onBorderWidthChange,
                valueRange = 0f..8f,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag("markerview_slider_border_width"),
                enabled = bubbleCalloutView != null,
            )
        }

        if (mode == InfoWindowMode.RICH_TEXT) {
            Text(stringResource(R.string.markerview_rich_text_label), modifier = Modifier.padding(top = 8.dp))
            OutlinedTextField(
                value = richTextHtml,
                onValueChange = onRichTextHtmlChange,
                label = { Text(stringResource(R.string.markerview_rich_text_hint)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("markerview_input_rich_text"),
                maxLines = 5,
                enabled = bubbleCalloutView != null,
            )
            DemoPanelButton(
                onClick = onAddRichTextBubble,
                modifier = Modifier.fillMaxWidth().testTag("markerview_btn_add_rich"),
                enabled = mapReady,
            ) {
                Text(stringResource(R.string.markerview_add_rich_text))
            }
        }
    }
}
