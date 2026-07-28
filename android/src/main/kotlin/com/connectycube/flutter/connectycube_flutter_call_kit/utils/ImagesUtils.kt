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

    // Flutter asset icons are loaded via Glide — keep drawable fallback only.
    if (isFlutterAssetPath(customAvatarResName)) {
        return defaultImgResId
    }

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

/** Load URL for configured avatar/icon (Flutter asset, http, or null for drawable). */
fun getConfiguredIconLoadUrl(context: Context): String? {
    val icon = getString(context.applicationContext, "icon")
    return resolveDrawableOrAssetUrl(context, icon)
}

fun getCallBackgroundResId(context: Context, overrideName: String? = null): Int {
    val backgroundResName = if (!TextUtils.isEmpty(overrideName)) {
        overrideName
    } else {
        getString(context.applicationContext, "background")
    }
    if (TextUtils.isEmpty(backgroundResName) || isFlutterAssetPath(backgroundResName)) return 0

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
    if (TextUtils.isEmpty(logoResName) || isFlutterAssetPath(logoResName)) return 0

    var resId = context.resources.getIdentifier(logoResName, "drawable", context.packageName)
    if (resId == 0) {
        resId = context.resources.getIdentifier(logoResName, "mipmap", context.packageName)
    }
    return resId
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
    val iconUrl = getConfiguredIconLoadUrl(context)
    if (!TextUtils.isEmpty(iconUrl)) {
        try {
            // Synchronous decode from android_asset for notification large icon fallback
            val assetKey = iconUrl!!
                .removePrefix("file:///android_asset/")
            if (assetKey.startsWith("flutter_assets/")) {
                context.assets.open(assetKey).use { stream ->
                    val decoded = BitmapFactory.decodeStream(stream)
                    if (decoded != null) return getCircleBitmap(decoded)
                }
            }
        } catch (_: Exception) {
            // fall through to drawable placeholder
        }
    }
    return getCircleBitmap(
        BitmapFactory.decodeResource(
            context.resources,
            getPhotoPlaceholderResId(context)
        )
    )
}
