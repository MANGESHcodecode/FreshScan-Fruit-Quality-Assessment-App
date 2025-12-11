package com.surendramaran.yolov8tflite

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.surendramaran.yolov8tflite.Constants.EXTRA_LABELS_PATH
import com.surendramaran.yolov8tflite.Constants.EXTRA_MODEL_PATH
import com.surendramaran.yolov8tflite.databinding.ActivityMainBinding
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), Detector.DetectorListener {
    private lateinit var binding: ActivityMainBinding
    private val isFrontCamera = false

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var detector: Detector

    private lateinit var cameraExecutor: ExecutorService
    private var currentFrame: Bitmap? = null
    private var modelPath: String = Constants.MODEL_PATH
    private var labelsPath: String = Constants.LABELS_PATH
    
    private lateinit var database: AppDatabase
    private var lastDetectedBoxes: List<BoundingBox>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Resolve model/labels from intent extras with sensible defaults
        intent.getStringExtra(EXTRA_MODEL_PATH)?.let { modelPath = it }
        intent.getStringExtra(EXTRA_LABELS_PATH)?.let { labelsPath = it }

        detector = Detector(baseContext, modelPath, labelsPath, this)
        detector.setup()

        database = AppDatabase.getDatabase(this)

        binding.modelName.text = modelPath

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.btnSave.setOnClickListener {
            lastDetectedBoxes?.let { boxes ->
                saveScanHistory(boxes) // This will save both image and history
            } ?: run {
                saveCurrentFrame() // Just save image if no detection
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider  = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val rotation = binding.viewFinder.display.rotation

        val cameraSelector = CameraSelector
            .Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview =  Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            val bitmapBuffer =
                Bitmap.createBitmap(
                    imageProxy.width,
                    imageProxy.height,
                    Bitmap.Config.ARGB_8888
                )
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
            imageProxy.close()

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

                if (isFrontCamera) {
                    postScale(
                        -1f,
                        1f,
                        imageProxy.width.toFloat(),
                        imageProxy.height.toFloat()
                    )
                }
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                matrix, true
            )

            currentFrame = rotatedBitmap
            detector.detect(rotatedBitmap)
        }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) {
        if (it[Manifest.permission.CAMERA] == true) { startCamera() }
    }

    override fun onDestroy() {
        super.onDestroy()
        detector.clear()
        cameraExecutor.shutdown()
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()){
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    companion object {
        private const val TAG = "Camera"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf (
            Manifest.permission.CAMERA
        ).apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }.toTypedArray()
    }

    override fun onEmptyDetect() {
        binding.overlay.clear()
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            binding.inferenceTime.text = "${inferenceTime}ms"
            binding.overlay.apply {
                setResults(boundingBoxes)
                invalidate()
            }
            // Store the latest detection for saving
            lastDetectedBoxes = boundingBoxes
        }
    }
    
    private fun saveScanHistory(boundingBoxes: List<BoundingBox>) {
        if (boundingBoxes.isEmpty()) return
        
        // Get the highest confidence detection
        val bestDetection = boundingBoxes.maxByOrNull { it.cnf } ?: return
        
        val fruitName = ShelfLifeCalculator.getFruitNameFromLabel(bestDetection.clsName)
        val condition = ShelfLifeCalculator.getConditionFromLabel(bestDetection.clsName)
        val shelfLifeDays = ShelfLifeCalculator.calculateShelfLife(fruitName, condition)
        val qualityScore = ShelfLifeCalculator.calculateQualityScore(condition, bestDetection.cnf)
        val detectedIssues = ShelfLifeCalculator.getDetectedIssues(fruitName, condition)
        
        // Save the current frame and get the image URI
        val imageUri = saveCurrentFrameAndGetUri()
        
        val scanHistory = ScanHistory(
            fruitName = fruitName,
            condition = condition,
            confidence = bestDetection.cnf,
            shelfLifeDays = shelfLifeDays,
            qualityScore = qualityScore,
            detectedIssues = detectedIssues.joinToString(", "),
            imagePath = imageUri?.toString()
        )
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                database.scanHistoryDao().insertScan(scanHistory)
                Log.d(TAG, "Scan history saved: $fruitName - $condition - $shelfLifeDays days - Quality: $qualityScore")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save scan history", e)
            }
        }
    }
    
    private fun saveCurrentFrameAndGetUri(): Uri? {
        val bitmap = currentFrame ?: return null

        val filename = "fruit_scan_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FreshScan")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = contentResolver
        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val itemUri = resolver.insert(collection, contentValues)
        if (itemUri != null) {
            try {
                resolver.openOutputStream(itemUri)?.use { output: OutputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(itemUri, contentValues, null, null)
                }
                
                return itemUri
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save image", e)
            }
        }
        return null
    }

    private fun saveCurrentFrame() {
        val bitmap = currentFrame ?: return

        val filename = "fruit_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FruitDetections")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = contentResolver
        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val itemUri = resolver.insert(collection, contentValues)
        if (itemUri != null) {
            try {
                resolver.openOutputStream(itemUri)?.use { output: OutputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save image", e)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)
            }
        }
    }
}
