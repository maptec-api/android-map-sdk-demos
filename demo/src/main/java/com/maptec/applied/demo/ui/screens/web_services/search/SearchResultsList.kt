package com.maptec.applied.demo.ui.screens.web_services.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.maptec.applied.demo.R
import com.maptec.applied.search.model.response.GeoJson
import com.maptec.applied.search.model.response.Place

/**
 * 搜索结果列表 —— 使用 Column 而非 LazyColumn，以便参与外层整页滚动。
 */
@Composable
fun SearchResultsList(
    places: List<Place>,
    onPlaceClick: (Place) -> Unit,
    modifier: Modifier = Modifier,
    canLoadMore: Boolean = false,
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        places.forEach { place ->
            PlaceItem(
                place = place,
                onClick = { onPlaceClick(place) },
            )
        }
        if (canLoadMore || isLoadingMore) {
            Spacer(modifier = Modifier.height(2.dp))
            Button(
                onClick = onLoadMore,
                enabled = !isLoadingMore,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isLoadingMore) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.heightIn(max = 18.dp),
                    )
                } else {
                    Text(stringResource(R.string.search_load_more))
                }
            }
        }
    }
}

/**
 * 地点项组件
 */
@Composable
fun PlaceItem(
    place: Place,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("search_place_item")
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = place.displayName?.text ?: "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
            )
            Spacer(modifier = Modifier.height(4.dp))
            place.types?.takeIf { it.isNotEmpty() }?.let { types ->
                Text(
                    text = types.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569),
                )
            }
            place.formattedAddress?.takeIf { it.isNotEmpty() }?.let { address ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                )
            }
            PlaceExtraFieldRows(place = place)
        }
    }
}

@Composable
private fun PlaceExtraFieldRows(place: Place) {
    place.aoi?.let { aoi ->
        val aoiValue = buildString {
            append("id=").append(aoi.id)
            append(", name=").append(aoi.name)
            aoi.geojson?.let { geo ->
                append(", type=").append(geo.type)
                append(", coordinates=").append(formatGeoJsonCoordinates(geo))
            }
        }
        ExtraFieldRow(label = "aoi", value = aoiValue)
    }
    place.phoneNumbers?.takeIf { it.isNotEmpty() }?.let { phones ->
        ExtraFieldRow(label = "phoneNumbers", value = phones.joinToString(", "))
    }
    place.emailAddresses?.takeIf { it.isNotEmpty() }?.let { emails ->
        ExtraFieldRow(label = "emailAddresses", value = emails.joinToString(", "))
    }
    place.websiteUris?.takeIf { it.isNotEmpty() }?.let { uris ->
        ExtraFieldRow(label = "websiteUris", value = uris.joinToString(", "))
    }
    place.summary?.let { summary ->
        val parts = buildList {
            (summary.text as String?)?.takeIf { it.isNotBlank() }?.let { add("text=$it") }
            (summary.languageCode as String?)?.takeIf { it.isNotBlank() }?.let { add("languageCode=$it") }
        }
        if (parts.isNotEmpty()) {
            ExtraFieldRow(label = "summary", value = parts.joinToString(", "))
        }
    }
    place.openingHours?.let { hours ->
        val hoursText = buildString {
            append(if (hours.openNow) "openNow=true" else "openNow=false")
            hours.weekdayText
                ?.mapNotNull { (it as String?)?.takeIf { s -> s.isNotBlank() } }
                ?.takeIf { it.isNotEmpty() }
                ?.let { append("; ").append(it.joinToString(" | ")) }
        }
        ExtraFieldRow(label = "openingHours", value = hoursText)
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
private fun ExtraFieldRow(label: String, value: String) {
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                ),
            ) {
                append(label)
            }
            withStyle(SpanStyle(color = Color(0xFF475569))) {
                append(": ")
                append(value)
            }
        },
        style = MaterialTheme.typography.bodySmall,
    )
}
