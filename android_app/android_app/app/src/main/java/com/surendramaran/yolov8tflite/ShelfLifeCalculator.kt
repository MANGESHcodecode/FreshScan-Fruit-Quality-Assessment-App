package com.surendramaran.yolov8tflite

import kotlin.random.Random

object ShelfLifeCalculator {
    // Shelf life in days for fresh fruits at room temperature
    private val freshShelfLife: Map<String, Int> = mapOf(
        "Apple" to Random.nextInt(5, 8),        // 5 to 7 days
        "Guava" to Random.nextInt(4, 6),        // 4 to 5 days
        "Mango" to Random.nextInt(5, 8),        // 5 to 7 days
        "Strawberry" to Random.nextInt(3, 6)    // 3 to 5 days
    )

    // Shelf life in days for rotten fruits (should be consumed immediately or discarded)
    private val rottenShelfLife: Map<String, Int> = mapOf(
        "Apple" to 0,
        "Guava" to 0,
        "Mango" to 0,
        "Strawberry" to 0
    )

    /**
     * Calculate shelf life based on fruit name and condition
     * @param fruitName The name of the fruit (e.g., "Apple", "Mango")
     * @param condition The condition of the fruit ("Fresh" or "Rotten")
     * @return Number of days remaining (original shelf life at scan time)
     */
    fun calculateShelfLife(fruitName: String, condition: String): Int {
        val normalizedFruitName = normalizeFruitName(fruitName)
        val normalizedCondition = condition.lowercase().trim()

        return when {
            normalizedCondition.contains("rotten") -> {
                rottenShelfLife[normalizedFruitName] ?: 0
            }
            normalizedCondition.contains("fresh") -> {
                freshShelfLife[normalizedFruitName] ?: 3
            }
            else -> 0
        }
    }

    /**
     * Calculate remaining shelf life based on scan timestamp
     * @param originalShelfLifeDays The original shelf life in days when scanned
     * @param scanTimestamp The timestamp when the fruit was scanned
     * @return Number of days remaining (can be negative if expired)
     */
    fun calculateRemainingShelfLife(originalShelfLifeDays: Int, scanTimestamp: Long): Int {
        val currentTime = System.currentTimeMillis()
        val timePassed = currentTime - scanTimestamp
        val daysPassed = (timePassed / (1000 * 60 * 60 * 12)).toInt()
        val remainingDays = originalShelfLifeDays - daysPassed
        return remainingDays.coerceAtLeast(0) // Don't go below 0 for display purposes, but can be negative internally
    }

    /**
     * Normalize fruit name from label format (e.g., "Apple_Fresh" -> "Apple")
     */
    private fun normalizeFruitName(label: String): String {
        return when {
            label.contains("Apple", ignoreCase = true) -> "Apple"
            label.contains("Guava", ignoreCase = true) -> "Guava"
            label.contains("Mango", ignoreCase = true) -> "Mango"
            label.contains("Strawberry", ignoreCase = true) -> "Strawberry"
            else -> label.split("_").firstOrNull() ?: label
        }
    }

    /**
     * Get condition from label (e.g., "Apple_Fresh" -> "Fresh")
     */
    fun getConditionFromLabel(label: String): String {
        return when {
            label.contains("Rotten", ignoreCase = true) -> "Rotten"
            label.contains("Fresh", ignoreCase = true) -> "Fresh"
            else -> "Unknown"
        }
    }

    /**
     * Get fruit name from label (e.g., "Apple_Fresh" -> "Apple")
     */
    fun getFruitNameFromLabel(label: String): String {
        return normalizeFruitName(label)
    }

    /**
     * Calculate quality score based on condition and confidence
     * Returns a score from 0-10
     */
    fun calculateQualityScore(condition: String, confidence: Float): Float {
        val baseScore = when {
            condition.contains("Fresh", ignoreCase = true) -> 8.0f
            condition.contains("Rotten", ignoreCase = true) -> 2.0f
            else -> 5.0f
        }
        // Adjust based on confidence (higher confidence = more reliable score)
        val confidenceAdjustment = (confidence - 0.5f) * 2.0f // Scale 0.5-1.0 to 0-1
        return (baseScore + confidenceAdjustment).coerceIn(0f, 10f)
    }

    /**
     * Get detected issues based on condition
     */
    fun getDetectedIssues(fruitName: String, condition: String): List<String> {
        val fruitInfo = FruitDatabase.getFruitInfo(fruitName)
        return when {
            condition.contains("Rotten", ignoreCase = true) -> fruitInfo.commonIssues
            condition.contains("Fresh", ignoreCase = true) -> emptyList()
            else -> listOf("Unable to determine condition")
        }
    }

    /**
     * Generate analysis report
     */
    fun generateAnalysisReport(
        fruitName: String,
        condition: String,
        confidence: Float,
        shelfLifeDays: Int,
        scanTimestamp: Long
    ): AnalysisReport {
        val normalizedFruitName = normalizeFruitName(fruitName)
        val fruitInfo = FruitDatabase.getFruitInfo(normalizedFruitName)
        val qualityScore = calculateQualityScore(condition, confidence)
        val detectedIssues = getDetectedIssues(normalizedFruitName, condition)
        
        val storageRecommendations = when {
            shelfLifeDays <= 0 -> listOf(
                "Consume immediately or discard",
                "Do not store further",
                "Check for any signs of spoilage before consumption"
            )
            shelfLifeDays <= 2 -> listOf(
                "Consume within 1-2 days",
                "Store in refrigerator",
                "Monitor for any changes in appearance or smell"
            )
            else -> fruitInfo.storageTips
        }

        return AnalysisReport(
            fruitName = normalizedFruitName,
            condition = condition,
            confidence = confidence,
            qualityScore = qualityScore,
            shelfLifeDays = shelfLifeDays,
            detectedIssues = detectedIssues,
            storageRecommendations = storageRecommendations,
            nutritionalInfo = fruitInfo.nutrition,
            storageTips = fruitInfo.storageTips,
            optimalStorageTemp = fruitInfo.optimalStorageTemp,
            optimalStorageHumidity = fruitInfo.optimalStorageHumidity,
            scanTimestamp = scanTimestamp
        )
    }
}

