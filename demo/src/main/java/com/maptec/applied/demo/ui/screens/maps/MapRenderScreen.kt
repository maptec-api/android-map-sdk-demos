package com.maptec.applied.demo.ui.screens.maps

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.demo.Constants
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.MapViewLifecycleEffect
import com.maptec.applied.demo.map.defaultDemoMapOptions
import com.maptec.applied.demo.map.showMapStyleLoadError
import com.maptec.applied.demo.ui.screens.common.DemoDropdownMenuItem
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback

private const val LANGUAGE_TAG_EN = "en"
private const val LANGUAGE_TAG_ZH = "zh"

/** BCP 47 tags for [MaptecMap.setLanguage]; independent from AppBar UI locale. */
private const val MAP_LABEL_LANG_DEFAULT = ""

@Composable
fun MapRenderScreen(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition = CameraPosition.Builder()
        .target(LatLng(1.360879, 103.732578))
        .zoom(16.0)
        .tilt(40.0)
        .build(),
    onMapReady: (MapView, MaptecMap) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val initialLanguage = remember {
        when (context.resources.configuration.locales[0].language) {
            "zh" -> LANGUAGE_TAG_ZH
            else -> LANGUAGE_TAG_EN
        }
    }

    var languageTag by remember { mutableStateOf(initialLanguage) }
    var maptecMap by remember { mutableStateOf<MaptecMap?>(null) }
    var isStyleRendered by remember { mutableStateOf(false) }

    val mapView = remember(cameraPosition) {
        val options = defaultDemoMapOptions(context).apply {
            zoomButtonsEnabled(false)
            camera(cameraPosition)
        }

        MapView(context, options).apply {
            onCreate(null)
            getMapAsync { map ->
                maptecMap = map
                map.setLanguage(languageTag)
                map.setStyle(StyleOption(Constants.DEFAULT_STYLE_ID), object : StyleStatusCallback {
                    override fun onStyleLoaded(style: Style?) {}

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
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                context,
                                context.getString(R.string.map_error_message_format, code, message),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }
            }
        }
    }

    MapViewLifecycleEffect(mapView)

    DemoPanelScaffold(
        sheetContent = {
            DemoPanelColumn {
                MapRenderBottomPanel(
                    languageTag = languageTag,
                    onLanguageChange = { tag ->
                        languageTag = tag
                        maptecMap?.setLanguage(tag)
                    },
                )
            }
        },
        content = {
            Box(modifier = modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("mapView"),
                    factory = { mapView.apply { tag = "mapView" } },
                    update = {},
                )

                if (isStyleRendered) {
                    Box(modifier = Modifier.testTag("mapRendered"))
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapRenderBottomPanel(
    languageTag: String,
    onLanguageChange: (String) -> Unit,
) {
    Text(text = stringResource(R.string.map_language_label))
    Text(
        text = stringResource(R.string.map_language_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    var expanded by remember { mutableStateOf(false) }
    val options = mapLanguageOptions()
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
                DemoDropdownMenuItem(
                    text = opt.label,
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

private data class LanguageOption(val label: String, val tag: String)

@Composable
private fun mapLanguageOptions(): List<LanguageOption> = listOf(
    LanguageOption(stringResource(R.string.map_lang_default), MAP_LABEL_LANG_DEFAULT),
    LanguageOption(stringResource(R.string.map_lang_en), "en"),
    LanguageOption(stringResource(R.string.map_lang_zh), "zh"),
    LanguageOption(stringResource(R.string.map_lang_ms), "ms"),
    LanguageOption(stringResource(R.string.map_lang_th), "th"),
    LanguageOption(stringResource(R.string.map_lang_ar), "ar"),
)

internal fun languageOptionTestTag(tag: String): String =
    "map_language_option_${tag.ifEmpty { "default" }}"
