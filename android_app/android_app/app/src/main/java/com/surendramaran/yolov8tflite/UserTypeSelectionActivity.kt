package com.surendramaran.yolov8tflite

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.surendramaran.yolov8tflite.databinding.ActivityUserTypeSelectionBinding

class UserTypeSelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserTypeSelectionBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserTypeSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("FreshScanPrefs", MODE_PRIVATE)

        // Check if user type is already selected
        val userType = sharedPreferences.getString("user_type", null)
        if (userType != null) {
            navigateToMainApp(userType)
            return
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.cardConsumer.setOnClickListener {
            saveUserType("consumer")
            navigateToMainApp("consumer")
        }

        binding.cardVendor.setOnClickListener {
            saveUserType("vendor")
            navigateToMainApp("vendor")
        }
    }

    private fun saveUserType(userType: String) {
        sharedPreferences.edit().putString("user_type", userType).apply()
    }

    private fun navigateToMainApp(userType: String) {
        val intent = if (userType == "vendor") {
            Intent(this, VendorDashboardActivity::class.java)
        } else {
            Intent(this, HomeDashboardActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}

