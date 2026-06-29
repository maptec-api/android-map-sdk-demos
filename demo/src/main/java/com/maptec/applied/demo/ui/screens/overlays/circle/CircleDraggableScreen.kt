package com.maptec.applied.demo.ui.screens.overlays.circle

import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.maptec.applied.demo.LOG_MODULE
import com.maptec.applied.demo.R
import com.maptec.applied.javabase.log.LoggerFactory
import com.maptec.applied.maps.overlay.OnOverlayDragListener
import com.maptec.applied.maps.overlay.circle.Circle
import com.maptec.applied.maps.overlay.circle.CircleOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleDraggableScreen() {
    val state = rememberBasicCircleState()
    val context = LocalContext.current
    var draggable by remember { mutableStateOf(true) }

    CircleScaffold { mapView, _, scaffoldState ->
        CirclePanelColumn {
            BasicCircleInputs(state)
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.circle_switch_draggable), modifier = Modifier.weight(1f))
                Switch(
                    checked = draggable,
                    onCheckedChange = { draggable = it },
                    modifier = Modifier.testTag("circle_switch_draggable"),
                )
            }
            DrawCircleButton(
                state = state,
                mapView = mapView,
                scaffoldState = scaffoldState,
                buildOptions = { state.applyTo(CircleOptions(), draggable = draggable) },
                onCircleAdded = { circle, _ ->
                    circle.addOnDragListener(object : OnOverlayDragListener<Circle> {
                        override fun onAnnotationDragStarted(circle: Circle) {
                            LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").d { "onAnnotationDragStarted" }
                        }

                        override fun onAnnotationDrag(circle: Circle) {
                            LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").d { "onAnnotationDrag" }
                        }

                        override fun onAnnotationDragFinished(circle: Circle) {
                            LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").d { "onAnnotationDragFinished" }
                            Toast.makeText(
                                context,
                                context.getString(R.string.circle_toast_drag_finished, circle.id),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    })
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
