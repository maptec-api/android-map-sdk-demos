package com.maptec.applied.demo.ui.screens.web_services.geocode

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.common.LatLngOutlinedTextField
import com.maptec.applied.demo.ui.screens.common.validateLatLng
import com.maptec.applied.demo.viewmodel.GeocodeViewModel
import com.maptec.applied.demo.viewmodel.GeocodeViewModel.Mode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReverseGeocodeScreen() {
    GeocodeScreenScaffold(
        mode = Mode.REVERSE,
        formContent = { viewModel -> ReverseGeocodeForm(viewModel) },
        submitEnabled = { viewModel ->
            val isLoading by viewModel.isLoading.collectAsState()
            val reverseLocation by viewModel.reverseLocation.collectAsState()
            !isLoading && validateLatLng(reverseLocation, required = true) == null
        },
    )
}

@Composable
private fun ReverseGeocodeForm(viewModel: GeocodeViewModel) {
    val reverseLocation by viewModel.reverseLocation.collectAsState()
    val reverseResultType by viewModel.reverseResultType.collectAsState()
    val reverseLocationType by viewModel.reverseLocationType.collectAsState()

    LatLngOutlinedTextField(
        value = reverseLocation,
        onValueChange = viewModel::updateReverseLocation,
        label = stringResource(R.string.geocode_label_location),
        hint = "1.360879,103.732578",
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = reverseResultType,
        onValueChange = viewModel::updateReverseResultType,
        label = { Text(stringResource(R.string.geocode_label_result_type)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
    OutlinedTextField(
        value = reverseLocationType,
        onValueChange = viewModel::updateReverseLocationType,
        label = { Text(stringResource(R.string.geocode_label_location_type)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
}
