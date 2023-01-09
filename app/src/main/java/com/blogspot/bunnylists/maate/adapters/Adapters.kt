package com.blogspot.bunnylists.maate.adapters

import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide

@BindingAdapter("imageFromUrl")
fun ImageView.imageFromUrl(url : String?){
    Glide.with(this.context).load(url).into(this)
}