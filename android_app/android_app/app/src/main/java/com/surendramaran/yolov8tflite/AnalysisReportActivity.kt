package com.surendramaran.yolov8tflite

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.surendramaran.yolov8tflite.databinding.ActivityAnalysisReportBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AnalysisReportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnalysisReportBinding
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalysisReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        database = AppDatabase.getDatabase(this)

        val scanId = intent.getLongExtra("scan_id", -1)
        if (scanId != -1L) {
            loadAnalysisReport(scanId)
        } else {
            finish()
        }
    }

    private fun loadAnalysisReport(scanId: Long) {
        lifecycleScope.launch {
            val scan = database.scanHistoryDao().getScanById(scanId)
            if (scan != null) {
                // Calculate remaining shelf life dynamically
                val remainingShelfLife = ShelfLifeCalculator.calculateRemainingShelfLife(
                    scan.shelfLifeDays,
                    scan.scanTimestamp
                )
                val report = ShelfLifeCalculator.generateAnalysisReport(
                    scan.fruitName,
                    scan.condition,
                    scan.confidence,
                    remainingShelfLife, // Use remaining shelf life instead of original
                    scan.scanTimestamp
                )
                displayReport(report, scan)
            }
        }
    }

    private fun displayReport(report: AnalysisReport, scan: ScanHistory) {
        // Load scanned image using Coil if available, otherwise show icon
        if (!scan.imagePath.isNullOrEmpty()) {
            try {
                val imageUri = Uri.parse(scan.imagePath)
                binding.ivFruitIcon.load(imageUri) {
                    crossfade(true)
                    placeholder(getFruitIconResId(report.fruitName))
                    error(getFruitIconResId(report.fruitName))
                    scale(coil.size.Scale.FILL)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                loadFruitIcon(report.fruitName)
            }
        } else {
            loadFruitIcon(report.fruitName)
        }

        binding.tvFruitName.text = report.fruitName
        binding.tvCondition.text = report.condition

        // Quality Assessment
        binding.tvQualityAssessment.text = report.getQualityAssessment()
        binding.tvFreshnessScore.text = String.format(Locale.getDefault(), "%.1f/10", report.qualityScore)
        binding.progressQuality.progress = report.qualityScore.toInt()

        // Shelf Life
        binding.tvShelfLifeDays.text = "${report.shelfLifeDays} ${if (report.shelfLifeDays == 1) "day" else "days"}"
        val shelfLifeDrawableRes = when {
            report.shelfLifeDays <= 0 -> R.drawable.bg_shelf_life_bad
            report.shelfLifeDays <= 2 -> R.drawable.bg_shelf_life_warning
            else -> R.drawable.bg_shelf_life_good
        }
        binding.tvShelfLifeStatus.setBackgroundResource(shelfLifeDrawableRes)
        binding.tvShelfLifeStatus.text = when {
            report.shelfLifeDays <= 0 -> "Expired"
            report.shelfLifeDays <= 2 -> "Urgent"
            else -> "Good"
        }

        // Detected Issues
        if (report.detectedIssues.isNotEmpty()) {
            binding.cardDetectedIssues.visibility = View.VISIBLE
            val issuesAdapter = SimpleListAdapter(report.detectedIssues)
            binding.rvDetectedIssues.layoutManager = LinearLayoutManager(this)
            binding.rvDetectedIssues.adapter = issuesAdapter
        } else {
            binding.cardDetectedIssues.visibility = View.GONE
        }

        // Storage Recommendations
        val recommendationsAdapter = SimpleListAdapter(report.storageRecommendations)
        binding.rvStorageRecommendations.layoutManager = LinearLayoutManager(this)
        binding.rvStorageRecommendations.adapter = recommendationsAdapter

        // Nutritional Information
        binding.tvServingSize.text = "Per ${report.nutritionalInfo.servingSize} serving"
        binding.tvCalories.text = "${report.nutritionalInfo.calories} kcal"
        binding.tvProtein.text = "${report.nutritionalInfo.protein}g"
        binding.tvCarbs.text = "${report.nutritionalInfo.carbs}g"
        binding.tvFiber.text = "${report.nutritionalInfo.fiber}g"
        binding.tvVitaminC.text = "${report.nutritionalInfo.vitaminC}mg"
        binding.tvPotassium.text = "${report.nutritionalInfo.potassium}mg"

        // Storage Tips
        binding.tvOptimalTemp.text = report.optimalStorageTemp
        binding.tvOptimalHumidity.text = report.optimalStorageHumidity
        val storageTipsAdapter = SimpleListAdapter(report.storageTips)
        binding.rvStorageTips.layoutManager = LinearLayoutManager(this)
        binding.rvStorageTips.adapter = storageTipsAdapter

        // Scan Information
        val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
        binding.tvScanDateTime.text = dateFormat.format(Date(report.scanTimestamp))
        binding.tvConfidence.text = String.format(Locale.getDefault(), "%.0f%%", report.confidence * 100)
    }
    
    private fun getFruitIconResId(fruitName: String): Int {
        return when (fruitName.lowercase()) {
            "apple" -> R.drawable.fruit_apple
            "guava" -> R.drawable.fruit_guava
            "mango" -> R.drawable.fruit_mango
            "strawberry" -> R.drawable.fruit_strawberry
            else -> R.drawable.fruit_apple
        }
    }
    
    private fun loadFruitIcon(fruitName: String) {
        val iconResId = getFruitIconResId(fruitName)
        binding.ivFruitIcon.setImageResource(iconResId)
        binding.ivFruitIcon.scaleType = ImageView.ScaleType.FIT_CENTER
    }
}

