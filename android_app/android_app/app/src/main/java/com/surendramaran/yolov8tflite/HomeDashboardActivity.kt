package com.surendramaran.yolov8tflite

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import android.app.DatePickerDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.surendramaran.yolov8tflite.databinding.ActivityHomeDashboardBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class HomeDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeDashboardBinding
    private lateinit var database: AppDatabase
    private lateinit var recentScansAdapter: RecentScansAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityHomeDashboardBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Initialize database
            database = AppDatabase.getDatabase(this)

            // Setup UI components
            setupUI()
            setupBottomNavigation()
            
            // Load data asynchronously
            loadStatistics()
            loadRecentScans()
        } catch (e: Exception) {
            e.printStackTrace()
            // If there's a critical error, we should still show something
            // For now, just let it crash with the stack trace
            throw e
        }
    }

    private fun setupUI() {
        try {
            // Set greeting based on time of day
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val greeting = when (hour) {
                in 5..11 -> "Good Morning"
                in 12..17 -> "Good Afternoon"
                in 18..20 -> "Good Evening"
                else -> "Good Night"
            }
            binding.tvGreeting.text = greeting

            // Set current date
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            binding.tvDate.text = dateFormat.format(Date())

            // Scan Fruit Card Click
            binding.cardScanFruit.setOnClickListener {
                try {
                    val intent = Intent(this, DashboardActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // View All Click
            binding.tvViewAll.setOnClickListener {
                try {
                    val intent = Intent(this, ScanHistoryActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Settings Click
            binding.btnSettings.setOnClickListener {
                try {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupBottomNavigation() {
        try {
            binding.bottomNavigation.setOnItemSelectedListener { item ->
                try {
                    when (item.itemId) {
                        R.id.nav_home -> {
                            // Already on home
                            true
                        }
                        R.id.nav_scan -> {
                            val intent = Intent(this, DashboardActivity::class.java)
                            startActivity(intent)
                            finish()
                            true
                        }
                        R.id.nav_dashboard -> {
                            openExpiryCalendar()
                            true
                        }
                        R.id.nav_history -> {
                            val intent = Intent(this, ScanHistoryActivity::class.java)
                            startActivity(intent)
                            finish()
                            true
                        }
                        else -> false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
            binding.bottomNavigation.selectedItemId = R.id.nav_home
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openExpiryCalendar() {
        val now = Calendar.getInstance()
        val listener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            val startOfDay = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            showExpiringFruits(startOfDay)
        }
        DatePickerDialog(
            this,
            listener,
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showExpiringFruits(startOfDay: Long) {
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000
        lifecycleScope.launch {
            try {
                val scans = withContext(Dispatchers.IO) {
                    database.scanHistoryDao().getAllScans().first()
                }
                val expiring = scans.filter { scan ->
                    val expiryTime = scan.scanTimestamp + scan.shelfLifeDays * 24L * 60L * 60L * 1000L
                    expiryTime in startOfDay until endOfDay
                }
                val message = if (expiring.isEmpty()) {
                    "No fruits expiring on this date."
                } else {
                    expiring.groupBy { it.fruitName }
                        .map { (fruit, items) -> "$fruit (${items.size})" }
                        .sorted()
                        .joinToString("\n")
                }
                AlertDialog.Builder(this@HomeDashboardActivity)
                    .setTitle("Expiring on selected date")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            } catch (e: Exception) {
                AlertDialog.Builder(this@HomeDashboardActivity)
                    .setTitle("Error")
                    .setMessage("Unable to load expiring fruits: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun loadStatistics() {
        lifecycleScope.launch {
            try {
                val allScans = withContext(Dispatchers.IO) {
                    try {
                        database.scanHistoryDao().getAllScans().first()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        emptyList<ScanHistory>()
                    }
                }
                
                // Calculate scans today
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val scansToday = allScans.count { it.scanTimestamp >= today }
                val freshScans = allScans.filter { it.qualityScore > 0 }
                val averageFreshness = if (freshScans.isNotEmpty()) {
                    freshScans.map { it.qualityScore }.average()
                } else {
                    0.0
                }
                val forSale = allScans.count { scan ->
                    val remainingDays = ShelfLifeCalculator.calculateRemainingShelfLife(
                        scan.shelfLifeDays,
                        scan.scanTimestamp
                    )
                    scan.condition.contains("Fresh", ignoreCase = true) && 
                    scan.qualityScore >= 6.0 && 
                    remainingDays > 0 
                }
                
                // Update UI on main thread
                binding.tvScansToday.text = scansToday.toString()
                binding.tvAverageFreshness.text = String.format(Locale.getDefault(), "%.1f/10", averageFreshness)
                binding.tvForSale.text = forSale.toString()
            } catch (e: Exception) {
                // Handle error gracefully
                e.printStackTrace()
                binding.tvScansToday.text = "0"
                binding.tvAverageFreshness.text = "0.0/10"
                binding.tvForSale.text = "0"
            }
        }
    }

    private fun loadRecentScans() {
        try {
            recentScansAdapter = RecentScansAdapter { scan ->
                try {
                    val intent = Intent(this, AnalysisReportActivity::class.java).apply {
                        putExtra("scan_id", scan.id)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            binding.rvRecentScans.layoutManager = LinearLayoutManager(this)
            binding.rvRecentScans.adapter = recentScansAdapter

            lifecycleScope.launch {
                try {
                    database.scanHistoryDao().getAllScans().collect { scans ->
                        val recentScans = scans.take(5)
                        try {
                            recentScansAdapter.submitList(recentScans)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    recentScansAdapter.submitList(emptyList())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        loadStatistics()
    }
}
