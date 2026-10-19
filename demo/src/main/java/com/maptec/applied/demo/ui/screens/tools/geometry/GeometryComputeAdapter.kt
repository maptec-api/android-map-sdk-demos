package com.maptec.applied.demo.ui.screens.tools.geometry

import com.maptec.applied.geometry.LatLng
import com.maptec.applied.geometry.util.PolyUtil
import com.maptec.applied.geometry.util.SphericalUtil

/**
 * 几何计算适配层：Demo 只依赖本对象，更换 SDK 时替换实现即可。
 */
internal object GeometryComputeAdapter {
    fun distanceBetween(from: LatLng, to: LatLng): Double =
        SphericalUtil.computeDistanceBetween(from, to)

    fun heading(from: LatLng, to: LatLng): Double =
        SphericalUtil.computeHeading(from, to)

    fun angleBetween(from: LatLng, to: LatLng): Double =
        SphericalUtil.computeAngleBetween(from, to)

    fun pathLength(path: List<LatLng>): Double =
        SphericalUtil.computeLength(path)

    fun area(polygon: List<LatLng>): Double =
        SphericalUtil.computeArea(polygon)

    fun signedArea(polygon: List<LatLng>): Double =
        SphericalUtil.computeSignedArea(polygon)

    fun offset(from: LatLng, distanceMeters: Double, headingDegrees: Double): LatLng =
        SphericalUtil.computeOffset(from, distanceMeters, headingDegrees)

    fun offsetOrigin(to: LatLng, distanceMeters: Double, headingDegrees: Double): LatLng? =
        SphericalUtil.computeOffsetOrigin(to, distanceMeters, headingDegrees)

    fun interpolate(from: LatLng, to: LatLng, fraction: Double): LatLng =
        SphericalUtil.interpolate(from, to, fraction)

    fun containsLocation(point: LatLng, polygon: List<LatLng>, geodesic: Boolean): Boolean =
        PolyUtil.containsLocation(point, polygon, geodesic)

    fun isLocationOnEdge(
        point: LatLng,
        polygon: List<LatLng>,
        geodesic: Boolean,
        toleranceMeters: Double,
    ): Boolean = PolyUtil.isLocationOnEdge(point, polygon, geodesic, toleranceMeters)

    fun isLocationOnPath(
        point: LatLng,
        path: List<LatLng>,
        geodesic: Boolean,
        toleranceMeters: Double,
    ): Boolean = PolyUtil.isLocationOnPath(point, path, geodesic, toleranceMeters)

    fun locationIndexOnPath(
        point: LatLng,
        path: List<LatLng>,
        geodesic: Boolean,
        toleranceMeters: Double,
    ): Int = PolyUtil.locationIndexOnPath(point, path, geodesic, toleranceMeters)

    fun locationIndexOnEdgeOrPath(
        point: LatLng,
        path: List<LatLng>,
        closed: Boolean,
        geodesic: Boolean,
        toleranceMeters: Double,
    ): Int = PolyUtil.locationIndexOnEdgeOrPath(point, path, closed, geodesic, toleranceMeters)

    fun distanceToLine(point: LatLng, start: LatLng, end: LatLng): Double =
        PolyUtil.distanceToLine(point, start, end)

    fun simplify(path: List<LatLng>, toleranceMeters: Double): List<LatLng> =
        PolyUtil.simplify(path, toleranceMeters)

    fun encodePath(path: List<LatLng>): String = PolyUtil.encodePath(path)

    fun decodePath(encoded: String): List<LatLng> = PolyUtil.decodePath(encoded)

    fun isClosedPolygon(path: List<LatLng>): Boolean = PolyUtil.isClosedPolygon(path)
}
