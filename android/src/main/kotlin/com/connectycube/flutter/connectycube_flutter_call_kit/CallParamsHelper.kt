package com.connectycube.flutter.connectycube_flutter_call_kit

import android.text.TextUtils
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.getBoolean
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.getLong
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.getString

/** Helpers to read nested CallEvent maps from MethodChannel arguments. */
@Suppress("UNCHECKED_CAST")
object CallParamsHelper {

    fun asStringKeyedMap(value: Any?): Map<String, Any?>? {
        if (value == null) return null
        if (value is Map<*, *>) {
            return value.entries.associate { it.key.toString() to it.value }
        }
        return null
    }

    fun readInt(value: Any?, fallback: Int = 0): Int {
        return when (value) {
            is Int -> value
            is Long -> value.toInt()
            is Double -> value.toInt()
            else -> value?.toString()?.toIntOrNull() ?: fallback
        }
    }

    fun readLong(value: Any?, fallback: Long): Long {
        return when (value) {
            is Long -> value
            is Int -> value.toLong()
            is Double -> value.toLong()
            else -> value?.toString()?.toLongOrNull() ?: fallback
        }
    }

    fun readBool(value: Any?, fallback: Boolean): Boolean {
        return when (value) {
            is Boolean -> value
            is String -> value.equals("true", true)
            else -> fallback
        }
    }

    fun durationMs(arguments: Map<String, Any?>, context: android.content.Context): Long {
        val androidMap = asStringKeyedMap(arguments["android"])
        val fromAndroid = androidMap?.get("duration_ms")
        if (fromAndroid != null) return readLong(fromAndroid, DEFAULT_CALL_DURATION_MS)
        if (arguments["duration"] != null) {
            return readLong(arguments["duration"], DEFAULT_CALL_DURATION_MS)
        }
        val configured = getLong(context, "default_duration_ms")
        return if (configured > 0) configured else DEFAULT_CALL_DURATION_MS
    }

    fun ringtonePath(arguments: Map<String, Any?>, context: android.content.Context): String? {
        val androidMap = asStringKeyedMap(arguments["android"])
        val fromAndroid = androidMap?.get("ringtone_path")?.toString()
        if (!TextUtils.isEmpty(fromAndroid)) return fromAndroid
        val global = getString(context, "ringtone")
        return if (TextUtils.isEmpty(global)) null else global
    }

    fun backgroundColor(arguments: Map<String, Any?>, context: android.content.Context): String? {
        val androidMap = asStringKeyedMap(arguments["android"])
        val fromAndroid = androidMap?.get("background_color")?.toString()
        if (!TextUtils.isEmpty(fromAndroid)) return fromAndroid
        val global = getString(context, "background_color")
        return if (TextUtils.isEmpty(global)) null else global
    }

    fun backgroundUrl(arguments: Map<String, Any?>, context: android.content.Context): String? {
        val androidMap = asStringKeyedMap(arguments["android"])
        val fromAndroid = androidMap?.get("background_url")?.toString()
        if (!TextUtils.isEmpty(fromAndroid)) return fromAndroid
        val global = getString(context, "background")
        return if (TextUtils.isEmpty(global)) null else global
    }

    fun logoUrl(arguments: Map<String, Any?>, context: android.content.Context): String? {
        val androidMap = asStringKeyedMap(arguments["android"])
        val fromAndroid = androidMap?.get("logo_url")?.toString()
        if (!TextUtils.isEmpty(fromAndroid)) return fromAndroid
        val global = getString(context, "logo")
        return if (TextUtils.isEmpty(global)) null else global
    }

    fun isShowLogo(arguments: Map<String, Any?>, context: android.content.Context): Boolean {
        val androidMap = asStringKeyedMap(arguments["android"])
        if (androidMap?.containsKey("is_show_logo") == true) {
            return readBool(androidMap["is_show_logo"], false)
        }
        return getBoolean(context, "is_show_logo")
    }

    fun textColor(arguments: Map<String, Any?>, context: android.content.Context): String? {
        val androidMap = asStringKeyedMap(arguments["android"])
        val fromAndroid = androidMap?.get("text_color")?.toString()
        if (!TextUtils.isEmpty(fromAndroid)) return fromAndroid
        val global = getString(context, "text_color")
        return if (TextUtils.isEmpty(global)) null else global
    }

