package com.surendramaran.yolov8tflite

data class AnalysisReport(
    val fruitName: String,
    val condition: String,
    val confidence: Float,
    val qualityScore: Float, // 0-10
    val shelfLifeDays: Int,
    val detectedIssues: List<String>,
    val storageRecommendations: List<String>,
    val nutritionalInfo: FruitNutrition,
    val storageTips: List<String>,
    val optimalStorageTemp: String,
    val optimalStorageHumidity: String,
    val scanTimestamp: Long
) {
    fun getQualityAssessment(): String {
        return when {
            qualityScore >= 8.0 -> "Excellent - Fruit is in perfect condition"
            qualityScore >= 6.0 -> "Good - Fruit is fresh and consumable"
            qualityScore >= 4.0 -> "Fair - Fruit should be consumed soon"
            qualityScore >= 2.0 -> "Poor - Fruit is showing signs of deterioration"
            else -> "Very Poor - Fruit may not be safe to consume"
        }
    }

    fun getFreshnessRating(): String {
        return when {
            qualityScore >= 8.0 -> "Excellent"
            qualityScore >= 6.0 -> "Good"
            qualityScore >= 4.0 -> "Fair"
            qualityScore >= 2.0 -> "Poor"
            else -> "Very Poor"
        }
    }
}

