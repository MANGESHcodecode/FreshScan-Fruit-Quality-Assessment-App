package com.surendramaran.yolov8tflite

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.ImageRequest
import com.surendramaran.yolov8tflite.databinding.ItemScanHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class ScanHistoryAdapter : ListAdapter<ScanHistory, ScanHistoryAdapter.ScanHistoryViewHolder>(DiffCallback()) {
    
    private var onItemClickListener: ((ScanHistory) -> Unit)? = null
    private var onDeleteClickListener: ((ScanHistory) -> Unit)? = null
    
    fun setOnItemClickListener(listener: (ScanHistory) -> Unit) {
        onItemClickListener = listener
    }
    
    fun setOnDeleteClickListener(listener: (ScanHistory) -> Unit) {
        onDeleteClickListener = listener
    }

    class ScanHistoryViewHolder(
        private val binding: ItemScanHistoryBinding,
        private val onItemClick: ((ScanHistory) -> Unit)?,
        private val onDeleteClick: ((ScanHistory) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(scan: ScanHistory) {
            binding.tvFruitName.text = scan.fruitName
            binding.tvFruitCondition.text = scan.condition
            
            // Calculate remaining shelf life dynamically
            val remainingDays = ShelfLifeCalculator.calculateRemainingShelfLife(
                scan.shelfLifeDays,
                scan.scanTimestamp
            )
            binding.tvShelfLife.text = "${remainingDays} ${binding.root.context.getString(R.string.days)}"
            
            // Load scanned image using Coil if available, otherwise show icon
            if (!scan.imagePath.isNullOrEmpty()) {
                try {
                    val imageUri = Uri.parse(scan.imagePath)
                    val placeholderResId = getFruitIconResId(scan.fruitName)
                    binding.ivFruitIcon.load(imageUri) {
                        crossfade(true)
                        placeholder(placeholderResId)
                        error(placeholderResId)
                        scale(coil.size.Scale.FILL)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    loadFruitIcon(scan.fruitName)
                }
            } else {
                loadFruitIcon(scan.fruitName)
            }
            
            // Format timestamp
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            val date = Date(scan.scanTimestamp)
            binding.tvScanTime.text = dateFormat.format(date)
            
            // Update shelf life background drawable based on remaining days
            val shelfLifeDrawableRes = when {
                remainingDays <= 0 -> R.drawable.bg_shelf_life_bad
                remainingDays <= 2 -> R.drawable.bg_shelf_life_warning
                else -> R.drawable.bg_shelf_life_good
            }
            binding.tvShelfLife.setBackgroundResource(shelfLifeDrawableRes)
            
            // Set click listener
            binding.root.setOnClickListener {
                onItemClick?.invoke(scan)
            }
            
            // Set delete button click listener
            binding.btnDelete.setOnClickListener {
                onDeleteClick?.invoke(scan)
            }
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
            binding.ivFruitIcon.clearColorFilter()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanHistoryViewHolder {
        val binding = ItemScanHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScanHistoryViewHolder(binding, onItemClickListener, onDeleteClickListener)
    }

    override fun onBindViewHolder(holder: ScanHistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<ScanHistory>() {
        override fun areItemsTheSame(oldItem: ScanHistory, newItem: ScanHistory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ScanHistory, newItem: ScanHistory): Boolean {
            return oldItem == newItem
        }
    }
}

