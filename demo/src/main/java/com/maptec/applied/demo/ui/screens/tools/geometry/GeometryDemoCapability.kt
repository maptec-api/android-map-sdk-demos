package com.maptec.applied.demo.ui.screens.tools.geometry

import androidx.annotation.StringRes
import com.maptec.applied.demo.R

enum class GeometryDemoTab(@StringRes val titleRes: Int) {
    Spherical(R.string.geometry_demo_tab_spherical),
    PathPolygon(R.string.geometry_demo_tab_path_polygon),
    Encoding(R.string.geometry_demo_tab_encoding),
}

enum class GeometryDemoCapability(
    val tab: GeometryDemoTab,
    @StringRes val chipLabelRes: Int,
    @StringRes val hintRes: Int,
    val apiLabel: String,
) {
    Distance(
        tab = GeometryDemoTab.Spherical,
        chipLabelRes = R.string.geometry_demo_chip_distance,
        hintRes = R.string.geometry_demo_hint_distance,
        apiLabel = "SphericalUtil.computeDistanceBetween / computeHeading",
    ),
    Angle(
        tab = GeometryDemoTab.Spherical,
        chipLabelRes = R.string.geometry_demo_chip_angle,
        hintRes = R.string.geometry_demo_hint_angle,
        apiLabel = "SphericalUtil.computeAngleBetween",
    ),
    Area(
        tab = GeometryDemoTab.Spherical,
        chipLabelRes = R.string.geometry_demo_chip_area,
        hintRes = R.string.geometry_demo_hint_area,
        apiLabel = "SphericalUtil.computeArea / PolyUtil.containsLocation",
    ),
    SignedArea(
        tab = GeometryDemoTab.Spherical,
        chipLabelRes = R.string.geometry_demo_chip_signed_area,
        hintRes = R.string.geometry_demo_hint_signed_area,
        apiLabel = "SphericalUtil.computeSignedArea",
    ),
    Offset(
        tab = GeometryDemoTab.Spherical,
        chipLabelRes = R.string.geometry_demo_chip_offset,
        hintRes = R.string.geometry_demo_hint_offset,
        apiLabel = "SphericalUtil.computeOffset",
    ),
    OffsetOrigin(
        tab = GeometryDemoTab.Spherical,
        chipLabelRes = R.string.geometry_demo_chip_offset_origin,
        hintRes = R.string.geometry_demo_hint_offset_origin,
        apiLabel = "SphericalUtil.computeOffsetOrigin",
    ),
    Interpolate(
        tab = GeometryDemoTab.Spherical,
        chipLabelRes = R.string.geometry_demo_chip_interpolate,
        hintRes = R.string.geometry_demo_hint_interpolate,
        apiLabel = "SphericalUtil.interpolate",
    ),
    Path(
        tab = GeometryDemoTab.PathPolygon,
        chipLabelRes = R.string.geometry_demo_chip_path,
        hintRes = R.string.geometry_demo_hint_path,
        apiLabel = "PolyUtil.simplify / isLocationOnPath",
    ),
    SegmentIndex(
        tab = GeometryDemoTab.PathPolygon,
        chipLabelRes = R.string.geometry_demo_chip_segment_index,
        hintRes = R.string.geometry_demo_hint_segment_index,
        apiLabel = "PolyUtil.locationIndexOnEdgeOrPath",
    ),
    DistanceToSegment(
        tab = GeometryDemoTab.PathPolygon,
        chipLabelRes = R.string.geometry_demo_chip_distance_to_segment,
        hintRes = R.string.geometry_demo_hint_distance_to_segment,
        apiLabel = "PolyUtil.distanceToLine",
    ),
    IsClosed(
        tab = GeometryDemoTab.PathPolygon,
        chipLabelRes = R.string.geometry_demo_chip_is_closed,
        hintRes = R.string.geometry_demo_hint_is_closed,
        apiLabel = "PolyUtil.isClosedPolygon",
    ),
    EncodeDecode(
        tab = GeometryDemoTab.Encoding,
        chipLabelRes = R.string.geometry_demo_chip_encode,
        hintRes = R.string.geometry_demo_hint_encode,
        apiLabel = "PolyUtil.encodePath / decodePath",
    ),
    ;

    companion object {
        fun forTab(tab: GeometryDemoTab): List<GeometryDemoCapability> =
            entries.filter { it.tab == tab }

        val defaultForTab: Map<GeometryDemoTab, GeometryDemoCapability> = mapOf(
            GeometryDemoTab.Spherical to Distance,
            GeometryDemoTab.PathPolygon to Path,
            GeometryDemoTab.Encoding to EncodeDecode,
        )
    }
}

internal object GeometryDemoColors {
    const val BLUE = "#2E7CF6"
    const val GREEN = "#34A853"
    const val ORANGE = "#FB8C00"
    const val PURPLE = "#8E44AD"
    const val RED = "#EA4335"
    const val BLUE_FILL = "#402E7CF6"
    const val GREEN_FILL = "#4034A853"
    const val RED_FILL = "#40EA4335"
    const val ORANGE_FILL = "#40FB8C00"
}
