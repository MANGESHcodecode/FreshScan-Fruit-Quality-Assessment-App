package com.surendramaran.yolov8tflite

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.surendramaran.yolov8tflite.databinding.ActivityVendorDashboardBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.view.Menu
import android.view.MenuItem

class VendorDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVendorDashboardBinding
    private lateinit var database: AppDatabase
    private lateinit var stockAdapter: StockAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVendorDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        database = AppDatabase.getDatabase(this)

        setupUI()
        setupRecyclerView()
        loadStockSummary()
        loadStockList()
    }

    private fun setupUI() {
        binding.cardScanFruit.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
        }

        binding.tvViewAllStock.setOnClickListener {
            // Navigate to full stock list
            val intent = Intent(this, StockManagementActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        stockAdapter = StockAdapter()
        binding.rvStockList.layoutManager = LinearLayoutManager(this)
        binding.rvStockList.adapter = stockAdapter
    }

    private fun loadStockSummary() {
        lifecycleScope.launch {
            try {
                val allScans = withContext(Dispatchers.IO) {
                    database.scanHistoryDao().getAllScans().first()
                }

                val totalItems = allScans.size
                val freshItems = allScans.count { it.condition.contains("Fresh", ignoreCase = true) }
                val rottenItems = allScans.count { it.condition.contains("Rotten", ignoreCase = true) }
                val totalValue = allScans.sumOf { 
                    when (it.fruitName.lowercase()) {
                        "apple" -> 50
                        "guava" -> 40
                        "mango" -> 60
                        "strawberry" -> 80
                        else -> 50
                    } * if (it.condition.contains("Fresh", ignoreCase = true)) 1 else 0
                }

                binding.tvTotalItems.text = totalItems.toString()
                binding.tvFreshItems.text = freshItems.toString()
                binding.tvRottenItems.text = rottenItems.toString()
                binding.tvTotalValue.text = "₹$totalValue"
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadStockList() {
        lifecycleScope.launch {
            try {
                val allScans = withContext(Dispatchers.IO) {
                    database.scanHistoryDao().getAllScans().first()
                }

                // Group by fruit name and condition
                val stockMap = allScans.groupBy { "${it.fruitName}_${it.condition}" }
                val stockItems = stockMap.map { (_, scans) ->
                    val firstScan = scans.first()
                    StockItem(
                        fruitName = firstScan.fruitName,
                        condition = firstScan.condition,
                        quantity = scans.size,
                        averageQuality = scans.map { it.qualityScore }.average().toFloat(),
                        averageShelfLife = scans.map { it.shelfLifeDays }.average().toInt()
                    )
                }.sortedByDescending { it.quantity }

                stockAdapter.submitList(stockItems.take(5))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_vendor_dashboard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

data class StockItem(
    val fruitName: String,
    val condition: String,
    val quantity: Int,
    val averageQuality: Float,
    val averageShelfLife: Int
)

