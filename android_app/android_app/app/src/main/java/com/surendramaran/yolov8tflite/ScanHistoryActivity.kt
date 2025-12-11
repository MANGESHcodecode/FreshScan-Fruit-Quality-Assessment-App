package com.surendramaran.yolov8tflite

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.surendramaran.yolov8tflite.databinding.ActivityScanHistoryBinding
import kotlinx.coroutines.launch

class ScanHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScanHistoryBinding
    private lateinit var adapter: ScanHistoryAdapter
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        database = AppDatabase.getDatabase(this)
        adapter = ScanHistoryAdapter().apply {
            setOnItemClickListener { scan ->
                val intent = Intent(this@ScanHistoryActivity, AnalysisReportActivity::class.java).apply {
                    putExtra("scan_id", scan.id)
                }
                startActivity(intent)
            }
            setOnDeleteClickListener { scan ->
                deleteScan(scan)
            }
        }

        binding.rvScanHistory.layoutManager = LinearLayoutManager(this)
        binding.rvScanHistory.adapter = adapter

        loadScanHistory()
    }

    private fun loadScanHistory() {
        lifecycleScope.launch {
            database.scanHistoryDao().getAllScans().collect { scans ->
                if (scans.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvScanHistory.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.rvScanHistory.visibility = View.VISIBLE
                    adapter.submitList(scans)
                }
            }
        }
    }
    
    private fun deleteScan(scan: ScanHistory) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Scan")
            .setMessage("Are you sure you want to delete this scan of ${scan.fruitName}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        database.scanHistoryDao().deleteScan(scan)
                        // Optionally delete the image file
                        if (!scan.imagePath.isNullOrEmpty()) {
                            try {
                                val uri = android.net.Uri.parse(scan.imagePath)
                                contentResolver.delete(uri, null, null)
                            } catch (e: Exception) {
                                // Ignore if image deletion fails
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

