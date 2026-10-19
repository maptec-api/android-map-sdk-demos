package com.maptec.applied.demo.ui.screens.web_services.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maptec.applied.demo.R
import com.maptec.applied.search.model.response.GeoJson
import com.maptec.applied.search.model.response.Place

/**
 * 地点详情底部选项卡
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailBottomSheet(
    place: Place?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    val maxSheetBodyHeight = (LocalConfiguration.current.screenHeightDp * 0.65f).dp
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.search_detail_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.search_close)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (place != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxSheetBodyHeight)
                        .verticalScroll(rememberScrollState())
                        .testTag("place_detail_bottom_sheet"),
                ) {
                    PlaceDetailContent(place = place)
                }
            } else {
                Text(
                    text = stringResource(R.string.search_detail_loading_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * 地点详情内容（地图底栏 / 地点详情页共用）。
 * 展示默认字段 + extraFields 扩展对象（aoi / phoneNumbers / summary / openingHours 等）。
 * 不加 verticalScroll：父布局可能已滚动或高度无界，由调用方决定是否滚动。
 */
@Composable
internal fun PlaceDetailContent(place: Place) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        place.displayName?.text?.takeIf { it.isNotEmpty() }?.let { name ->
            DetailItem(
                label = stringResource(R.string.search_detail_name),
                value = name
            )
        }

        place.formattedAddress?.takeIf { it.isNotEmpty() }?.let { address ->
            DetailItem(
                label = stringResource(R.string.search_detail_address),
                value = address
            )
        }

        place.phoneNumbers?.takeIf { it.isNotEmpty() }?.let { phones ->
            DetailItem(
                label = stringResource(R.string.search_detail_phone),
                value = phones.joinToString(", ")
            )
        }

        place.emailAddresses?.takeIf { it.isNotEmpty() }?.let { emails ->
            DetailItem(
                label = stringResource(R.string.search_detail_email),
                value = emails.joinToString(", ")
            )
        }

        place.websiteUris?.takeIf { it.isNotEmpty() }?.let { uris ->
            DetailItem(
                label = stringResource(R.string.search_detail_website),
                value = uris.joinToString(", ")
            )
        }

        place.types?.takeIf { it.isNotEmpty() }?.let { types ->
            DetailItem(
                label = stringResource(R.string.search_detail_type_label),
                value = types.joinToString(", ")
            )
        }

        place.location?.let { location ->
            DetailItem(
                label = stringResource(R.string.search_detail_coordinate),
                value = "纬度: ${location.latitude}, 经度: ${location.longitude}"
            )
        } ?: DetailItem(
            label = stringResource(R.string.search_detail_coordinate),
            value = stringResource(R.string.search_detail_coordinate_none)
        )

        place.summary?.let { summary ->
            val parts = buildList {
                (summary.text as String?)?.takeIf { it.isNotBlank() }?.let { add(it) }
                (summary.languageCode as String?)?.takeIf { it.isNotBlank() }?.let { add("($it)") }
            }
            if (parts.isNotEmpty()) {
                DetailItem(
                    label = stringResource(R.string.search_detail_summary),
                    value = parts.joinToString(" ")
                )
            }
        }

        place.openingHours?.let { hours ->
            val hoursText = buildString {
                append(if (hours.openNow) "openNow=true" else "openNow=false")
                hours.weekdayText
                    ?.mapNotNull { (it as String?)?.takeIf { s -> s.isNotBlank() } }
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { append("\n").append(it.joinToString("\n")) }
                (hours.nextStatusChange as String?)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { append("\nnextStatusChange=").append(it) }
            }
            DetailItem(
                label = stringResource(R.string.search_detail_opening_hours),
                value = hoursText
            )
        }

        place.aoi?.let { aoi ->
            val aoiValue = buildString {
                append("id=").append((aoi.id as String?).orEmpty())
                append("\nname=").append((aoi.name as String?).orEmpty())
                aoi.geojson?.let { geo ->
                    append("\ntype=").append((geo.type as String?).orEmpty())
                    append("\ncoordinates=").append(formatGeoJsonCoordinates(geo))
                }
            }
            DetailItem(
                label = stringResource(R.string.search_detail_aoi),
                value = aoiValue
            )
        }

        place.addressComponents?.takeIf { it.isNotEmpty() }?.let { components ->
            val text = components.joinToString("\n") { c ->
                val longName = (c.longName as String?).orEmpty()
                val shortName = (c.shortName as String?).orEmpty()
                buildString {
                    append(longName.ifBlank { shortName })
                    if (shortName.isNotBlank() && shortName != longName) {
                        append(" (").append(shortName).append(")")
                    }
                    c.types
                        .mapNotNull { (it as String?)?.takeIf { t -> t.isNotBlank() } }
                        .takeIf { it.isNotEmpty() }
                        ?.let { append(" [").append(it.joinToString(", ")).append("]") }
                }
            }
            DetailItem(
                label = stringResource(R.string.search_detail_address_components),
                value = text
            )
        }

        place.timeZone?.let { tz ->
            val tzText = buildString {
                append((tz.id as String?).orEmpty())
                (tz.version as String?)?.takeIf { it.isNotBlank() }?.let {
                    append(" (version=").append(it).append(")")
                }
            }
            DetailItem(
                label = stringResource(R.string.search_detail_timezone),
                value = tzText
            )
        }
    }
}

private fun formatGeoJsonCoordinates(geojson: GeoJson): String {
    return geojson.coordinates.joinToString(prefix = "[", postfix = "]") { ring ->
        ring.joinToString(prefix = "[", postfix = "]") { point ->
            point.joinToString(prefix = "[", postfix = "]", separator = ", ")
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
