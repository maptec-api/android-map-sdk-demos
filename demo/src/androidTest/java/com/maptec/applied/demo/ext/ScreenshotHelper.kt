package com.maptec.applied.demo.ext

import android.app.Activity
import android.app.Instrumentation
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * 设备截屏：优先 Activity [PixelCopy]（不抢 UiAutomation），失败再试 UiAutomation，
 * 并在 "Not connected" 时 destroy 后重试，避免 already registered 连锁失败。
 */
fun takeDeviceScreenshot(activity: Activity? = null): Bitmap {
    if (activity != null) {
        runCatching { captureViaPixelCopy(activity) }.getOrNull()?.let { return it }
    }

    val instrumentation = InstrumentationRegistry.getInstrumentation()
    var lastError: Throwable? = null

    repeat(3) { attempt ->
        try {
            val bmp = instrumentation.uiAutomation.takeScreenshot()
            if (bmp != null) return bmp
        } catch (e: IllegalStateException) {
            lastError = e
            resetUiAutomation(instrumentation)
            Thread.sleep(200L * (attempt + 1))
        }
    }

    throw AssertionError("截屏失败（PixelCopy + UiAutomation）", lastError)
}

/** Instrumentation.destroyUiAutomation 为 @hide；只销毁、不立刻 reconnect，避免 finish 时 CONNECTING。 */
private fun resetUiAutomation(instrumentation: Instrumentation) {
    runCatching {
        Instrumentation::class.java.getMethod("destroyUiAutomation").invoke(instrumentation)
    }
}

private fun captureViaPixelCopy(activity: Activity): Bitmap {
    val decor = activity.window.decorView
    require(decor.width > 0 && decor.height > 0) { "decorView 尺寸无效" }

    val surfaces = mutableListOf<SurfaceView>()
    collectSurfaceViews(decor, surfaces)

    val result = AtomicReference<Bitmap?>(null)
    val error = AtomicReference<Throwable?>(null)
    val latch = CountDownLatch(1)
    val main = Handler(Looper.getMainLooper())

    main.post {
        try {
            if (surfaces.isEmpty()) {
                val ui = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
                PixelCopy.request(activity.window, ui, { code ->
                    if (code == PixelCopy.SUCCESS) result.set(ui) else {
                        ui.recycle()
                        error.set(IllegalStateException("Window PixelCopy failed: $code"))
                    }
                    latch.countDown()
                }, main)
                return@post
            }

            val mapLayer = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(mapLayer)
            copySurfaces(surfaces, 0, canvas, false) { ok ->
                if (!ok) {
                    mapLayer.recycle()
                    error.set(IllegalStateException("SurfaceView PixelCopy failed"))
                    latch.countDown()
                    return@copySurfaces
                }
                val ui = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
                PixelCopy.request(activity.window, ui, { code ->
                    if (code != PixelCopy.SUCCESS) {
                        mapLayer.recycle()
                        ui.recycle()
                        error.set(IllegalStateException("Window PixelCopy failed: $code"))
                        latch.countDown()
                        return@request
                    }
                    val merged = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
                    Canvas(merged).apply {
                        drawBitmap(mapLayer, 0f, 0f, null)
                        drawBitmap(ui, 0f, 0f, null)
                    }
                    mapLayer.recycle()
                    ui.recycle()
                    result.set(merged)
                    latch.countDown()
                }, main)
            }
        } catch (t: Throwable) {
            error.set(t)
            latch.countDown()
        }
    }

    check(latch.await(10, TimeUnit.SECONDS)) { "PixelCopy 超时" }
    error.get()?.let { throw it }
    return requireNotNull(result.get()) { "PixelCopy 无结果" }
}

private fun collectSurfaceViews(view: View, out: MutableList<SurfaceView>) {
    if (view is SurfaceView) out += view
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            collectSurfaceViews(view.getChildAt(i), out)
        }
    }
}

private fun copySurfaces(
    surfaces: List<SurfaceView>,
    index: Int,
    canvas: Canvas,
    anySuccess: Boolean,
    onDone: (Boolean) -> Unit,
) {
    if (index >= surfaces.size) {
        onDone(anySuccess)
        return
    }
    val sv = surfaces[index]
    if (sv.width <= 0 || sv.height <= 0 || !sv.holder.surface.isValid) {
        copySurfaces(surfaces, index + 1, canvas, anySuccess, onDone)
        return
    }
    val layer = Bitmap.createBitmap(sv.width, sv.height, Bitmap.Config.ARGB_8888)
    val main = Handler(Looper.getMainLooper())
    PixelCopy.request(sv, layer, { code ->
        var nextOk = anySuccess
        if (code == PixelCopy.SUCCESS) {
            val loc = IntArray(2)
            sv.getLocationInWindow(loc)
            canvas.drawBitmap(layer, loc[0].toFloat(), loc[1].toFloat(), null)
            nextOk = true
        }
        layer.recycle()
        copySurfaces(surfaces, index + 1, canvas, nextOk, onDone)
    }, main)
}
