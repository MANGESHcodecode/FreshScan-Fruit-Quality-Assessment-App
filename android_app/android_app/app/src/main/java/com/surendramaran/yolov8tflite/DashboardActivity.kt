package com.surendramaran.yolov8tflite

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.surendramaran.yolov8tflite.databinding.ActivityDashboardBinding

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // Show fruit selection bottom sheet
        showFruitSelectionBottomSheet()

        binding.btnScanHistory.setOnClickListener {
            val intent = Intent(this, ScanHistoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showFruitSelectionBottomSheet() {
        val bottomSheet = FruitSelectionBottomSheet()
        bottomSheet.show(supportFragmentManager, "FruitSelectionBottomSheet")
    }
}


