package com.maptec.applied.demo.ui.screens.interaction

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.ui.screens.common.DemoPanelButton
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.ui.screens.common.LocalDemoConfigPanelController
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MaptecMap
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicReference

/**
 * 地图在线截屏 Demo：演示 [MaptecMap.snapshot]、保存到相册与分享。
 */
@Composable
fun MapSnapshotScreen() {
    val context = LocalContext.current
    val configPanelController = LocalDemoConfigPanelController.current
    var maptecMap by remember { mutableStateOf<MaptecMap?>(null) }
    var snapshotBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val heldBitmap = remember { AtomicReference<Bitmap?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf(context.getString(R.string.map_snapshot_status_idle)) }

    LaunchedEffect(Unit) {
        snapshotFlow { configPanelController.available }.first { it }
        if (!configPanelController.expanded) {
            configPanelController.toggle()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            heldBitmap.getAndSet(null)?.takeIf { !it.isRecycled }?.recycle()
        }
    }

    DemoPanelScaffold(
        sheetContent = {
            DemoPanelColumn {
                MapSnapshotControlPanel(
                    statusText = statusText,
                    isCapturing = isCapturing,
                    snapshotBitmap = snapshotBitmap,
                    hasSnapshot = snapshotBitmap != null,
                    onCapture = {
                        val map = maptecMap
                        if (map == null || isCapturing) return@MapSnapshotControlPanel
                        isCapturing = true
                        map.snapshot { bitmap ->
                            // SDK 回调不保证主线程；更新 Compose / Toast 需自行切回 UI 线程
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                isCapturing = false
                                if (bitmap == null) {
                                    statusText = context.getString(R.string.map_snapshot_status_failed)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.map_snapshot_status_failed),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    return@post
                                }
                                val previous = heldBitmap.getAndSet(bitmap)
                                snapshotBitmap = bitmap
                                previous?.takeIf { it !== bitmap && !it.isRecycled }?.recycle()
                                statusText = context.getString(
                                    R.string.map_snapshot_status_success,
                                    bitmap.width,
                                    bitmap.height,
                                )
                            }
                        }
                    },
                    onSave = {
                        val bitmap = snapshotBitmap
                        if (bitmap == null || bitmap.isRecycled) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.map_snapshot_toast_need_capture),
                                Toast.LENGTH_SHORT,
                            ).show()
                            return@MapSnapshotControlPanel
                        }
                        val uri = saveSnapshotToGallery(context, bitmap)
                        Toast.makeText(
                            context,
                            context.getString(
                                if (uri != null) {
                                    R.string.map_snapshot_toast_save_ok
                                } else {
                                    R.string.map_snapshot_toast_save_fail
                                },
                            ),
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    onShare = {
                        val bitmap = snapshotBitmap
                        if (bitmap == null || bitmap.isRecycled) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.map_snapshot_toast_need_capture),
                                Toast.LENGTH_SHORT,
                            ).show()
                            return@MapSnapshotControlPanel
                        }
                        val uri = saveSnapshotToGallery(context, bitmap)
                        if (uri == null) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.map_snapshot_toast_share_fail),
                                Toast.LENGTH_SHORT,
                            ).show()
                            return@MapSnapshotControlPanel
                        }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val chooser = Intent.createChooser(
                            shareIntent,
                            context.getString(R.string.map_snapshot_share_chooser),
                        )
                        val activity = context.findActivity()
                        if (activity != null) {
                            activity.startActivity(chooser)
                        } else {
                            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(chooser)
                        }
                    },
                )
            }
        },
        content = {
            Mapview(
                modifier = Modifier.fillMaxSize(),
                cameraPosition = CameraPosition.Builder()
                    .target(LatLng(1.3521, 103.8198))
                    .zoom(14.0)
                    .build(),
                onMapReady = { _, map ->
                    maptecMap = map
                },
            )
        },
    )
}

@Composable
private fun MapSnapshotControlPanel(
    statusText: String,
    isCapturing: Boolean,
    snapshotBitmap: Bitmap?,
    hasSnapshot: Boolean,
    onCapture: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.map_snapshot_sheet_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("text_snapshot_status"),
        )
        DemoPanelButton(
            onClick = onCapture,
            enabled = !isCapturing,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("button_snapshot_capture"),
        ) {
            Text(
                text = stringResource(
                    if (isCapturing) {
                        R.string.map_snapshot_capturing
                    } else {
                        R.string.map_snapshot_capture
                    },
                ),
            )
        }
        DemoPanelButton(
            onClick = onSave,
            enabled = hasSnapshot && !isCapturing,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("button_snapshot_save"),
        ) {
            Text(text = stringResource(R.string.map_snapshot_save))
        }
        DemoPanelButton(
            onClick = onShare,
            enabled = hasSnapshot && !isCapturing,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("button_snapshot_share"),
        ) {
            Text(text = stringResource(R.string.map_snapshot_share))
        }
        if (snapshotBitmap != null && !snapshotBitmap.isRecycled) {
            Text(
                text = stringResource(R.string.map_snapshot_preview_label),
                style = MaterialTheme.typography.labelLarge,
            )
            Image(
                bitmap = snapshotBitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.map_snapshot_preview_label),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(2.dp)
                    .testTag("image_snapshot_preview"),
            )
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun saveSnapshotToGallery(context: Context, bitmap: Bitmap): Uri? {
    val displayName = "maptec_snapshot_${System.currentTimeMillis()}.png"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/MaptecDemo",
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    return try {
        resolver.openOutputStream(uri)?.use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                resolver.delete(uri, null, null)
                return null
            }
        } ?: run {
            resolver.delete(uri, null, null)
            return null
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        uri
    } catch (_: Exception) {
        runCatching { resolver.delete(uri, null, null) }
        null
    }
}
