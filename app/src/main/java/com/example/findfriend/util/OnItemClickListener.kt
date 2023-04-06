package com.example.findfriend.util

import android.view.View

interface OnItemClickListener<T> {
    fun onClick(view: View, item: T)
}