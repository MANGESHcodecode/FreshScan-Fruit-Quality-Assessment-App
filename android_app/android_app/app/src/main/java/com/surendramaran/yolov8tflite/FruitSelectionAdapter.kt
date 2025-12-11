package com.surendramaran.yolov8tflite

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.surendramaran.yolov8tflite.databinding.ItemFruitSelectionBinding

class FruitSelectionAdapter(
    private val fruits: List<FruitItem>,
    private val onFruitClick: (FruitItem) -> Unit
) : RecyclerView.Adapter<FruitSelectionAdapter.FruitViewHolder>() {

    class FruitViewHolder(
        private val binding: ItemFruitSelectionBinding,
        private val onFruitClick: (FruitItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(fruit: FruitItem) {
            binding.tvFruitName.text = fruit.name
            binding.ivFruitIcon.setImageResource(fruit.iconResId)
            
            binding.root.setOnClickListener {
                onFruitClick(fruit)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FruitViewHolder {
        val binding = ItemFruitSelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FruitViewHolder(binding, onFruitClick)
    }

    override fun onBindViewHolder(holder: FruitViewHolder, position: Int) {
        holder.bind(fruits[position])
    }

    override fun getItemCount(): Int = fruits.size
}

