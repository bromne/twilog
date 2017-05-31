package com.bromne.view

import android.content.Context
import android.support.annotation.AnimRes
import android.view.View
import android.view.animation.AnimationUtils

fun View.startAnimation(context: Context, @AnimRes resourceId: Int): Unit {
    val anim = AnimationUtils.loadAnimation(context, resourceId)
    this.startAnimation(anim)
}


