package com.surendramaran.yolov8tflite

data class FruitNutrition(
    val calories: Int,
    val protein: Double, // grams
    val carbs: Double, // grams
    val fiber: Double, // grams
    val vitaminC: Double, // mg
    val potassium: Int, // mg
    val servingSize: String = "100g"
)

data class FruitInfo(
    val name: String,
    val nutrition: FruitNutrition,
    val storageTips: List<String>,
    val commonIssues: List<String>,
    val optimalStorageTemp: String,
    val optimalStorageHumidity: String
)

object FruitDatabase {
    private val appleInfo = FruitInfo(
        name = "Apple",
        nutrition = FruitNutrition(
            calories = 52,
            protein = 0.3,
            carbs = 14.0,
            fiber = 2.4,
            vitaminC = 4.6,
            potassium = 107
        ),
        storageTips = listOf(
            "Store in a cool, dark place or refrigerator",
            "Keep away from other fruits to prevent ripening",
            "Check for bruises and remove damaged apples",
            "Store in perforated plastic bags for better air circulation"
        ),
        commonIssues = listOf(
            "Bruising and browning",
            "Soft spots indicating over-ripeness",
            "Mold growth in humid conditions",
            "Wrinkled skin from dehydration"
        ),
        optimalStorageTemp = "0-4°C (32-39°F)",
        optimalStorageHumidity = "90-95%"
    )

    private val guavaInfo = FruitInfo(
        name = "Guava",
        nutrition = FruitNutrition(
            calories = 68,
            protein = 2.6,
            carbs = 14.0,
            fiber = 5.4,
            vitaminC = 228.3,
            potassium = 417
        ),
        storageTips = listOf(
            "Store at room temperature until ripe",
            "Refrigerate when ripe to extend shelf life",
            "Keep in a paper bag to speed up ripening",
            "Avoid storing with other fruits"
        ),
        commonIssues = listOf(
            "Over-ripening leading to soft texture",
            "Fungal spots on the skin",
            "Loss of firmness",
            "Discoloration of flesh"
        ),
        optimalStorageTemp = "8-10°C (46-50°F) when ripe",
        optimalStorageHumidity = "85-90%"
    )

    private val mangoInfo = FruitInfo(
        name = "Mango",
        nutrition = FruitNutrition(
            calories = 60,
            protein = 0.8,
            carbs = 15.0,
            fiber = 1.6,
            vitaminC = 36.4,
            potassium = 168
        ),
        storageTips = listOf(
            "Store unripe mangoes at room temperature",
            "Refrigerate ripe mangoes to slow further ripening",
            "Place in a paper bag with an apple to speed ripening",
            "Keep away from direct sunlight"
        ),
        commonIssues = listOf(
            "Black spots indicating spoilage",
            "Over-ripening causing mushy texture",
            "Skin wrinkling from dehydration",
            "Sour smell indicating fermentation"
        ),
        optimalStorageTemp = "10-13°C (50-55°F) when ripe",
        optimalStorageHumidity = "85-90%"
    )

    private val strawberryInfo = FruitInfo(
        name = "Strawberry",
        nutrition = FruitNutrition(
            calories = 32,
            protein = 0.7,
            carbs = 7.7,
            fiber = 2.0,
            vitaminC = 58.8,
            potassium = 153
        ),
        storageTips = listOf(
            "Refrigerate immediately after purchase",
            "Don't wash until ready to eat",
            "Store in original container or spread on paper towel",
            "Remove any moldy berries immediately"
        ),
        commonIssues = listOf(
            "Mold growth (very common)",
            "Soft, mushy texture",
            "Discoloration and bruising",
            "Loss of firmness and juiciness"
        ),
        optimalStorageTemp = "0-2°C (32-36°F)",
        optimalStorageHumidity = "90-95%"
    )

    fun getFruitInfo(fruitName: String): FruitInfo {
        return when (fruitName.lowercase()) {
            "apple" -> appleInfo
            "guava" -> guavaInfo
            "mango" -> mangoInfo
            "strawberry" -> strawberryInfo
            else -> appleInfo // default
        }
    }
}

