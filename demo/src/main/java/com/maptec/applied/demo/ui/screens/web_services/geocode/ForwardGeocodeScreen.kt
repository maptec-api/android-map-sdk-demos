@file:OptIn(ExperimentalMaterial3Api::class)

package com.maptec.applied.demo.ui.screens.web_services.geocode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.common.LatLngOutlinedTextField
import com.maptec.applied.demo.ui.screens.common.validateLatLng
import com.maptec.applied.demo.viewmodel.GeocodeViewModel
import com.maptec.applied.demo.viewmodel.GeocodeViewModel.Mode

@Composable
fun ForwardGeocodeScreen() {
    GeocodeScreenScaffold(
        mode = Mode.FORWARD,
        formContent = { viewModel -> ForwardGeocodeForm(viewModel) },
        submitEnabled = { viewModel ->
            val isLoading by viewModel.isLoading.collectAsState()
            val enableBiasRectangle by viewModel.enableBiasRectangle.collectAsState()
            val rectNorthEast by viewModel.rectNorthEast.collectAsState()
            val rectSouthWest by viewModel.rectSouthWest.collectAsState()
            !isLoading && (
                !enableBiasRectangle ||
                    (
                        validateLatLng(rectNorthEast, required = true) == null &&
                            validateLatLng(rectSouthWest, required = true) == null
                        )
                )
        },
    )
}

@Composable
private fun ForwardGeocodeForm(viewModel: GeocodeViewModel) {
    val address by viewModel.address.collectAsState()
    val components by viewModel.components.collectAsState()
    val enableBiasRectangle by viewModel.enableBiasRectangle.collectAsState()
    val rectNorthEast by viewModel.rectNorthEast.collectAsState()
    val rectSouthWest by viewModel.rectSouthWest.collectAsState()

    OutlinedTextField(
        value = address,
        onValueChange = viewModel::updateAddress,
        label = { Text(stringResource(R.string.geocode_label_address)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    )
    OutlinedTextField(
        value = components,
        onValueChange = viewModel::updateComponents,
        label = { Text(stringResource(R.string.geocode_label_components)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.geocode_label_location_bias))
            Switch(
                checked = enableBiasRectangle,
                onCheckedChange = viewModel::updateEnableBiasRectangle,
                modifier = Modifier.testTag("locationBiasSwitch"),
            )
        }
    }
    if (enableBiasRectangle) {
        LatLngOutlinedTextField(
            value = rectNorthEast,
            onValueChange = viewModel::updateRectNorthEast,
            label = stringResource(R.string.geocode_label_northeast),
            modifier = Modifier.fillMaxWidth(),
            testTag = "geocode_input_rect_ne",
        )
        LatLngOutlinedTextField(
            value = rectSouthWest,
            onValueChange = viewModel::updateRectSouthWest,
            label = stringResource(R.string.geocode_label_southwest),
            modifier = Modifier.fillMaxWidth(),
            testTag = "geocode_input_rect_sw",
        )
    }
}
