package com.surendramaran.yolov8tflite

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.surendramaran.yolov8tflite.databinding.ItemFruitCardBinding

data class FruitItem(
    val name: String,
    val iconResId: Int,
    val modelPath: String,
    val labelsPath: String
)

class FruitAdapter(
    private val fruits: List<FruitItem>,
    private val onFruitClick: (FruitItem) -> Unit
) : RecyclerView.Adapter<FruitAdapter.FruitViewHolder>() {

    class FruitViewHolder(private val binding: ItemFruitCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(fruit: FruitItem, onClick: (FruitItem) -> Unit) {
            binding.tvFruitName.text = fruit.name
            binding.ivFruitIcon.setImageResource(fruit.iconResId)
            binding.root.setOnClickListener { onClick(fruit) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FruitViewHolder {
        val binding = ItemFruitCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FruitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FruitViewHolder, position: Int) {
        holder.bind(fruits[position], onFruitClick)
    }

    override fun getItemCount() = fruits.size
}

