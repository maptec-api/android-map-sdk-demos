package com.maptec.applied.demo.ui.screens.overlays.model

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.io.File
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.Constants
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.map.showMapStyleLoadError
import com.maptec.applied.demo.ui.screens.common.DemoNumericSliderField
import com.maptec.applied.demo.ui.screens.common.DemoPanelButton
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.ui.screens.common.DemoPanelSwitchRow
import com.maptec.applied.demo.ui.screens.common.formatDemoSliderValue
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback
import com.maptec.applied.maps.overlay.model.Model
import com.maptec.applied.maps.overlay.model.ModelOptions

/** 滨海湾金沙酒店。本地 glb，锚点用这份坐标。 */
private val SANDS_POSITION = LatLng(1.283983, 103.859149)
/** 新加坡摩天观景轮。在线 glb，和金沙不是同一个锚点。 */
private val FLYER_POSITION = LatLng(1.28942, 103.86319)
private const val MODEL_ID_SANDS = "sands-detail"
private const val MODEL_ID_FLYER = "flyer-detail"
private const val MODEL_ASSET = "models/sands-detail.glb"
private const val MODEL_HTTPS_URL =
    "https://cdn.jsdelivr.net/gh/maptec-api/android-map-sdk-demos@main/demo/src/main/assets/flyer-detail.glb"

/**
 * 把 assets 里的 sands-detail.glb 拷到应用私有目录，并返回该文件绝对路径。
 * 路径形如 `/data/data/<包名>/files/models/sands-detail.glb`。
 */
private fun sandsFilePath(context: Context): String {
    val out = File(context.filesDir, MODEL_ASSET)
    out.parentFile?.mkdirs()
    context.assets.open(MODEL_ASSET).use { input ->
        out.outputStream().use { output -> input.copyTo(output) }
    }
    return out.absolutePath
}

/**
 * 金沙和摩天轮都按米建模，默认 1 倍就是真实尺寸。
 */
private const val MODEL_DEFAULT_SCALE = 1f
private const val MODEL_STYLE_LIGHT = Constants.DEFAULT_STYLE_ID
private const val MODEL_STYLE_DARK = "dark"

