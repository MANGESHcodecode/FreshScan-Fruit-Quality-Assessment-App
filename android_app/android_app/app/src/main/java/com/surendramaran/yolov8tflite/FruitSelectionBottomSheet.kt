package com.surendramaran.yolov8tflite

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.surendramaran.yolov8tflite.Constants.APPLE_LABELS
import com.surendramaran.yolov8tflite.Constants.APPLE_MODEL
import com.surendramaran.yolov8tflite.Constants.EXTRA_LABELS_PATH
import com.surendramaran.yolov8tflite.Constants.EXTRA_MODEL_PATH
import com.surendramaran.yolov8tflite.Constants.GUAVA_LABELS
import com.surendramaran.yolov8tflite.Constants.GUAVA_MODEL
import com.surendramaran.yolov8tflite.Constants.MANGO_LABELS
import com.surendramaran.yolov8tflite.Constants.MANGO_MODEL
import com.surendramaran.yolov8tflite.Constants.STRAWBERRY_LABELS
import com.surendramaran.yolov8tflite.Constants.STRAWBERRY_MODEL
import com.surendramaran.yolov8tflite.databinding.BottomSheetFruitSelectionBinding

class FruitSelectionBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetFruitSelectionBinding? = null
    private val binding get() = _binding!!

    private val fruits = listOf(
        FruitItem("Apple", R.drawable.fruit_apple, APPLE_MODEL, APPLE_LABELS),
        FruitItem("Guava", R.drawable.fruit_guava, GUAVA_MODEL, GUAVA_LABELS),
        FruitItem("Mango", R.drawable.fruit_mango, MANGO_MODEL, MANGO_LABELS),
        FruitItem("Strawberry", R.drawable.fruit_strawberry, STRAWBERRY_MODEL, STRAWBERRY_LABELS)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFruitSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        val adapter = FruitSelectionAdapter(fruits) { fruit ->
            startDetector(fruit.modelPath, fruit.labelsPath)
            dismiss()
        }
        binding.rvFruits.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFruits.adapter = adapter
    }

    private fun startDetector(modelPath: String, labelsPath: String) {
        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            putExtra(EXTRA_MODEL_PATH, modelPath)
            putExtra(EXTRA_LABELS_PATH, labelsPath)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

