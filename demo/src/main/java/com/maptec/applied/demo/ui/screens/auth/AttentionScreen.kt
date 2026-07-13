@file:OptIn(ExperimentalMaterial3Api::class)

package com.maptec.applied.demo.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.catalog.CatalogRoutes
import com.maptec.applied.demo.viewmodel.AttentionViewModelFactory

/**
 * 鉴权 Demo 页面。
 * 提供 apiKey 与 Signature SHA1 输入、按 SDK 勾选应用、各自独立确认后写入对应 SDK，以及跳转到地图/搜索/路径规划页面进行验证。
 */
@Composable
fun AttentionScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: com.maptec.applied.demo.viewmodel.AttentionViewModel = viewModel(
        factory = remember(context) { AttentionViewModelFactory(context.applicationContext) }
    )

    val apiKeyInput by viewModel.apiKeyInput.collectAsState()
    val sha1Input by viewModel.sha1Input.collectAsState()
    val searchChecked by viewModel.searchChecked.collectAsState()
    val routeChecked by viewModel.routeChecked.collectAsState()
    val mapChecked by viewModel.mapChecked.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val authStatusJson by viewModel.authStatusJson.collectAsState()
    val authStatusLoading by viewModel.authStatusLoading.collectAsState()

    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.attention_api_key_label),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = viewModel::updateApiKeyInput,
            label = { Text(stringResource(id = R.string.attention_api_key_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = viewModel::applyApiKey,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.attention_confirm_api_key))
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(id = R.string.attention_sha1_label),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = sha1Input,
            onValueChange = viewModel::updateSha1Input,
            label = { Text(stringResource(id = R.string.attention_sha1_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = viewModel::applySignatureSha1,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.attention_confirm_sha1))
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(id = R.string.attention_sdk_checkbox_title),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = searchChecked,
                onCheckedChange = viewModel::setSearchChecked
            )
            Text(stringResource(id = R.string.attention_sdk_search), Modifier.padding(start = 4.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = routeChecked,
                onCheckedChange = viewModel::setRouteChecked
            )
            Text(stringResource(id = R.string.attention_sdk_route), Modifier.padding(start = 4.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = mapChecked,
                onCheckedChange = viewModel::setMapChecked
            )
            Text(stringResource(id = R.string.attention_sdk_map), Modifier.padding(start = 4.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(id = R.string.attention_verify_title),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { navController.navigate(CatalogRoutes.ADD_MAP) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.attention_verify_map))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { navController.navigate(CatalogRoutes.TEXT_SEARCH) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.attention_verify_search))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { navController.navigate(CatalogRoutes.ROUTE) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.attention_verify_route))
        }

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = viewModel::loadAuthStatus,
            modifier = Modifier.fillMaxWidth(),
            enabled = !authStatusLoading
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (authStatusLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp).padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text(
                    if (authStatusLoading) stringResource(id = R.string.attention_loading) else stringResource(id = R.string.attention_view_authorized_services)
                )
            }
        }
        authStatusJson?.let { json ->
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("authStatusJson"),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = json,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