private suspend fun downloadGlb(): ByteArray = withContext(Dispatchers.IO) {
    URL(MODEL_HTTPS_URL).openStream().use { it.readBytes() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelBasicScreen() {
    val context = LocalContext.current
    var map by remember { mutableStateOf<MaptecMap?>(null) }
    var model by remember { mutableStateOf<Model?>(null) }
    var placed by remember { mutableStateOf(false) }
    var height by remember { mutableFloatStateOf(0f) }
    var rotationX by remember { mutableFloatStateOf(0f) }
    var rotationY by remember { mutableFloatStateOf(0f) }
    var rotationZ by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(MODEL_DEFAULT_SCALE) }
    var minZoomText by remember { mutableStateOf("0") }
    var maxZoomText by remember { mutableStateOf("22") }
    var visible by remember { mutableStateOf(true) }
    var binarySource by remember { mutableStateOf(false) }
    var httpsSource by remember { mutableStateOf(false) }
    var darkStyle by remember { mutableStateOf(false) }
    var styleSwitching by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var loadState by remember { mutableStateOf<Model.LoadState?>(null) }
    var loadFailureReason by remember { mutableStateOf<String?>(null) }
    var loadStateTrail by remember { mutableStateOf("") }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val scope = rememberCoroutineScope()

    val minZoomError = modelZoomError(minZoomText, maxZoomText, isMin = true)
    val maxZoomError = modelZoomError(maxZoomText, minZoomText, isMin = false)
    val zoomValid = minZoomError == null && maxZoomError == null

    fun publishLoadState(item: Model) {
        val next = item.loadState
        loadFailureReason = item.loadFailureReason
        if (next == loadState) {
            return
        }
        loadState = next
        loadStateTrail = if (loadStateTrail.isEmpty()) next.name else "$loadStateTrail → ${next.name}"
    }

    fun place(maptecMap: MaptecMap, httpsBytes: ByteArray?): Model {
        loadState = null
        loadFailureReason = null
        loadStateTrail = ""
        val fromHttps = httpsBytes != null
        val modelId = if (fromHttps) MODEL_ID_FLYER else MODEL_ID_SANDS
        val options = ModelOptions()
            .withModelId(modelId)
            .withPosition(if (fromHttps) FLYER_POSITION else SANDS_POSITION)
            .withHeight(height)
            .withRotation(rotationX, rotationY, rotationZ)
            .withScale(scale, scale, scale)
            .withMinZoom(minZoomText.toFloat())
            .withMaxZoom(maxZoomText.toFloat())
            .withVisible(visible)
            .withClickable(true)
        when {
            httpsBytes != null -> options.withSource(httpsBytes)
            binarySource -> options.withSource(context.assets.open(MODEL_ASSET).use { it.readBytes() })
            else -> options.withSource(sandsFilePath(context))
        }
        val model = maptecMap.getOverlayEngine().addModel(options)
        model.addOnClickListener { item ->
            Toast.makeText(
                context,
                context.getString(R.string.model_click_message, item.modelId ?: modelId),
                Toast.LENGTH_SHORT,
            ).show()
            true
        }
        model.addOnLoadListener { item, event, reason ->
            publishLoadState(item)
            val detail = if (reason.isNullOrEmpty()) event.name else "${event.name}: $reason"
            Toast.makeText(context, "${item.modelId ?: modelId} $detail", Toast.LENGTH_SHORT).show()
        }
        publishLoadState(model)
        return model
    }

    fun reload(maptecMap: MaptecMap) {
        if (loading || styleSwitching || model != null || !zoomValid) {
            return
        }
        loading = true
        scope.launch {
            val created = try {
                val httpsBytes = if (httpsSource) downloadGlb() else null
                place(maptecMap, httpsBytes)
            } catch (error: Exception) {
                Toast.makeText(
                    context,
                    error.message ?: "model create failed",
                    Toast.LENGTH_LONG,
                ).show()
                null
            }
            loading = false
            model = created
            if (created != null) {
                maptecMap.moveCamera(
                    CameraUpdateFactory.newLatLng(if (httpsSource) FLYER_POSITION else SANDS_POSITION),
                )
            }
        }
    }

    fun switchMapStyle(maptecMap: MaptecMap, dark: Boolean) {
        val styleId = if (dark) MODEL_STYLE_DARK else MODEL_STYLE_LIGHT
        if (styleSwitching || loading || darkStyle == dark) {
            return
        }
        val preservedCamera = maptecMap.cameraPosition
        val previous = model
        styleSwitching = true
        darkStyle = dark
        // setStyle 会清掉引擎里的覆盖物，但 Java 侧仍按 modelId 占着当前模型。
        // 不先 deleteModel，后面的 addModel 会报 already exists，界面引用却已经是 null。
        if (previous != null) {
            maptecMap.overlayEngine.deleteModel(previous)
            publishLoadState(previous)
        }
        model = null
        maptecMap.setStyle(StyleOption(styleId), object : StyleStatusCallback {
            override fun onStyleLoaded(style: Style?) {}

            override fun onStyleRendered(style: Style?) {
                mainHandler.post {
                    scope.launch {
                        val created = if (previous == null || !zoomValid) {
                            null
                        } else try {
                            val httpsBytes = if (httpsSource) downloadGlb() else null
                            place(maptecMap, httpsBytes)
                        } catch (error: Exception) {
                            Toast.makeText(
                                context,
                                error.message ?: "model create failed",
                                Toast.LENGTH_LONG,
                            ).show()
                            null
                        }
                        maptecMap.moveCamera(CameraUpdateFactory.newCameraPosition(preservedCamera))
                        styleSwitching = false
                        model = created
                    }
                }
            }

            override fun onFailed(style: Style?, message: String) {
                mainHandler.post {
                    styleSwitching = false
                    darkStyle = !dark
                    showMapStyleLoadError(context, message)
                }
            }
        })
    }

    val camera = remember {
        CameraPosition.Builder()
            .target(SANDS_POSITION)
            .zoom(16.0)
            .tilt(55.0)
            .bearing(200.0)
            .build()
    }

    DemoPanelScaffold(
        sheetContent = {
            DemoPanelColumn {
                DemoPanelSwitchRow(
                    label = stringResource(
                        R.string.point_cluster_input_map_style_label,
                        stringResource(
                            if (darkStyle) R.string.map_style_dark else R.string.map_style_light,
                        ),
                    ),
                    checked = darkStyle,
                    onCheckedChange = { dark ->
                        val maptecMap = map ?: return@DemoPanelSwitchRow
                        switchMapStyle(maptecMap, dark)
                    },
                    testTag = "model_switch_map_style",
                )
                Text(
                    text = when {
                        model == null && placed && !styleSwitching -> stringResource(R.string.model_status_removed)
                        model != null && !visible -> stringResource(R.string.model_status_hidden)
                        else -> stringResource(R.string.model_status_visible)
                    },
                )
                Text(
                    text = stringResource(
                        R.string.model_load_state,
                        loadState?.name ?: stringResource(R.string.model_load_state_none),
                        loadFailureReason?.takeIf { it.isNotEmpty() }?.let { " ($it)" }.orEmpty(),
                    ),
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (loadStateTrail.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.model_load_state_trail, loadStateTrail),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                DemoNumericSliderField(
                    label = stringResource(R.string.model_height_label),
                    value = height,
                    valueText = formatDemoSliderValue(height),
                    onValueChange = {
                        height = it
                        model?.setHeight(it)
                    },
                    onValueTextChange = {},
                    valueRange = 0f..80f,
                    rangeStartLabel = "0",
                    rangeEndLabel = "80",
                    modifier = Modifier.padding(top = 12.dp),
                    testTag = "model_slider_height",
                )
                DemoNumericSliderField(
                    label = stringResource(R.string.model_heading_x_label),
                    value = rotationX,
                    valueText = formatDemoSliderValue(rotationX),
                    onValueChange = {
                        rotationX = it
                        model?.setRotation(it, rotationY, rotationZ)
                    },
                    onValueTextChange = {},
                    valueRange = 0f..360f,
                    rangeStartLabel = "0",
                    rangeEndLabel = "360",
                    modifier = Modifier.padding(top = 12.dp),
                    testTag = "model_slider_heading_x",
                )
                DemoNumericSliderField(
                    label = stringResource(R.string.model_heading_y_label),
                    value = rotationY,
                    valueText = formatDemoSliderValue(rotationY),
                    onValueChange = {
                        rotationY = it
                        model?.setRotation(rotationX, it, rotationZ)
                    },
                    onValueTextChange = {},
                    valueRange = 0f..360f,
                    rangeStartLabel = "0",
                    rangeEndLabel = "360",
                    modifier = Modifier.padding(top = 12.dp),
                    testTag = "model_slider_heading_y",
                )
                DemoNumericSliderField(
                    label = stringResource(R.string.model_heading_z_label),
                    value = rotationZ,
                    valueText = formatDemoSliderValue(rotationZ),
                    onValueChange = {
                        rotationZ = it
                        model?.setRotation(rotationX, rotationY, it)
                    },
                    onValueTextChange = {},
                    valueRange = 0f..360f,
                    rangeStartLabel = "0",
                    rangeEndLabel = "360",
                    modifier = Modifier.padding(top = 12.dp),
                    testTag = "model_slider_heading_z",
                )
                DemoNumericSliderField(
                    label = stringResource(R.string.model_scale_label),
                    value = scale,
                    valueText = formatDemoSliderValue(scale),
                    onValueChange = {
                        scale = it
                        model?.setScale(it, it, it)
                    },
                    onValueTextChange = {},
                    valueRange = 1f..200f,
                    rangeStartLabel = "1",
                    rangeEndLabel = "200",
                    modifier = Modifier.padding(top = 12.dp),
                    testTag = "model_slider_scale",
                )
                DemoNumericSliderField(
                    label = stringResource(R.string.model_min_zoom_label),
                    value = minZoomText.toFloatOrNull() ?: 0f,
                    valueText = minZoomText,
                    onValueChange = {
                        minZoomText = formatDemoSliderValue(it)
                        applyModelZoom(model, minZoomText, maxZoomText)
                    },
                    onValueTextChange = {
                        minZoomText = it
                        applyModelZoom(model, minZoomText, maxZoomText)
                    },
                    valueRange = 0f..22f,
                    rangeStartLabel = "0",
                    rangeEndLabel = "22",
                    isError = minZoomError != null,
                    errorText = minZoomError,
                    modifier = Modifier.padding(top = 12.dp),
                    testTag = "model_slider_min_zoom",
                )
                DemoNumericSliderField(
                    label = stringResource(R.string.model_max_zoom_label),
                    value = maxZoomText.toFloatOrNull() ?: 22f,
                    valueText = maxZoomText,
                    onValueChange = {
                        maxZoomText = formatDemoSliderValue(it)
                        applyModelZoom(model, minZoomText, maxZoomText)
                    },
                    onValueTextChange = {
                        maxZoomText = it
                        applyModelZoom(model, minZoomText, maxZoomText)
                    },
                    valueRange = 0f..22f,
                    rangeStartLabel = "0",
                    rangeEndLabel = "22",
                    isError = maxZoomError != null,
                    errorText = maxZoomError,
                    modifier = Modifier.padding(top = 12.dp),
                    testTag = "model_slider_max_zoom",
                )
                DemoPanelSwitchRow(
                    label = stringResource(R.string.model_source_binary),
                    checked = binarySource,
                    onCheckedChange = {
                        binarySource = it
                        if (it) {
                            httpsSource = false
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp),
                    testTag = "model_switch_binary",
                )
                DemoPanelSwitchRow(
                    label = stringResource(R.string.model_source_https),
                    checked = httpsSource,
                    onCheckedChange = {
                        httpsSource = it
                        if (it) {
                            binarySource = false
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp),
                    testTag = "model_switch_https",
                )
                DemoPanelSwitchRow(
                    label = stringResource(R.string.model_visible_label),
                    checked = visible,
                    onCheckedChange = {
                        visible = it
                        model?.setVisible(it)
                    },
                    modifier = Modifier.padding(top = 8.dp),
                    testTag = "model_switch_visible",
                )
                DemoPanelButton(
                    onClick = {
                        val current = model ?: return@DemoPanelButton
                        map?.getOverlayEngine()?.deleteModel(current)
                        publishLoadState(current)
                        model = null
                    },
                    enabled = model != null,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.model_action_remove))
                }
                DemoPanelButton(
                    onClick = {
                        val maptecMap = map ?: return@DemoPanelButton
                        reload(maptecMap)
                    },
                    enabled = model == null && map != null && !loading && zoomValid,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.model_action_add))
                }
            }
        },
        content = {
            Mapview(
                modifier = Modifier.fillMaxSize(),
                cameraPosition = camera,
                onMapReady = { _, maptecMap ->
                    map = maptecMap
                },
                onStyleRendered = { _, maptecMap, _ ->
                    map = maptecMap
                    if (!placed && !styleSwitching) {
                        placed = true
                        model = runCatching { place(maptecMap, null) }.getOrElse { error ->
                            Toast.makeText(
                                context,
                                error.message ?: "model create failed",
                                Toast.LENGTH_LONG,
                            ).show()
                            null
                        }
                    }
                },
            )
        },
    )
}

