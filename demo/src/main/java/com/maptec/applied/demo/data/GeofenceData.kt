package com.maptec.applied.demo.data

object GeofenceData {
    // 模拟上层下发：圆形围栏（中心点 + 半径，单位 米）
    const val POINT_GEOFENCE_JSON = """
    {
      "type": "Feature",
      "id": "hq_building",
      "properties": { "name": "总部大楼", "radius": 200.0 },
      "geometry": { "type": "Point", "coordinates": [116.3975, 39.9087] }
    }
    """

    // 模拟上层下发：多边形围栏
    const val POLYGON_GEOFENCE_JSON = """
    {
      "type": "Feature",
      "id": "campus_zone",
      "properties": { "name": "园区围栏" },
      "geometry": {
        "type": "Polygon",
        "coordinates": [
          [
            [116.390, 39.905],
            [116.395, 39.905],
            [116.395, 39.910],
            [116.390, 39.910],
            [116.390, 39.905]
          ]
        ]
      }
    }
    """
}
