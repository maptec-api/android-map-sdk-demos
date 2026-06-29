package com.maptec.applied.demo.ui.screens.map

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.demo.Constants
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.defaultDemoMapOptions
import com.maptec.applied.demo.map.MapViewLifecycleEffect
import com.maptec.applied.demo.map.showMapStyleLoadError
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.demo.LanguageType
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition = CameraPosition.Builder().target(LatLng(1.4, 103.75)).zoom(16.0)
        .tilt(40.0).build(),
    onMapReady: (MapView, MaptecMap) -> Unit = { _, _ -> },
) {
    var fps by remember { mutableStateOf("") }
    var debugActive by remember { mutableStateOf(false) }
    var skyEnabled by remember { mutableStateOf(false) }
    var languageTag by remember { mutableStateOf(LanguageType.DEFAULT) }
    var maptecMap by remember { mutableStateOf<MaptecMap?>(null) }
    var isStyleRendered by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val mapView = remember(cameraPosition) {
        val options = defaultDemoMapOptions(context).apply {
            zoomButtonsEnabled(false)
            camera(cameraPosition)
        }

        MapView(context, options).apply {
            onCreate(null)
            getMapAsync { map ->
                maptecMap = map

                map.setStyle(StyleOption(Constants.DEFAULT_STYLE_ID), object : StyleStatusCallback {
                    override fun onStyleLoaded(style: Style?) {
                        map.setOnFpsChangedListener { _fps ->
                            val cur = map.cameraPosition
                            fps = "fps:" + _fps.toInt() +
                                " scale:" + cur.zoom.toInt() +
                                " tilt:" + cur.tilt.toInt()
                        }
                    }

                    override fun onStyleRendered(style: Style?) {
                        isStyleRendered = true
                    }

                    override fun onFailed(style: Style?, message: String) {
                        showMapStyleLoadError(context, message)
                    }
                })
                onMapReady(this, map)
                addOnMapHttpErrorListener { code, message ->
                    if (code in 10006..10049) {
                        Toast.makeText(
                            context,
                            "服务端返回错误码：$code 错误信息：$message",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
        }
    }

    MapViewLifecycleEffect(mapView)

    val scaffoldState = rememberBottomSheetScaffoldState()

    LaunchedEffect(Unit) {
        scaffoldState.bottomSheetState.partialExpand()
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 48.dp,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            MapRenderBottomPanel(
                skyEnabled = skyEnabled,
                onSkyEnabledChange = { enabled ->
                    skyEnabled = enabled
                    maptecMap?.setSkyEnabled(enabled)
                },
                languageTag = languageTag,
                onLanguageChange = { tag ->
                    languageTag = tag
                    maptecMap?.setLanguage(tag)
                },
            )
        },
        content = { padding ->
            Box(modifier = modifier.fillMaxSize().padding(padding)) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("mapView"),
                    factory = { mapView.apply { tag = "mapView" } },
                    update = {},
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                ) {
                    Text(
                        text = fps,
                        color = Color.Red,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.background(Color.Transparent),
                    )
                }

                if (isStyleRendered) {
                    Box(modifier = Modifier.testTag("mapRendered"))
                }

                Button(
                    onClick = {
                        maptecMap?.let { map ->
                            debugActive = !map.isDebugActive
                            map.setDebugActive(debugActive)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .testTag("map_btn_debug_toggle"),
                ) {
                    Text(text = if (debugActive) "关闭调试" else "开启调试")
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapRenderBottomPanel(
    skyEnabled: Boolean,
    onSkyEnabledChange: (Boolean) -> Unit,
    languageTag: String,
    onLanguageChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.map_sky_title))
                Text(
                    text = stringResource(R.string.map_sky_description),
                    color = Color.Gray,
                    fontSize = 12.sp,
                )
            }
            Switch(
                checked = skyEnabled,
                onCheckedChange = onSkyEnabledChange,
                modifier = Modifier.testTag("map_switch_sky_enabled"),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "提示：拉到高 zoom (>=18) 并双指 pitch，观察顶部天空开关效果。",
            color = Color.Gray,
            fontSize = 12.sp,
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "标签语言")
                Text(
                    text = "BCP 47 标签，名称字段不存在时回退到 name",
                    color = Color.Gray,
                    fontSize = 12.sp,
                )
            }
        }
        var expanded by remember { mutableStateOf(false) }
        val options = LanguageOptions
        val current = options.firstOrNull { it.tag == languageTag } ?: options.first()
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = current.label,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .testTag("map_language_dropdown"),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt.label) },
                        onClick = {
                            expanded = false
                            onLanguageChange(opt.tag)
                        },
                        modifier = Modifier.testTag(languageOptionTestTag(opt.tag)),
                    )
                }
            }
        }
    }
}

private data class LanguageOption(val label: String, val tag: String)

internal fun languageOptionTestTag(tag: String): String =
    "map_language_option_${tag.ifEmpty { "default" }}"

private val LanguageOptions = listOf(
    LanguageOption("默认 (矢量瓦片 name)", LanguageType.DEFAULT),
    LanguageOption("英语 (en)", LanguageType.ENGLISH),
    LanguageOption("中文 (zh)", LanguageType.CHINESE),
    LanguageOption("马来语 (ms)", LanguageType.MALAY),
    LanguageOption("泰语 (th)", LanguageType.THAI),
    LanguageOption("阿拉伯语 (ar)", LanguageType.ARABIC),
)
