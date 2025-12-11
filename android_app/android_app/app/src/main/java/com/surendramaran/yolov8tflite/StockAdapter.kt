package com.surendramaran.yolov8tflite

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.surendramaran.yolov8tflite.databinding.ItemStockBinding

class StockAdapter : ListAdapter<StockItem, StockAdapter.StockViewHolder>(DiffCallback()) {

    class StockViewHolder(private val binding: ItemStockBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(stock: StockItem) {
            binding.tvFruitName.text = stock.fruitName
            binding.tvCondition.text = stock.condition
            binding.tvQuantity.text = "Qty: ${stock.quantity}"
            binding.tvQuality.text = String.format("%.1f/10", stock.averageQuality)
            binding.tvShelfLife.text = "${stock.averageShelfLife} days"
            
            // Set fruit icon
            val iconResId = when (stock.fruitName.lowercase()) {
                "apple" -> R.drawable.fruit_apple
                "guava" -> R.drawable.fruit_guava
                "mango" -> R.drawable.fruit_mango
                "strawberry" -> R.drawable.fruit_strawberry
                else -> R.drawable.fruit_apple
            }
            binding.ivFruitIcon.setImageResource(iconResId)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val binding = ItemStockBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StockViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<StockItem>() {
        override fun areItemsTheSame(oldItem: StockItem, newItem: StockItem): Boolean {
            return oldItem.fruitName == newItem.fruitName && oldItem.condition == newItem.condition
        }

        override fun areContentsTheSame(oldItem: StockItem, newItem: StockItem): Boolean {
            return oldItem == newItem
        }
    }
}