    fun actionColor(arguments: Map<String, Any?>, context: android.content.Context): String? {
        val androidMap = asStringKeyedMap(arguments["android"])
        val fromAndroid = androidMap?.get("action_color")?.toString()
        if (!TextUtils.isEmpty(fromAndroid)) return fromAndroid
        val fromGlobalAction = getString(context, "action_color")
        if (!TextUtils.isEmpty(fromGlobalAction)) return fromGlobalAction
        val color = getString(context, "color")
        return if (TextUtils.isEmpty(color)) null else color
    }

    fun acceptLabel(arguments: Map<String, Any?>): String? {
        val direct = arguments["accept_button_label"]?.toString()
        if (!TextUtils.isEmpty(direct)) return direct
        val androidMap = asStringKeyedMap(arguments["android"])
        return androidMap?.get("text_accept")?.toString()
    }

    fun rejectLabel(arguments: Map<String, Any?>): String? {
        val direct = arguments["reject_button_label"]?.toString()
        if (!TextUtils.isEmpty(direct)) return direct
        val androidMap = asStringKeyedMap(arguments["android"])
        return androidMap?.get("text_decline")?.toString()
    }

    fun missedShow(arguments: Map<String, Any?>, context: android.content.Context): Boolean {
        val missed = asStringKeyedMap(arguments["missed_call_notification"])
        if (missed?.containsKey("show_notification") == true) {
            return readBool(missed["show_notification"], true)
        }
        return if (contextContainsKey(context, "show_missed_call_notification")) {
            getBoolean(context, "show_missed_call_notification")
        } else {
            true
        }
    }

    fun missedSubtitle(arguments: Map<String, Any?>, context: android.content.Context): String {
        val missed = asStringKeyedMap(arguments["missed_call_notification"])
        val fromMissed = missed?.get("subtitle")?.toString()
        if (!TextUtils.isEmpty(fromMissed)) return fromMissed!!
        val global = getString(context, "missed_call_subtitle")
        return if (TextUtils.isEmpty(global)) "Missed call" else global!!
    }

    fun missedCallbackText(arguments: Map<String, Any?>, context: android.content.Context): String {
        val missed = asStringKeyedMap(arguments["missed_call_notification"])
        val fromMissed = missed?.get("callback_text")?.toString()
        if (!TextUtils.isEmpty(fromMissed)) return fromMissed!!
        val global = getString(context, "missed_call_callback_text")
        return if (TextUtils.isEmpty(global)) "Call back" else global!!
    }

    fun missedShowCallback(arguments: Map<String, Any?>, context: android.content.Context): Boolean {
        val missed = asStringKeyedMap(arguments["missed_call_notification"])
        if (missed?.containsKey("is_show_callback") == true) {
            return readBool(missed["is_show_callback"], true)
        }
        return if (contextContainsKey(context, "show_missed_call_callback")) {
            getBoolean(context, "show_missed_call_callback")
        } else {
            true
        }
    }

    fun missedCount(arguments: Map<String, Any?>): Int {
        val missed = asStringKeyedMap(arguments["missed_call_notification"])
        return readInt(missed?.get("count"), 1)
    }

    fun missedId(arguments: Map<String, Any?>, callId: String): String {
        val missed = asStringKeyedMap(arguments["missed_call_notification"])
        val id = missed?.get("id")
        return if (id != null) id.toString() else callId
    }

    fun missedChannelName(arguments: Map<String, Any?>, context: android.content.Context): String {
        val androidMap = asStringKeyedMap(arguments["android"])
        val fromAndroid = androidMap?.get("missed_call_notification_channel_name")?.toString()
        if (!TextUtils.isEmpty(fromAndroid)) return fromAndroid!!
        val global = getString(context, "missed_call_notification_channel_name")
        return if (TextUtils.isEmpty(global)) "Missed Call" else global!!
    }

    fun incomingChannelName(arguments: Map<String, Any?>, context: android.content.Context): String {
        val androidMap = asStringKeyedMap(arguments["android"])
        val fromAndroid = androidMap?.get("incoming_call_notification_channel_name")?.toString()
        if (!TextUtils.isEmpty(fromAndroid)) return fromAndroid!!
        val global = getString(context, "incoming_call_notification_channel_name")
        return if (TextUtils.isEmpty(global)) CALL_CHANNEL_NAME else global!!
    }

    private fun contextContainsKey(context: android.content.Context, key: String): Boolean {
        val prefs = context.getSharedPreferences("connectycube_flutter_call_kit", android.content.Context.MODE_PRIVATE)
        return prefs.contains(key)
    }
}
