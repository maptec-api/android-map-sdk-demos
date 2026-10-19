package com.maptec.applied.demo.data

/**
 * POI分类数据
 * 基于国际化通用POI分类体系,
 * 参考文档 https://maptech.yuque.com/ak9qv5/cwo028/cfl6ggtr7ni1dg3w#QfkBD
 */
object PoiCategoryData {
    /**
     * POI分类项
     */
    data class CategoryItem(
        val value: String, // 用于API调用的值
        val displayName: String // 显示名称
    )

    /**
     * 获取所有二级分类列表（用于下拉框）
     */
    fun getAllSecondaryCategories(): List<CategoryItem> {
        return listOf(
            // 1. 餐饮美食 (Food & Drink)
            CategoryItem("Chinese_Cuisine", "Chinese_Cuisine"),
            CategoryItem("Asian_Cuisine", "Asian_Cuisine"),
            CategoryItem("Western_Cuisine", "Western_Cuisine"),
            CategoryItem("Fast_Food_Snacks", "Fast_Food_Snacks"),
            CategoryItem("Cafe_Tea_Dessert", "Cafe_Tea_Dessert"),
            CategoryItem("Bars_Nightlife", "Bars_Nightlife"),
            
            // 2. 购物零售 (Shopping & Retail)
            CategoryItem("Shopping_Centers", "Shopping_Centers"),
            CategoryItem("Groceries", "Groceries"),
            CategoryItem("Fashion_Accessories", "Fashion_Accessories"),
            CategoryItem("Electronics", "Electronics"),
            CategoryItem("Home_Garden", "Home_Garden"),
            CategoryItem("Personal_Goods", "Personal_Goods"),
            CategoryItem("Specialty_Retail", "Specialty_Retail"),
            
            // 3. 交通设施 (Transport & Mobility)
            CategoryItem("Air_Travel", "Air_Travel"),
            CategoryItem("Rail_Transport", "Rail_Transport"),
            CategoryItem("Bus_Coach", "Bus_Coach"),
            CategoryItem("Water_Transport", "Water_Transport"),
            CategoryItem("Parking", "Parking"),
            CategoryItem("Energy_Facilities", "Energy_Facilities"),
            
            // 4. 汽车服务 (Automotive Services)
            CategoryItem("Car_Dealers", "Car_Dealers"),
            CategoryItem("Repair_Maintenance", "Repair_Maintenance"),
            CategoryItem("Car_Rental", "Car_Rental"),
            
            // 5. 住宿服务 (Accommodation)
            CategoryItem("Hotels", "Hotels"),
            CategoryItem("Alternative_Lodging", "Alternative_Lodging"),
            CategoryItem("Outdoor_Lodging", "Outdoor_Lodging"),
            
            // 6. 休闲娱乐 (Entertainment & Leisure)
            CategoryItem("Cinema_Theater", "Cinema_Theater"),
            CategoryItem("Recreation", "Recreation"),
            CategoryItem("Theme_Parks", "Theme_Parks"),
            CategoryItem("Wellness", "Wellness"),
            
            // 7. 旅游与文化 (Tourism & Culture)
            CategoryItem("Parks_Plazas", "Parks_Plazas"),
            CategoryItem("Landmarks", "Landmarks"),
            CategoryItem("Museums_Galleries", "Museums_Galleries"),
            CategoryItem("Religious_Places", "Religious_Places"),
            
            // 8. 运动健身 (Sports & Fitness)
            CategoryItem("Stadiums", "Stadiums"),
            CategoryItem("Fitness", "Fitness"),
            CategoryItem("Specific_Sports", "Specific_Sports"),
            
            // 9. 医疗保健 (Health & Medical)
            CategoryItem("Hospitals", "Hospitals"),
            CategoryItem("Clinics", "Clinics"),
            CategoryItem("Pharmacy_Care", "Pharmacy_Care"),
            
            // 10. 生活服务 (Services & Daily Life)
            CategoryItem("Financial", "Financial"),
            CategoryItem("Post_Logistics", "Post_Logistics"),
            CategoryItem("Telecom", "Telecom"),
            CategoryItem("Convenience", "Convenience"),
            CategoryItem("Business", "Business"),
            
            // 11. 机构与团体 (Government & Organizations)
            CategoryItem("Government", "Government"),
            CategoryItem("Education", "Education"),
            CategoryItem("Social_Orgs", "Social_Orgs"),
            
            // 12. 公司企业 (Corporations & Industry)
            CategoryItem("Offices", "Offices"),
            CategoryItem("Industry", "Industry"),
            
            // 13. 房产住宅 (Residential)
            CategoryItem("Housing", "Housing"),
            
            // 14. 自然地理 (Geography)
            CategoryItem("Water_Bodies", "Water_Bodies"),
            CategoryItem("Land_Features", "Land_Features")
        )
    }
}
