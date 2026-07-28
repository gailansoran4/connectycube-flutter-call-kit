package com.connectycube.flutter.connectycube_flutter_call_kit.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.text.TextUtils

fun getPhotoPlaceholderResId(context: Context): Int {
    val customAvatarResName = getString(context.applicationContext, "icon")
    val defaultImgResId =
        context.resources.getIdentifier(
            "photo_placeholder",
            "drawable",
            context.packageName
        )

    return if (TextUtils.isEmpty(customAvatarResName)) {
        defaultImgResId
    } else {
        val avatarResourceId =
            context.resources.getIdentifier(
                customAvatarResName,
                "drawable",
                context.packageName
            )
        if (avatarResourceId != 0) {
            avatarResourceId
        } else {
            defaultImgResId
        }
    }
}

fun getCallBackgroundResId(context: Context, overrideName: String? = null): Int {
    val backgroundResName = if (!TextUtils.isEmpty(overrideName)) {
        overrideName
    } else {
        getString(context.applicationContext, "background")
    }
    if (TextUtils.isEmpty(backgroundResName)) return 0

    return context.resources.getIdentifier(
        backgroundResName,
        "drawable",
        context.packageName
    )
}

fun getLogoResId(context: Context, overrideName: String? = null): Int {
    val logoResName = if (!TextUtils.isEmpty(overrideName)) {
        overrideName
    } else {
        getString(context.applicationContext, "logo")
    }
    if (TextUtils.isEmpty(logoResName)) return 0

    var resId = context.resources.getIdentifier(logoResName, "drawable", context.packageName)
    if (resId == 0) {
        resId = context.resources.getIdentifier(logoResName, "mipmap", context.packageName)
    }
    return resId
}

fun resolveDrawableOrAssetUrl(context: Context, value: String?): String? {
    if (TextUtils.isEmpty(value)) return null
    val raw = value!!.trim()
    if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) {
        return raw
    }
    if (raw.startsWith("assets/") || raw.contains("/")) {
        return "file:///android_asset/flutter_assets/$raw"
    }
    return null
}

fun getCircleBitmap(bitmap: Bitmap): Bitmap {
    val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val paint = Paint()
    val rect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())

    paint.isAntiAlias = true
    paint.shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

    canvas.drawRoundRect(rect, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
    return output
}

fun getDefaultPhoto(context: Context): Bitmap {
    return getCircleBitmap(
        BitmapFactory.decodeResource(
            context.resources,
            getPhotoPlaceholderResId(context)
        )
    )
}
