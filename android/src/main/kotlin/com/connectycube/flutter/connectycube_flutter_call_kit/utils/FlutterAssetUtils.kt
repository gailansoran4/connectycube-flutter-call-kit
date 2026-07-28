package com.connectycube.flutter.connectycube_flutter_call_kit.utils

import android.content.Context
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import java.io.File
import java.io.FileOutputStream

private const val TAG = "FlutterAssetUtils"
private val RINGTONE_EXTENSIONS = listOf(".mp3", ".wav", ".ogg", ".m4a", ".aac", ".caf")
private val IMAGE_EXTENSIONS = listOf(".png", ".jpg", ".jpeg", ".webp", ".gif")

/** True when [path] looks like a Flutter asset (or remote URL handled elsewhere). */
fun isFlutterAssetPath(path: String?): Boolean {
    if (TextUtils.isEmpty(path)) return false
    val raw = path!!.trim()
    if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) return false
    return raw.startsWith("assets/", true) || raw.contains("/")
}

/**
 * Maps a Flutter asset key to a loadable URI string for Glide/images:
 * `assets/image/logo.png` → `file:///android_asset/flutter_assets/assets/image/logo.png`
 */
fun resolveDrawableOrAssetUrl(context: Context, value: String?): String? {
    if (TextUtils.isEmpty(value)) return null
    val raw = value!!.trim()
    if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) {
        return raw
    }
    if (isFlutterAssetPath(raw)) {
        val assetKey = firstExistingFlutterAsset(context, raw, IMAGE_EXTENSIONS) ?: raw
        return "file:///android_asset/flutter_assets/$assetKey"
    }
    return null
}

/**
 * Resolves ringtone for notifications:
 * - Flutter asset path (`assets/ringtone/call_ring` or `.mp3`) → copied to cache File URI
 * - plain name → `android.resource://…/raw/<name>`
 * - empty → system default
 */
fun resolveRingtoneUri(context: Context, ringtonePath: String?): Uri {
    val custom = if (!TextUtils.isEmpty(ringtonePath)) {
        ringtonePath!!.trim()
    } else {
        getString(context, "ringtone")?.trim()
    }

    if (TextUtils.isEmpty(custom)) {
        return Settings.System.DEFAULT_RINGTONE_URI
    }

    if (custom!!.startsWith("http://", true) || custom.startsWith("https://", true)) {
        return Uri.parse(custom)
    }

    if (isFlutterAssetPath(custom)) {
        val cached = copyFlutterAssetToCache(context, custom, RINGTONE_EXTENSIONS)
        if (cached != null) {
            Log.d(TAG, "Using cached Flutter ringtone: ${cached.absolutePath}")
            return Uri.fromFile(cached)
        }
        Log.w(TAG, "Flutter ringtone asset not found: $custom — falling back to system")
        return Settings.System.DEFAULT_RINGTONE_URI
    }

    // Drawable/raw resource name (legacy)
    val rawId = context.resources.getIdentifier(custom, "raw", context.packageName)
    if (rawId != 0) {
        return Uri.parse("android.resource://${context.packageName}/$rawId")
    }
    return Uri.parse("android.resource://${context.packageName}/raw/$custom")
}

/**
 * Copies a Flutter asset into the app cache so the OS notification ringtone
 * player can read it (android_asset URIs are unreliable for NotificationChannel sound).
 */
fun copyFlutterAssetToCache(
    context: Context,
    assetPath: String,
    tryExtensions: List<String> = emptyList()
): File? {
    val candidates = assetPathCandidates(assetPath, tryExtensions)
    for (candidate in candidates) {
        val assetKey = "flutter_assets/$candidate"
        try {
            context.assets.open(assetKey).use { input ->
                val safeName = candidate.replace('/', '_')
                val outFile = File(context.cacheDir, "cq_callkit_$safeName")
                if (!outFile.exists() || outFile.length() == 0L) {
                    FileOutputStream(outFile).use { output -> input.copyTo(output) }
                }
                if (outFile.exists() && outFile.length() > 0L) {
                    return outFile
                }
            }
        } catch (_: Exception) {
            // try next candidate
        }
    }
    return null
}

fun firstExistingFlutterAsset(
    context: Context,
    assetPath: String,
    tryExtensions: List<String>
): String? {
    for (candidate in assetPathCandidates(assetPath, tryExtensions)) {
        try {
            context.assets.open("flutter_assets/$candidate").use { return candidate }
        } catch (_: Exception) {
            // continue
        }
    }
    return null
}

fun assetPathCandidates(path: String, tryExtensions: List<String>): List<String> {
    val trimmed = path.trim().trimStart('/')
    val hasExtension = trimmed.substringAfterLast('/').contains('.')
    if (hasExtension || tryExtensions.isEmpty()) {
        return listOf(trimmed)
    }
    return tryExtensions.map { ext ->
        if (trimmed.endsWith(ext, true)) trimmed else trimmed + ext
    } + trimmed
}
