package com.maptec.applied.demo

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maptec.applied.demo.ext.assertGeometryDemoTextAbsent
import com.maptec.applied.demo.ext.assertGeometryDemoTextContains
import com.maptec.applied.demo.ext.assertGeometryResultContains
import com.maptec.applied.demo.ext.clickGeometryClearButton
import com.maptec.applied.demo.ext.clickGeometryMap
import com.maptec.applied.demo.ext.expandConfigPanel
import com.maptec.applied.demo.ext.openGeometryDemoReady
import com.maptec.applied.demo.ext.resetToMainCatalog
import com.maptec.applied.demo.ext.selectGeometryChip
import com.maptec.applied.demo.ext.selectGeometryTab
import com.maptec.applied.demo.ext.settleForIdle
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 几何计算聚合页仪器化测试。
 */
@RunWith(AndroidJUnit4::class)
class GeometryDemoScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /** 避开底部 Tab/Chip 区域，减少误触 。 */
    private val mapTapA = 0.36f to 0.28f
    private val mapTapB = 0.62f to 0.32f
    private val mapTapC = 0.48f to 0.22f

    @Before
    fun setUp() {
        composeTestRule.resetToMainCatalog()
    }

    @After
    fun tearDown() {
        composeTestRule.resetToMainCatalog()
        composeTestRule.settleForIdle()
    }

    private fun launchGeometryDemo() {
        composeTestRule.openGeometryDemoReady()
    }

    private fun clickConfigTag(tag: String) {
        composeTestRule.expandConfigPanel()
        composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).performClick()
        Thread.sleep(400)
    }

    @Test
    fun distance_twoMapTaps_showDistanceAndHeading() {
        launchGeometryDemo()
        composeTestRule.selectGeometryChip("Distance")
        composeTestRule.clickGeometryMap(mapTapA, mapTapB)
        composeTestRule.assertGeometryDemoTextContains("Distance =")
        composeTestRule.assertGeometryResultContains("computeDistanceBetween")
    }

    @Test
    fun angle_twoMapTaps_showAngleBetween() {
        launchGeometryDemo()
        composeTestRule.selectGeometryChip("Angle")
        composeTestRule.clickGeometryMap(mapTapA, mapTapB)
        composeTestRule.assertGeometryDemoTextContains("computeAngleBetween")
        composeTestRule.assertGeometryResultContains("Degrees")
    }

    @Test
    fun area_threeVertices_showComputeArea() {
        launchGeometryDemo()
        composeTestRule.selectGeometryChip("Area")
        composeTestRule.clickGeometryMap(mapTapA, mapTapB, mapTapC)
        composeTestRule.assertGeometryDemoTextContains("computeArea")
    }

    @Test
    fun signedArea_ccwSample_showsSignedArea() {
        launchGeometryDemo()
        composeTestRule.selectGeometryChip("SignedArea")
        clickConfigTag("geometry_sample_ccw_button")
        composeTestRule.assertGeometryDemoTextContains("computeSignedArea")
    }

    @Test
    fun offset_referencePoint_showsComputeOffset() {
        launchGeometryDemo()
        composeTestRule.selectGeometryChip("Offset")
        composeTestRule.clickGeometryMap(mapTapA)
        composeTestRule.assertGeometryResultContains("computeOffset")
    }

    @Test
    fun interpolate_twoPoints_showsInterpolateResult() {
        launchGeometryDemo()
        composeTestRule.selectGeometryChip("Interpolate")
        composeTestRule.clickGeometryMap(mapTapA, mapTapB)
        composeTestRule.assertGeometryDemoTextContains("fraction")
        composeTestRule.assertGeometryResultContains("interpolate")
    }

    @Test
    fun path_simplifyExample_showsSimplifySummary() {
        launchGeometryDemo()
        composeTestRule.selectGeometryTab("PathPolygon")
        composeTestRule.selectGeometryChip("Path")
        clickConfigTag("geometry_simplify_example_button")
        composeTestRule.assertGeometryDemoTextContains("simplify")
    }

    @Test
    fun distanceToSegment_threeTaps_showDistanceToLine() {
        launchGeometryDemo()
        composeTestRule.selectGeometryTab("PathPolygon")
        composeTestRule.selectGeometryChip("DistanceToSegment")
        composeTestRule.clickGeometryMap(mapTapA, mapTapB, mapTapC)
        composeTestRule.assertGeometryDemoTextContains("distanceToLine")
    }

    @Test
    fun isClosed_filledSample_showsClosedTrue() {
        launchGeometryDemo()
        composeTestRule.selectGeometryTab("PathPolygon")
        composeTestRule.selectGeometryChip("IsClosed")
        clickConfigTag("geometry_closed_sample_button")
        composeTestRule.assertGeometryDemoTextContains("isClosedPolygon = true")
    }

    @Test
    fun encode_twoPoints_showEncodeAndDecode() {
        launchGeometryDemo()
        composeTestRule.selectGeometryTab("Encoding")
        composeTestRule.selectGeometryChip("EncodeDecode")
        composeTestRule.clickGeometryMap(mapTapA, mapTapB)
        composeTestRule.assertGeometryDemoTextContains("Encoded =")
        composeTestRule.expandConfigPanel()
        composeTestRule.assertGeometryResultContains("Decoded")
    }

    @Test
    fun clearButton_clearsDistanceResult() {
        launchGeometryDemo()
        composeTestRule.selectGeometryChip("Distance")
        composeTestRule.clickGeometryMap(mapTapA, mapTapB)
        composeTestRule.assertGeometryDemoTextContains("Distance =")
        composeTestRule.clickGeometryClearButton()
        composeTestRule.assertGeometryDemoTextAbsent("Distance =")
    }
}
