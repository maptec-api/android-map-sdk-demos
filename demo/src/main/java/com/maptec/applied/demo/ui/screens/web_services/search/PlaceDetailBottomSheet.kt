package com.maptec.applied.demo.ui.screens.web_services.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maptec.applied.demo.R
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
                // 加载中
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (place != null) {
                // 显示地点详情
                PlaceDetailContent(place = place)
            } else {
                // 加载失败
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
 * 地点详情内容
 */
@Composable
private fun PlaceDetailContent(place: Place) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 名称
        if (place.displayName.text.isNotEmpty()) {
            DetailItem(
                label = stringResource(R.string.search_detail_name),
                value = place.displayName.text
            )
        }

        // 地址
        place.formattedAddress?.let { address ->
            if (address.isNotEmpty()) {
                DetailItem(
                    label = stringResource(R.string.search_detail_address),
                    value = address
                )
            }
        }

        // 电话
        place.phoneNumber?.let { phone ->
            if (phone.isNotEmpty()) {
                DetailItem(
                    label = stringResource(R.string.search_detail_phone),
                    value = phone
                )
            }
        }

        // 类型
        if (place.types.isNotEmpty()) {
            DetailItem(
                label = stringResource(R.string.search_detail_type_label),
                value = place.types.joinToString(", ")
            )
        }

        // 坐标
        place.location?.let { location ->
            DetailItem(
                label = stringResource(R.string.search_detail_coordinate),
                value = "纬度: ${location.latitude}, 经度: ${location.longitude}"
            )
        } ?: DetailItem(label = stringResource(R.string.search_detail_coordinate), value = stringResource(R.string.search_detail_coordinate_none))

        // 简介
        place.summary?.text?.let { summary ->
            if (summary.isNotEmpty()) {
                DetailItem(
                    label = stringResource(R.string.search_detail_summary),
                    value = summary
                )
            }
        }
    }
}

/**
 * 详情项组件
 */
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