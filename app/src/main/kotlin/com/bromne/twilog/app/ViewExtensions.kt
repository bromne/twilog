package com.bromne.twilog.app

import android.widget.ImageView
import com.bromne.twilog.R
import com.bumptech.glide.GenericTransitionOptions
import com.bumptech.glide.Glide

object ViewExtensions {
    fun ImageView.load(url: String, error: Int? = null) {
        Glide.with(this.context)
                .load(url)
                .transition(GenericTransitionOptions.with(R.anim.fade_in_medium))
                .into(this)
                .let {
                    if (error != null) {
                        it.onLoadFailed(this.context.getDrawable(error))
                    }
                }
    }
}