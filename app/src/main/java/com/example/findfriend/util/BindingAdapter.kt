package com.example.findfriend.util

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide

@BindingAdapter("app:srcCompat")
fun ImageView.bindGlideSrc(url: String) {
    Glide
        .with(context)
        .load(url)
        .into(this)
}