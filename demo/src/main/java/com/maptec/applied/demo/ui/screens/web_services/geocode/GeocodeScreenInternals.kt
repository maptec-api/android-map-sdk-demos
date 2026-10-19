@file:OptIn(ExperimentalMaterial3Api::class)

package com.maptec.applied.demo.ui.screens.web_services.geocode

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.common.WebServicePanel
import com.maptec.applied.demo.viewmodel.GeocodeViewModel
import com.maptec.applied.demo.viewmodel.GeocodeViewModel.Mode
import com.maptec.applied.demo.viewmodel.GeocodeViewModelFactory
import com.maptec.applied.search.model.response.GeocodeResponse
import com.maptec.applied.search.model.response.GeocodeResult

@Composable
internal fun GeocodeScreenScaffold(
    mode: Mode,
    formContent: @Composable (GeocodeViewModel) -> Unit,
    submitEnabled: @Composable (GeocodeViewModel) -> Boolean,
) {
    val context = LocalContext.current
    val viewModel: GeocodeViewModel = viewModel(
        factory = remember(context) { GeocodeViewModelFactory(context.applicationContext) },
    )

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val response by viewModel.response.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val language by viewModel.language.collectAsState()
    val region by viewModel.region.collectAsState()

    val scrollState = rememberScrollState()
    var advancedExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(mode) {
        viewModel.switchMode(mode)
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFD))
            .verticalScroll(scrollState)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // JSON 响应卡片已在 UI 层隐藏，ViewModel 仍保留响应数据。
        // apiSnapshot?.let { snapshot ->
        //     WebServiceApiResponseCard(
        //         titlePrefixRes = R.string.geocode_api_response_title,
        //         selectedApiName = snapshot.apiName,
        //         responseJson = snapshot.responseJson,
        //         collapseRes = R.string.geocode_collapse,
        //         expandRes = R.string.geocode_expand,
        //     )
        // }

        WebServicePanel {
            formContent(viewModel)

            GeocodeAdvancedSection(
                expanded = advancedExpanded,
                onToggle = { advancedExpanded = !advancedExpanded },
                language = language,
                region = region,
                onLanguageChange = viewModel::updateLanguage,
                onRegionChange = viewModel::updateRegion,
            )

            val enabled = submitEnabled(viewModel)
            Button(
                onClick = viewModel::submit,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("geocode_submit_button"),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
            ) {
                Text(
                    if (isLoading) {
                        stringResource(R.string.geocode_loading)
                    } else {
                        stringResource(R.string.geocode_submit)
                    },
                )
            }
        }

        errorMessage?.let { msg ->
            WebServicePanel {
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("geocode_error_message"),
                )
            }
        }

        response?.let { r ->
            GeocodeResponseSection(r)
        }
    }
}

@Composable
private fun GeocodeAdvancedSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    language: String,
    region: String,
    onLanguageChange: (String) -> Unit,
    onRegionChange: (String) -> Unit,
) {
    OutlinedButton(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFDDE5F2)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
    ) {
        Text(
            if (expanded) {
                stringResource(R.string.search_advanced_less)
            } else {
                stringResource(R.string.search_advanced_more)
            },
        )
    }

    if (expanded) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFF8FAFD),
            border = BorderStroke(1.dp, Color(0xFFE3EAF4)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = language,
                    onValueChange = onLanguageChange,
                    label = { Text(stringResource(R.string.geocode_label_language)) },
                    modifier = Modifier.weight(1f).testTag("geocode_input_language"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = region,
                    onValueChange = onRegionChange,
                    label = { Text(stringResource(R.string.geocode_label_region)) },
                    modifier = Modifier.weight(1f).testTag("geocode_input_region"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }
    }
}

@Composable
internal fun GeocodeResponseSection(response: GeocodeResponse) {
    Text(
        text = "status: ${response.status}",
        modifier = Modifier.testTag("geocode_response_status"),
    )
    response.error?.let { err ->
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "error: ${err.code} ${err.message}",
            color = MaterialTheme.colorScheme.error,
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (response.results == null || response.results?.isEmpty() == true) {
        Text("results: []")
        return
    }

    GeocodeResultsList(response.results.orEmpty())
}

@Composable
private fun GeocodeResultsList(results: List<GeocodeResult>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        results.forEachIndexed { index, item ->
            GeocodeResultItem(item = item, index = index)
        }
    }
}

@Composable
private fun GeocodeResultItem(item: GeocodeResult, index: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("geocode_result_item"),
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
                text = "#${index + 1}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
            )
            item.formattedAddress?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                )
            }
            item.placeId?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "placeId: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569),
                )
            }
            val loc = item.geometry?.location
            if (loc != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "location: ${loc.latitude},${loc.longitude}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569),
                )
            }
            item.geometry?.locationType?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "locationType: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569),
                )
            }
            item.matchDetails?.matchScore?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "matchScore: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569),
                )
            }
            item.types?.let { types ->
                if (types.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "types: ${types.joinToString(",")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF475569),
                    )
                }
            }
        }
    }
}
