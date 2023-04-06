package com.example.findfriend.util

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.example.findfriend.BR

class ItemViewHolder<T>(
    private val binding: ViewDataBinding,
    private val listener: OnItemClickListener<T>
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: T) {

        binding.run {
            setVariable(BR.item, item)
            setVariable(BR.eventHandler, listener)
            executePendingBindings()
        }
    }
}