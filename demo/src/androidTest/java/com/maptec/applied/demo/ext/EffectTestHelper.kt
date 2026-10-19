package com.maptec.applied.demo.ext

import androidx.test.platform.app.InstrumentationRegistry
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.MapOverlayEngine
import com.maptec.applied.maps.overlay.OverlayKind
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 功能效果类（`*EffectTest`）共用：主线程拿 [MaptecMap]、轮询叠放/拾取结果。
 *
 * 验证约定见仓库 `docs/test-plan.md` §1.0：以 SDK API 为真源，不依赖截图像素。
 * 改动后必须在 **Android 模拟器**上跑通对应 `*EffectTest`（`ANDROID_SERIAL=emulator-*`），未验证不算完成。
 */
object EffectTestHelper {

    fun withMapApi(mapView: MapView, timeoutSec: Long = 5L, action: (MaptecMap) -> Unit) {
        val latch = CountDownLatch(1)
        var error: Throwable? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            mapView.getMapAsync { map ->
                try {
                    action(map)
                } catch (e: Throwable) {
                    error = e
                } finally {
                    latch.countDown()
                }
            }
        }
        assertTrue("MaptecMap 超时", latch.await(timeoutSec, TimeUnit.SECONDS))
        error?.let { throw it }
    }

    fun withEngine(mapView: MapView, action: (MapOverlayEngine) -> Unit) {
        withMapApi(mapView) { map -> action(map.getOverlayEngine()) }
    }

    /**
     * 地理点拾取 overlayId 列表（自上而下，index 0 = 最上层）。
     * [knownIds] 非空时只保留这些 id（过滤底图/其它噪声）。
     */
    fun hitOverlayIds(
        mapView: MapView,
        at: LatLng,
        knownIds: Set<String> = emptySet(),
    ): List<String> {
        var ids = emptyList<String>()
        withEngine(mapView) { engine ->
            ids = engine.queryOverlayHitsFromPoint(at)
                .map { it.overlayId }
                .let { list -> if (knownIds.isEmpty()) list else list.filter { it in knownIds } }
        }
        return ids
    }

    fun waitUntilHitStack(
        mapView: MapView,
        at: LatLng,
        expected: List<String>,
        knownIds: Set<String> = expected.toSet(),
        timeoutMs: Long = 10_000L,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var actual = emptyList<String>()
        while (System.currentTimeMillis() < deadline) {
            actual = hitOverlayIds(mapView, at, knownIds)
            if (actual == expected) return
            Thread.sleep(200)
        }
        fail(
            "拾取叠放顺序不符\n期望(上→下): $expected\n实际(上→下): $actual",
        )
    }

    fun waitUntilTopHit(
        mapView: MapView,
        at: LatLng,
        expectedTop: String,
        knownIds: Set<String>,
        timeoutMs: Long = 10_000L,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var actual: String? = null
        while (System.currentTimeMillis() < deadline) {
            actual = hitOverlayIds(mapView, at, knownIds).firstOrNull()
            if (actual == expectedTop) return
            Thread.sleep(200)
        }
        fail("顶层不符：期望=$expectedTop 实际=$actual 全序=${hitOverlayIds(mapView, at, knownIds)}")
    }

    fun waitUntilAllNative(
        mapView: MapView,
        kind: OverlayKind,
        overlayIds: Collection<String>,
        timeoutMs: Long = 10_000L,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            var ready = false
            withEngine(mapView) { engine ->
                ready = overlayIds.all { engine.hasNativeOverlay(kind, it) }
            }
            if (ready) return
            Thread.sleep(200)
        }
        fail("原生 overlay 未就绪: kind=$kind ids=$overlayIds")
    }
}
