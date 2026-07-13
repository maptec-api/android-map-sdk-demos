@file:OptIn(ExperimentalMaterial3Api::class)

package com.maptec.applied.demo.ui.screens.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maptec.applied.demo.R

@Composable
fun WebServicePanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun AdvancedFieldsSurface(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFD),
        border = BorderStroke(1.dp, Color(0xFFE3EAF4)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
fun NearbyAdvancedSummary(
    expanded: Boolean,
    resultLimit: String,
    rank: String?,
    language: String,
    region: String,
    onToggle: () -> Unit,
) {
    val defaultLabel = stringResource(R.string.search_default_value)
    val notSetLabel = stringResource(R.string.search_not_set)

    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFDDE5F2)),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Icon(
                    Icons.Default.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (expanded) {
                        stringResource(R.string.search_advanced_less)
                    } else {
                        stringResource(R.string.search_advanced_more)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.weight(1f))
                androidx.compose.material3.Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            if (!expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    NearbySummaryChip(
                        "${stringResource(R.string.search_label_result_limit)}: ${resultLimit.ifBlank { defaultLabel }}",
                    )
                    NearbySummaryChip(
                        "${stringResource(R.string.search_label_rank)}: ${rank ?: defaultLabel}",
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    NearbySummaryChip(
                        "${stringResource(R.string.search_label_language)}: ${language.ifBlank { defaultLabel }}",
                    )
                    NearbySummaryChip(
                        "${stringResource(R.string.search_label_region)}: ${region.ifBlank { notSetLabel }}",
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.NearbySummaryChip(text: String) {
    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF3F7FC),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF516173),
            maxLines = 1,
        )
    }
}

@Composable
fun WebServiceApiResponseCard(
    titlePrefixRes: Int,
    selectedApiName: String,
    responseJson: String,
    collapseRes: Int = R.string.search_action_collapse,
    expandRes: Int = R.string.search_action_expand,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("api_response_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${stringResource(titlePrefixRes)}($selectedApiName)",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF46566A),
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) stringResource(collapseRes) else stringResource(expandRes))
                }
            }
            if (expanded) {
                val scroll = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(scroll),
                ) {
                    Text(text = responseJson, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
