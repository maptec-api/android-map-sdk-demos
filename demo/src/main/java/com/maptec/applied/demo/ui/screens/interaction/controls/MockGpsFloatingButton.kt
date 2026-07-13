// DEBUG: remove after Overlay verification
package com.maptec.applied.demo.ui.screens.interaction.controls

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.maptec.applied.demo.R

@Composable
fun MockGpsFloatingButton(
    isPlaying: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .testTag("mock_gps_playback_button"),
        colors = ButtonDefaults.buttonColors()
    ) {
        Text(
            text = stringResource(
                if (isPlaying) R.string.location_mock_gps_stop else R.string.location_mock_gps_start
            )
        )
    }
}
