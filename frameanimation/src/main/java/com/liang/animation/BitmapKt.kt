package com.liang.animation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.TypedValue
import android.view.View

fun View.getBitmap(resId: Int): Bitmap? {
    return BitmapFactory.decodeResource(resources, resId, BitmapFactory.Options().apply {
        val value = TypedValue()
        resources.openRawResource(resId, value)
        inTargetDensity = value.density
        inScaled = false
    })
}