private fun modelZoomError(input: String, otherInput: String, isMin: Boolean): String? {
    val trimmed = input.trim()
    val label = if (isMin) "minZoom" else "maxZoom"
    if (trimmed.isBlank()) return "请输入 $label"
    val value = trimmed.toFloatOrNull() ?: return "$label 不是数字"
    if (value !in 0f..22f) return "$label 应在 0～22 之间"
    val other = otherInput.trim().toFloatOrNull() ?: return null
    return when {
        isMin && value > other -> "minZoom 不能大于 maxZoom"
        !isMin && value < other -> "maxZoom 不能小于 minZoom"
        else -> null
    }
}

private fun applyModelZoom(model: Model?, minText: String, maxText: String) {
    val current = model ?: return
    if (modelZoomError(minText, maxText, isMin = true) != null) return
    if (modelZoomError(maxText, minText, isMin = false) != null) return
    val minZoom = minText.trim().toFloat()
    val maxZoom = maxText.trim().toFloat()
    val currentMin = current.minZoom
    val currentMax = current.maxZoom
    if (currentMax != null && minZoom > currentMax) {
        current.setMaxZoom(maxZoom)
        current.setMinZoom(minZoom)
    } else if (currentMin != null && maxZoom < currentMin) {
        current.setMinZoom(minZoom)
        current.setMaxZoom(maxZoom)
    } else {
        current.setMinZoom(minZoom)
        current.setMaxZoom(maxZoom)
    }
}
