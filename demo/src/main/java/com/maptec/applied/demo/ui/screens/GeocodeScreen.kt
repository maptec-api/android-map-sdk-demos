@file:OptIn(ExperimentalMaterial3Api::class)

package com.maptec.applied.demo.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.common.LatLngOutlinedTextField
import com.maptec.applied.demo.ui.screens.common.validateLatLng
import com.maptec.applied.demo.viewmodel.GeocodeViewModel
import com.maptec.applied.demo.viewmodel.GeocodeViewModel.Mode
import com.maptec.applied.demo.viewmodel.GeocodeViewModelFactory
import com.maptec.applied.search.model.response.GeocodeResponse

@Composable
fun GeocodeScreen() {
    val context = LocalContext.current
    val viewModel: GeocodeViewModel = viewModel(
        factory = remember(context) { GeocodeViewModelFactory(context.applicationContext) }
    )

    val mode by viewModel.mode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val response by viewModel.response.collectAsState()
    val apiSnapshot by viewModel.apiSnapshot.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    val language by viewModel.language.collectAsState()
    val region by viewModel.region.collectAsState()

    val address by viewModel.address.collectAsState()
    val components by viewModel.components.collectAsState()
    val enableBiasRectangle by viewModel.enableBiasRectangle.collectAsState()
    val rectNorthEast by viewModel.rectNorthEast.collectAsState()
    val rectSouthWest by viewModel.rectSouthWest.collectAsState()

    val reverseLocation by viewModel.reverseLocation.collectAsState()
    val reverseResultType by viewModel.reverseResultType.collectAsState()
    val reverseLocationType by viewModel.reverseLocationType.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        TabRow(selectedTabIndex = mode.ordinal) {
            Mode.entries.forEach { m ->
                Tab(
                    selected = mode == m,
                    onClick = { viewModel.switchMode(m) },
                    text = { Text(m.label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        apiSnapshot?.let { snapshot ->
            ApiResponseDebugCard(
                selectedApiName = snapshot.apiName,
                responseJson = snapshot.responseJson
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = language,
            onValueChange = viewModel::updateLanguage,
            label = { Text(stringResource(R.string.geocode_label_language)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = region,
            onValueChange = viewModel::updateRegion,
            label = { Text(stringResource(R.string.geocode_label_region)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        when (mode) {
            Mode.FORWARD -> {
                OutlinedTextField(
                    value = address,
                    onValueChange = viewModel::updateAddress,
                    label = { Text(stringResource(R.string.geocode_label_address)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = components,
                    onValueChange = viewModel::updateComponents,
                    label = { Text(stringResource(R.string.geocode_label_components)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.geocode_label_location_bias))
                    Switch(
                        checked = enableBiasRectangle,
                        onCheckedChange = viewModel::updateEnableBiasRectangle,
                        modifier = Modifier.testTag("locationBiasSwitch")
                    )
                }

                if (enableBiasRectangle) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LatLngOutlinedTextField(
                        value = rectNorthEast,
                        onValueChange = viewModel::updateRectNorthEast,
                        label = stringResource(R.string.geocode_label_northeast),
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "geocode_input_rect_ne"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LatLngOutlinedTextField(
                        value = rectSouthWest,
                        onValueChange = viewModel::updateRectSouthWest,
                        label = stringResource(R.string.geocode_label_southwest),
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "geocode_input_rect_sw"
                    )
                }
            }

            Mode.REVERSE -> {
                LatLngOutlinedTextField(
                    value = reverseLocation,
                    onValueChange = viewModel::updateReverseLocation,
                    label = stringResource(R.string.geocode_label_location),
                    hint = "1.46878,103.80373",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = reverseResultType,
                    onValueChange = viewModel::updateReverseResultType,
                    label = { Text(stringResource(R.string.geocode_label_result_type)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = reverseLocationType,
                    onValueChange = viewModel::updateReverseLocationType,
                    label = { Text(stringResource(R.string.geocode_label_location_type)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val submitEnabled = when (mode) {
            Mode.FORWARD -> !isLoading && (!enableBiasRectangle
                    || (validateLatLng(rectNorthEast, required = true) == null && validateLatLng(rectSouthWest, required = true) == null))
            Mode.REVERSE -> !isLoading && validateLatLng(reverseLocation, required = true) == null
        }
        Button(
            onClick = viewModel::submit,
            enabled = submitEnabled,
            modifier = Modifier.fillMaxWidth().testTag("geocode_submit_button")
        ) {
            Text(if (isLoading) stringResource(R.string.geocode_loading) else stringResource(R.string.geocode_submit))
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        errorMessage?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("geocode_error_message"),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        response?.let { r ->
            ResponseSection(r)
        }
    }
}

@Composable
private fun ResponseSection(response: GeocodeResponse) {
    Text("status: ${response.status}")
    response.error?.let { err ->
        Spacer(modifier = Modifier.height(4.dp))
        Text("error: ${err.code} ${err.message}", color = MaterialTheme.colorScheme.error)
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (response.results == null || response.results?.isEmpty() == true) {
        Text("results: []")
        return
    }

    response.results?.forEachIndexed { index, item ->
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("#${index + 1}")
                item.formattedAddress?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(it)
                }
                item.placeId?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("placeId: $it")
                }
                val loc = item.geometry?.location
                if (loc != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("location: ${loc.latitude},${loc.longitude}")
                }
                item.geometry?.locationType?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("locationType: $it")
                }
                item.matchDetails?.matchScore?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("matchScore: $it")
                }
                item.types?.let { types ->
                    if (types.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("types: ${types.joinToString(",")}")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ApiResponseDebugCard(
    selectedApiName: String,
    responseJson: String
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("api_response_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "${stringResource(R.string.geocode_api_response_title)}($selectedApiName)", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) stringResource(R.string.geocode_collapse) else stringResource(R.string.geocode_expand))
                }
            }
            if (expanded) {
                val scroll = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(scroll)
                ) {
                    Text(
                        text = responseJson,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
