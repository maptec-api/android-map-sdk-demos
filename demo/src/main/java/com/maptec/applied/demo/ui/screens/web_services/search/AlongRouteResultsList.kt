package com.maptec.applied.demo.ui.screens.web_services.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.maptec.applied.search.model.response.AlongRouteResult

@Composable
fun AlongRouteResultsList(
    results: List<AlongRouteResult>,
    onResultClick: (AlongRouteResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        results.forEach { result ->
            AlongRouteResultItem(
                result = result,
                onClick = { onResultClick(result) },
            )
        }
    }
}

@Composable
private fun AlongRouteResultItem(
    result: AlongRouteResult,
    onClick: () -> Unit,
) {
    val place = result.place
    val relation = result.routeRelation
    val evaluation = result.routeEvaluation

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("search_along_route_item")
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = place.displayName?.text ?: "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            place.formattedAddress?.takeIf { it.isNotBlank() }?.let { address ->
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }
            place.types?.takeIf { it.isNotEmpty() }?.let { types ->
                Text(
                    text = types.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4A5568),
                )
            }
            Text(
                text = stringResource(
                    R.string.search_along_route_relation_summary,
                    relation.distanceFromRouteMeters,
                    relation.routeOffsetMeters,
                    relation.routeProgressRatio * 100,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF2B6CB0),
            )
            evaluation?.let { eval ->
                Text(
                    text = stringResource(
                        R.string.search_along_route_evaluation_summary,
                        eval.estimatedDurationSeconds,
                        eval.estimatedDistanceMeters,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2F855A),
                )
            }
        }
    }
}
