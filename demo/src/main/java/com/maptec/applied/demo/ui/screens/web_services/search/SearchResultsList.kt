package com.maptec.applied.demo.ui.screens.web_services.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maptec.applied.demo.R
import com.maptec.applied.search.model.response.Place

/**
 * 搜索结果列表
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
    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(places) { place ->
                PlaceItem(
                    place = place,
                    onClick = { onPlaceClick(place) },
                )
            }
            if (canLoadMore || isLoadingMore) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onLoadMore,
                        enabled = !isLoadingMore,
                        modifier = Modifier.fillMaxWidth(),
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
                text = place.displayName.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = place.types.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475569),
            )
        }
    }
}
