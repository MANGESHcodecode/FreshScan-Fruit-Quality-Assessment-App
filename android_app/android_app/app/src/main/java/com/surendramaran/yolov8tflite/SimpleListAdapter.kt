package com.surendramaran.yolov8tflite

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.surendramaran.yolov8tflite.databinding.ItemSimpleListBinding

class SimpleListAdapter(private val items: List<String>) : RecyclerView.Adapter<SimpleListAdapter.ViewHolder>() {

    class ViewHolder(private val binding: ItemSimpleListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: String) {
            binding.tvItem.text = "• $item"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSimpleListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}

