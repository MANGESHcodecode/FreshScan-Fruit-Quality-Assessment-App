package com.surendramaran.yolov8tflite

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.surendramaran.yolov8tflite.databinding.ItemRecentScanBinding
import java.text.SimpleDateFormat
import java.util.*

class RecentScansAdapter(
    private val onScanClick: (ScanHistory) -> Unit
) : ListAdapter<ScanHistory, RecentScansAdapter.RecentScanViewHolder>(DiffCallback()) {

    class RecentScanViewHolder(
        private val binding: ItemRecentScanBinding,
        private val onScanClick: (ScanHistory) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(scan: ScanHistory) {
            binding.tvFruitName.text = scan.fruitName
            binding.tvCondition.text = scan.condition
            
            // Load scanned image using Coil if available, otherwise show icon
            if (!scan.imagePath.isNullOrEmpty()) {
                try {
                    val imageUri = Uri.parse(scan.imagePath)
                    binding.ivFruitIcon.load(imageUri) {
                        crossfade(true)
                        placeholder(getFruitIconResId(scan.fruitName))
                        error(getFruitIconResId(scan.fruitName))
                        scale(coil.size.Scale.FILL)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    loadFruitIcon(scan.fruitName)
                }
            } else {
                loadFruitIcon(scan.fruitName)
            }
            
            // Format time ago
            val timeAgo = getTimeAgo(scan.scanTimestamp)
            binding.tvTimeAgo.text = timeAgo
            
            binding.root.setOnClickListener {
                try {
                    onScanClick(scan)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            try {
                binding.btnRescan.setOnClickListener {
                    try {
                        onScanClick(scan)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                // btnRescan might not exist in some layouts
                e.printStackTrace()
            }
        }
        
        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            
            return when {
                days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
                hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
                minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
                else -> "Just now"
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
            val iconResId = when (fruitName.lowercase()) {
                "apple" -> R.drawable.fruit_apple
                "guava" -> R.drawable.fruit_guava
                "mango" -> R.drawable.fruit_mango
                "strawberry" -> R.drawable.fruit_strawberry
                else -> R.drawable.fruit_apple
            }
            binding.ivFruitIcon.setImageResource(iconResId)
            binding.ivFruitIcon.scaleType = ImageView.ScaleType.FIT_CENTER
            binding.ivFruitIcon.clearColorFilter()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentScanViewHolder {
        val binding = ItemRecentScanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecentScanViewHolder(binding, onScanClick)
    }

    override fun onBindViewHolder(holder: RecentScanViewHolder, position: Int) {
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

