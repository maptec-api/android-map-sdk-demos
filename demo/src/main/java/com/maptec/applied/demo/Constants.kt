package com.maptec.applied.demo

import com.maptec.applied.geometry.LatLng

/** 模块日志名（喂给 [com.maptec.applied.javabase.log.LoggerFactory.getLogger]）。 */
internal const val LOG_MODULE: String = "Demo"

/** Demo 通用常量：默认地图样式与初始相机位置等。 */
object Constants {
    const val DEFAULT_STYLE_ID: String = "style_00001"
    val DEFAULT_MAP_CENTER: LatLng = LatLng(1.4, 103.75)
    const val DEFAULT_ZOOM_LEVEL: Double = 16.0
}
