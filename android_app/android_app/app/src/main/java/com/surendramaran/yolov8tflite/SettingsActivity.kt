package com.surendramaran.yolov8tflite

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.surendramaran.yolov8tflite.databinding.ActivitySettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var database: AppDatabase

    private val prefName = "FreshScanPrefs"
    private val keyAutoDetect = "auto_detect_enabled"
    private val keyCameraQuality = "camera_quality"
    private val keyNotifications = "notifications_enabled"
    private val keyTheme = "theme_preference"
    private val keyLanguage = "language_preference"
    private val keyUserType = "user_type"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(prefName, MODE_PRIVATE)
        database = AppDatabase.getDatabase(this)

        setupToolbar()
        bindExistingValues()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_settings)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun bindExistingValues() {
        val appVersion = try {
            val info = packageManager.getPackageInfo(packageName, 0)
            info.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
        binding.tvAppVersionValue.text = appVersion

        binding.switchNotifications.isChecked = prefs.getBoolean(keyNotifications, true)
        binding.switchAutoDetect.isChecked = prefs.getBoolean(keyAutoDetect, true)

        val language = prefs.getString(keyLanguage, "English (US)") ?: "English (US)"
        binding.tvLanguageValue.text = language

        val theme = prefs.getString(keyTheme, "Light") ?: "Light"
        binding.tvThemeValue.text = theme

        val cameraQuality = prefs.getString(keyCameraQuality, "High") ?: "High"
        binding.tvCameraQualityValue.text = cameraQuality

        val userType = prefs.getString(keyUserType, "consumer") ?: "consumer"
        binding.toggleUserType.check(
            if (userType == "vendor") binding.btnVendor.id else binding.btnCustomer.id
        )
        binding.tvUserTypeValue.text = userType.replaceFirstChar { it.uppercase() }
    }

    private fun setupClickListeners() {
        binding.cardProfile.setOnClickListener {
            Toast.makeText(this, "Profile settings coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.cardNotifications.setOnClickListener {
            binding.switchNotifications.toggle()
        }
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(keyNotifications, isChecked).apply()
            Toast.makeText(this, if (isChecked) "Notifications enabled" else "Notifications disabled", Toast.LENGTH_SHORT).show()
        }

        binding.cardLanguage.setOnClickListener { showSingleChoiceDialog("Language", arrayOf("English (US)")) { choice ->
            prefs.edit().putString(keyLanguage, choice).apply()
            binding.tvLanguageValue.text = choice
        } }

        binding.cardTheme.setOnClickListener { showSingleChoiceDialog("Theme", arrayOf("Light", "Dark")) { choice ->
            prefs.edit().putString(keyTheme, choice).apply()
            binding.tvThemeValue.text = choice
        } }

        binding.cardStorage.setOnClickListener {
            Toast.makeText(this, "Storage management coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.cardCameraQuality.setOnClickListener {
            val options = arrayOf("High", "Medium", "Low")
            showSingleChoiceDialog("Camera Quality", options) { choice ->
                prefs.edit().putString(keyCameraQuality, choice).apply()
                binding.tvCameraQualityValue.text = choice
            }
        }

        binding.cardAutoDetection.setOnClickListener {
            binding.switchAutoDetect.toggle()
        }
        binding.switchAutoDetect.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(keyAutoDetect, isChecked).apply()
        }

        binding.toggleUserType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val newType = if (checkedId == binding.btnVendor.id) "vendor" else "consumer"
            prefs.edit().putString(keyUserType, newType).apply()
            binding.tvUserTypeValue.text = newType.replaceFirstChar { it.uppercase() }
            navigateAfterRoleChange(newType)
        }

        binding.cardHelp.setOnClickListener {
            Toast.makeText(this, "Opening help & support...", Toast.LENGTH_SHORT).show()
        }

        binding.cardPrivacy.setOnClickListener {
            Toast.makeText(this, "Privacy policy placeholder", Toast.LENGTH_SHORT).show()
        }

        binding.cardTerms.setOnClickListener {
            Toast.makeText(this, "Terms of service placeholder", Toast.LENGTH_SHORT).show()
        }

        binding.cardClearData.setOnClickListener { confirmAndClearData() }
    }

    private fun navigateAfterRoleChange(userType: String) {
        val intent = if (userType == "vendor") {
            Intent(this, VendorDashboardActivity::class.java)
        } else {
            Intent(this, HomeDashboardActivity::class.java)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun showSingleChoiceDialog(title: String, options: Array<String>, onSelected: (String) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(options) { dialog, which ->
                onSelected(options[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmAndClearData() {
        AlertDialog.Builder(this)
            .setTitle("Clear All Data")
            .setMessage("This will delete all scan history and saved images. This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> clearAllScanData() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearAllScanData() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val scans = database.scanHistoryDao().getAllScans().first()
                    scans.forEach { scan ->
                        if (!scan.imagePath.isNullOrEmpty()) {
                            try {
                                val uri = android.net.Uri.parse(scan.imagePath)
                                contentResolver.delete(uri, null, null)
                            } catch (_: Exception) {
                                // Ignore individual failures
                            }
                        }
                    }
                    database.scanHistoryDao().deleteAllScans()
                }
                Toast.makeText(this@SettingsActivity, "All scan history cleared", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Failed to clear data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

