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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maptec.applied.demo.R
import com.maptec.applied.search.model.response.Suggestion

/**
 * 提示词列表 —— 使用 Column 而非 LazyColumn，以便参与外层整页滚动。
 */
@Composable
fun SuggestionsList(
    suggestions: List<Suggestion>,
    onSuggestionClick: (Suggestion) -> Unit,
    modifier: Modifier = Modifier,
    canLoadMore: Boolean = false,
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        suggestions.forEach { suggestion ->
            SuggestionItem(
                suggestion = suggestion,
                onClick = { onSuggestionClick(suggestion) },
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
 * 提示词项组件
 */
@Composable
private fun SuggestionItem(
    suggestion: Suggestion,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("search_suggestion_item")
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
                text = suggestion.text.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
            )
        }
    }
}
