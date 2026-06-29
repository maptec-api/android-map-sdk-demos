package com.maptec.applied.demo

/**
 * 地图标签语言常量（BCP 47），供 Demo 配合 [com.maptec.applied.maps.MaptecMap.setLanguage]
 * 或 [com.maptec.applied.maps.MapOptions.mapLanguage] 使用。
 *
 * 引擎实际支持的语言取决于矢量瓦片里有哪些 `name:xx` 字段；缺失时回退到 `name`。
 */
object LanguageType {
    /** 引擎默认（用瓦片的 `name` 字段，通常是当地语言/混排）。 */
    const val DEFAULT = ""

    const val CHINESE = "zh"
    const val CHINESE_SIMPLIFIED = "zh-Hans"
    const val CHINESE_TRADITIONAL = "zh-Hant"
    const val ENGLISH = "en"
    const val JAPANESE = "ja"
    const val KOREAN = "ko"
    const val GERMAN = "de"
    const val FRENCH = "fr"
    const val SPANISH = "es"
    const val RUSSIAN = "ru"
    const val MALAY = "ms"
    const val THAI = "th"
    const val ARABIC = "ar"
}
