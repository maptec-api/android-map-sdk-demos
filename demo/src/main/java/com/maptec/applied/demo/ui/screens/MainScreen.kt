package com.maptec.applied.demo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maptec.applied.demo.ui.screens.common.ScreenItem

@Composable
fun MainScreen(
    items: List<ScreenItem>,
    onItemClicked: (ScreenItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 5.dp, vertical = 5.dp)
    ) {
        items(items, key = { it.route }) { item ->
            ListItem(
                headlineContent = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clickable {
                    onItemClicked(item)
                }
            )
            Divider(color = DividerDefaults.color, thickness = 1.dp)
        }
    }
}

@Preview
@Composable
fun MainScreenPreview() {
    val items = listOf(
        ScreenItem("基础地图", "map"),
        ScreenItem("检索", "search"),
        ScreenItem("算路", "route"),
        ScreenItem("导航", "guide"),
    )
    MainScreen(items = items, onItemClicked = {})
}