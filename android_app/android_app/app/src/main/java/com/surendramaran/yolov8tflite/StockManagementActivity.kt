package com.surendramaran.yolov8tflite

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.surendramaran.yolov8tflite.databinding.ActivityStockManagementBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StockManagementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStockManagementBinding
    private lateinit var database: AppDatabase
    private lateinit var stockAdapter: StockAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStockManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        database = AppDatabase.getDatabase(this)

        setupRecyclerView()
        loadAllStock()
    }

    private fun setupRecyclerView() {
        stockAdapter = StockAdapter()
        binding.rvStockList.layoutManager = LinearLayoutManager(this)
        binding.rvStockList.adapter = stockAdapter
    }

    private fun loadAllStock() {
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

                stockAdapter.submitList(stockItems)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

