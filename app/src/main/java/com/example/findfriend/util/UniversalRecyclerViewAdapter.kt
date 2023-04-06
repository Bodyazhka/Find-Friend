package com.example.findfriend.util

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

class UniversalRecyclerViewAdapter<T>(
    @LayoutRes private val layoutRes: Int,
    private val listener: OnItemClickListener<T>,
    callback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, ItemViewHolder<T>>(callback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder<T> {
        return ItemViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                layoutRes,
                parent,
                false
            ), listener
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder<T>, position: Int) {
        holder.bind(getItem(position))
    }
}