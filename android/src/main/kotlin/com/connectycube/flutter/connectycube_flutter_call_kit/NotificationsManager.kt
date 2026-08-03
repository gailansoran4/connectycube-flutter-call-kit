package com.connectycube.flutter.connectycube_flutter_call_kit

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.TextUtils
import android.widget.RemoteViews
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.getConfiguredLogoLoadUrl
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.getDefaultLogo
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.getDefaultPhoto
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.getPhotoPlaceholderResId
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.getString
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.isApplicationForeground
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.resolveDrawableOrAssetUrl
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.resolveRingtoneUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val CALL_CHANNEL_ID = "calls_channel_id"
const val CALL_CHANNEL_NAME = "Calls"
const val MISSED_CALL_CHANNEL_ID = "missed_calls_channel_id"

fun cancelCallNotification(context: Context, callId: String) {
    val notificationManager = NotificationManagerCompat.from(context)
    notificationManager.cancel(callId.hashCode())
}

fun cancelMissedCallNotification(context: Context, callId: String) {
    val notificationManager = NotificationManagerCompat.from(context)
    notificationManager.cancel(("missing_$callId").hashCode())
}

fun showCallNotification(
    context: Context,
    callId: String,
    callType: Int,
    callInitiatorId: Int,
    callInitiatorName: String,
    callOpponents: ArrayList<Int>,
    callPhoto: String?,
    userInfo: String,
    acceptButtonLabel: String? = null,
    rejectButtonLabel: String? = null,
    acceptButtonBgColor: String? = null,
    acceptButtonTextColor: String? = null,
    rejectButtonBgColor: String? = null,
    rejectButtonTextColor: String? = null,
    durationMs: Long = DEFAULT_CALL_DURATION_MS,
    ringtonePath: String? = null,
    backgroundColor: String? = null,
    backgroundUrl: String? = null,
    logoUrl: String? = null,
    isShowLogo: Boolean = false,
    textColor: String? = null,
    actionColor: String? = null,
    channelName: String = CALL_CHANNEL_NAME,
    missedShow: Boolean = true,
    missedSubtitle: String = "Missed call",
    missedCallbackText: String = "Call back",
    missedShowCallback: Boolean = true,
    missedCount: Int = 1,
    missedId: String = callId,
    missedChannelName: String = "Missed Call"
) {
    Log.d("NotificationsManager", "[showCallNotification]")
    val notificationManager = NotificationManagerCompat.from(context)

    val ringtone = resolveRingtoneUri(context, ringtonePath)
    Log.d("NotificationsManager", "ringtone: $ringtone")

    val isVideoCall = callType == 1
    val callTypeTitle =
        String.format(CALL_TYPE_PLACEHOLDER, if (isVideoCall) "Video" else "Audio")

    val callData = buildCallBundle(
        callId, callType, callInitiatorId, callInitiatorName, callOpponents,
        callPhoto, userInfo, acceptButtonLabel, rejectButtonLabel,
        acceptButtonBgColor, acceptButtonTextColor,
        rejectButtonBgColor, rejectButtonTextColor, durationMs,
        backgroundColor, backgroundUrl, logoUrl, isShowLogo, textColor, actionColor,
        ringtonePath, missedShow, missedSubtitle, missedCallbackText, missedShowCallback,
        missedCount, missedId, missedChannelName
    )

    // App icon / logo for the custom notification content (never caller photo).
    val logoFallback = getDefaultLogo(context, logoUrl)
    val configuredLogoUrl = getConfiguredLogoLoadUrl(context, logoUrl)
    val remoteLogo = when {
        !TextUtils.isEmpty(configuredLogoUrl) &&
            configuredLogoUrl!!.startsWith("http", true) -> configuredLogoUrl
        !TextUtils.isEmpty(logoUrl) && logoUrl!!.startsWith("http", true) -> logoUrl
        else -> null
    }

    val builder: NotificationCompat.Builder =
        createCallNotification(
            context,
            callInitiatorName,
            callTypeTitle,
            ringtone,
            isVideoCall,
            callData,
            durationMs,
            acceptButtonLabel,
            rejectButtonLabel,
            acceptButtonBgColor,
            acceptButtonTextColor,
            rejectButtonBgColor,
            rejectButtonTextColor,
            logoFallback
        )

    addCallFullScreenIntent(
        context,
        builder,
        callId,
        callType,
        callInitiatorId,
        callInitiatorName,
        callOpponents,
        callPhoto,
        userInfo,
        acceptButtonLabel,
        rejectButtonLabel,
        acceptButtonBgColor,
        acceptButtonTextColor,
        rejectButtonBgColor,
        rejectButtonTextColor,
        durationMs,
        backgroundColor,
        backgroundUrl,
        logoUrl,
        isShowLogo,
        textColor
    )

    addCancelCallNotificationIntent(
        context,
        builder,
        callId.hashCode(),
        callData
    )

    setNotificationSmallIcon(context, builder, isVideoCall)
    setNotificationColor(context, builder, actionColor)
    createCallNotificationChannel(notificationManager, ringtone, channelName)

    // Incoming calls always use notification_incoming_call.xml (banner + drawer).
    if (remoteLogo != null) {
        loadLogoAndPostNotification(
            context,
            notificationManager,
            builder,
            callId.hashCode(),
            remoteLogo,
            logoFallback,
            usesCustomRemoteViews = true,
            callInitiatorName,
            callTypeTitle,
            acceptButtonLabel,
            rejectButtonLabel,
            acceptButtonBgColor,
            acceptButtonTextColor,
            rejectButtonBgColor,
            rejectButtonTextColor,
            callData
        )
    } else {
        // Icon is already inside the custom RemoteViews — do not set largeIcon
        // (OEM skins would draw a second icon and break the layout).
        postNotification(callId.hashCode(), notificationManager, builder)
    }

    if (!notificationManager.areNotificationsEnabled()) {
        Log.w(
            "NotificationsManager",
            "[showCallNotification] notifications disabled – UI may not appear"
        )
    }

    // Full-screen intent is suppressed while the app is interactive/foreground, so
    // launch IncomingCallActivity directly in that case. When background/locked,
    // rely on the full-screen intent (background activity starts are restricted).
    if (isApplicationForeground(context)) {
        try {
            val intent = createStartIncomingScreenIntent(
                context,
                callId,
                callType,
                callInitiatorId,
                callInitiatorName,
                callOpponents,
                callPhoto,
                userInfo,
                acceptButtonLabel,
                rejectButtonLabel,
                acceptButtonBgColor,
                acceptButtonTextColor,
                rejectButtonBgColor,
                rejectButtonTextColor,
                durationMs,
                backgroundColor,
                backgroundUrl,
                logoUrl,
                isShowLogo,
                textColor
            )
            context.startActivity(intent)
            Log.d(
                "NotificationsManager",
                "[showCallNotification] started IncomingCallActivity (foreground)"
            )
        } catch (e: Exception) {
            Log.w(
                "NotificationsManager",
                "[showCallNotification] startActivity failed: ${e.message}"
            )
        }
    }
}

fun showMissCallNotification(
    context: Context,
    callId: String,
    callType: Int,
    callInitiatorId: Int,
    callInitiatorName: String,
    callOpponents: ArrayList<Int>,
    callPhoto: String?,
    userInfo: String,
    missedShow: Boolean = true,
    missedSubtitle: String = "Missed call",
    missedCallbackText: String = "Call back",
    missedShowCallback: Boolean = true,
    missedCount: Int = 1,
    missedId: String = callId,
    missedChannelName: String = "Missed Call",
    actionColor: String? = null
) {
    if (!missedShow) {
        Log.d("NotificationsManager", "[showMissCallNotification] skipped (show=false)")
        return
    }

    val notificationManager = NotificationManagerCompat.from(context)
    val notificationId = ("missing_$missedId").hashCode()
    createMissedCallNotificationChannel(notificationManager, missedChannelName)

    val callData = Bundle()
    callData.putString(EXTRA_CALL_ID, callId)
    callData.putInt(EXTRA_CALL_TYPE, callType)
    callData.putInt(EXTRA_CALL_INITIATOR_ID, callInitiatorId)
    callData.putString(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
    callData.putIntegerArrayList(EXTRA_CALL_OPPONENTS, callOpponents)
    callData.putString(EXTRA_CALL_PHOTO, callPhoto)
    callData.putString(EXTRA_CALL_USER_INFO, userInfo)
    callData.putBoolean(EXTRA_MISSED_SHOW, missedShow)
    callData.putString(EXTRA_MISSED_SUBTITLE, missedSubtitle)
    callData.putString(EXTRA_MISSED_CALLBACK_TEXT, missedCallbackText)
    callData.putBoolean(EXTRA_MISSED_SHOW_CALLBACK, missedShowCallback)
    callData.putInt(EXTRA_MISSED_COUNT, missedCount)
    callData.putString(EXTRA_MISSED_ID, missedId)

    val missedCallTypeTitle =
        "Missed ${if (callType == 1) "Video" else "Audio"} call"
    val missedSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    val builder = NotificationCompat.Builder(context, MISSED_CALL_CHANNEL_ID)
        .setContentTitle(callInitiatorName)
        .setContentText(missedCallTypeTitle)
        .setSubText(missedSubtitle)
        .setSmallIcon(resolveMissedSmallIcon(context, callType))
        .setAutoCancel(true)
        .setOnlyAlertOnce(true)
        .setCategory(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                android.app.Notification.CATEGORY_MISSED_CALL
            else NotificationCompat.CATEGORY_CALL
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setSound(missedSound)
        .setContentIntent(getAppPendingIntent(context, notificationId, callData))

    if (missedCount > 1) {
        builder.setNumber(missedCount)
    }

    if (missedShowCallback) {
        val callbackAction = NotificationCompat.Action.Builder(
            0,
            missedCallbackText,
            getMissedCallbackPendingIntent(context, notificationId, callData)
        ).build()
        builder.addAction(callbackAction)
    }

    setNotificationColor(context, builder, actionColor)

    val defaultPhoto = getDefaultPhoto(context)
    if (TextUtils.isEmpty(callPhoto)) {
        builder.setLargeIcon(defaultPhoto)
        notificationManager.notify(notificationId, builder.build())
    } else {
        loadPhotoAndPostNotification(
            context,
            notificationManager,
            builder,
            notificationId,
            callPhoto!!,
            defaultPhoto
        )
    }
}

fun buildCallBundle(
    callId: String,
    callType: Int,
    callInitiatorId: Int,
    callInitiatorName: String,
    callOpponents: ArrayList<Int>,
    callPhoto: String?,
    userInfo: String,
    acceptButtonLabel: String?,
    rejectButtonLabel: String?,
    acceptButtonBgColor: String?,
    acceptButtonTextColor: String?,
    rejectButtonBgColor: String?,
    rejectButtonTextColor: String?,
    durationMs: Long,
    backgroundColor: String?,
    backgroundUrl: String?,
    logoUrl: String?,
    isShowLogo: Boolean,
    textColor: String?,
    actionColor: String?,
    ringtonePath: String?,
    missedShow: Boolean,
    missedSubtitle: String,
    missedCallbackText: String,
    missedShowCallback: Boolean,
    missedCount: Int,
    missedId: String,
    missedChannelName: String
): Bundle {
    val callData = Bundle()
    callData.putString(EXTRA_CALL_ID, callId)
    callData.putInt(EXTRA_CALL_TYPE, callType)
    callData.putInt(EXTRA_CALL_INITIATOR_ID, callInitiatorId)
    callData.putString(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
    callData.putIntegerArrayList(EXTRA_CALL_OPPONENTS, callOpponents)
    callData.putString(EXTRA_CALL_PHOTO, callPhoto)
    callData.putString(EXTRA_CALL_USER_INFO, userInfo)
    callData.putString(EXTRA_ACCEPT_BUTTON_LABEL, acceptButtonLabel)
    callData.putString(EXTRA_REJECT_BUTTON_LABEL, rejectButtonLabel)
    callData.putString(EXTRA_ACCEPT_BUTTON_BG_COLOR, acceptButtonBgColor)
    callData.putString(EXTRA_ACCEPT_BUTTON_TEXT_COLOR, acceptButtonTextColor)
    callData.putString(EXTRA_REJECT_BUTTON_BG_COLOR, rejectButtonBgColor)
    callData.putString(EXTRA_REJECT_BUTTON_TEXT_COLOR, rejectButtonTextColor)
    callData.putLong(EXTRA_CALL_DURATION, durationMs)
    callData.putString(EXTRA_BACKGROUND_COLOR, backgroundColor)
    callData.putString(EXTRA_BACKGROUND_URL, backgroundUrl)
    callData.putString(EXTRA_LOGO_URL, logoUrl)
    callData.putBoolean(EXTRA_IS_SHOW_LOGO, isShowLogo)
    callData.putString(EXTRA_TEXT_COLOR, textColor)
    callData.putString(EXTRA_ACTION_COLOR, actionColor)
    callData.putString(EXTRA_RINGTONE_PATH, ringtonePath)
    callData.putBoolean(EXTRA_MISSED_SHOW, missedShow)
    callData.putString(EXTRA_MISSED_SUBTITLE, missedSubtitle)
    callData.putString(EXTRA_MISSED_CALLBACK_TEXT, missedCallbackText)
    callData.putBoolean(EXTRA_MISSED_SHOW_CALLBACK, missedShowCallback)
    callData.putInt(EXTRA_MISSED_COUNT, missedCount)
    callData.putString(EXTRA_MISSED_ID, missedId)
    callData.putString(EXTRA_MISSED_CHANNEL_NAME, missedChannelName)
    return callData
}

fun resolveMissedSmallIcon(context: Context, callType: Int): Int {
    val name = if (callType == 1) "ic_video_missed" else "ic_call_missed"
    val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
    return if (resId != 0) resId else context.applicationInfo.icon
}

fun getAppPendingIntent(context: Context, requestCode: Int, callData: Bundle): PendingIntent {
    val launchIntent = getLaunchIntent(context) ?: Intent()
    launchIntent.addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_SINGLE_TOP or
            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
            Intent.FLAG_ACTIVITY_CLEAR_TOP
    )
    launchIntent.putExtras(callData)
    return PendingIntent.getActivity(
        context,
        requestCode,
        launchIntent,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
    )
}

fun getMissedCallbackPendingIntent(
    context: Context,
    requestCode: Int,
    callData: Bundle
): PendingIntent {
    return PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, EventReceiver::class.java)
            .setAction(ACTION_CALL_CALLBACK)
            .putExtras(callData),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
    )
}

fun postNotification(
    notificationId: Int,
    notificationManager: NotificationManagerCompat,
    builder: NotificationCompat.Builder
) {
    val notification = builder.build()
    notification.flags = notification.flags or NotificationCompat.FLAG_INSISTENT
    notificationManager.notify(notificationId, notification)
}

fun loadPhotoAndPostNotification(
    context: Context,
    notificationManager: NotificationManagerCompat,
    builder: NotificationCompat.Builder,
    notificationId: Int,
    photoUrl: String,
    defaultPhoto: Bitmap
) {
    CoroutineScope(Dispatchers.IO).launch {
        val photoPlaceholder = getPhotoPlaceholderResId(context)
        val loadUrl = resolveDrawableOrAssetUrl(context, photoUrl) ?: photoUrl

        if (!TextUtils.isEmpty(photoUrl)) {
            val futureTarget = Glide.with(context)
                .asBitmap()
                .load(loadUrl)
                .transform(CircleCrop())
                .error(photoPlaceholder)
                .placeholder(photoPlaceholder)
                .submit()

            try {
                val bitmap = futureTarget.get()
                builder.setLargeIcon(bitmap)
                Glide.with(context).clear(futureTarget)
                postNotification(notificationId, notificationManager, builder)
            } catch (e: Exception) {
                builder.setLargeIcon(defaultPhoto)
                postNotification(notificationId, notificationManager, builder)
            }
        } else {
            builder.setLargeIcon(defaultPhoto)
            postNotification(notificationId, notificationManager, builder)
        }
    }
}

fun loadLogoAndPostNotification(
    context: Context,
    notificationManager: NotificationManagerCompat,
    builder: NotificationCompat.Builder,
    notificationId: Int,
    logoUrl: String,
    defaultLogo: Bitmap,
    usesCustomRemoteViews: Boolean = false,
    title: String = "",
    callName: String? = null,
    acceptButtonLabel: String? = null,
    rejectButtonLabel: String? = null,
    acceptButtonBgColor: String? = null,
    acceptButtonTextColor: String? = null,
    rejectButtonBgColor: String? = null,
    rejectButtonTextColor: String? = null,
    callData: Bundle? = null
) {
    CoroutineScope(Dispatchers.IO).launch {
        val loadUrl = resolveDrawableOrAssetUrl(context, logoUrl) ?: logoUrl
        val futureTarget = Glide.with(context)
            .asBitmap()
            .load(loadUrl)
            .transform(CircleCrop())
            .submit()

        val bitmap = try {
            futureTarget.get().also { Glide.with(context).clear(futureTarget) }
        } catch (_: Exception) {
            defaultLogo
        }

        if (usesCustomRemoteViews && callData != null) {
            val requestCode = (callData.getString(EXTRA_CALL_ID) ?: title).hashCode()
            val rejectIntent = getRejectCallIntent(context, callData, requestCode)
            val acceptIntent = getAcceptCallIntent(context, callData, requestCode)
            val headsUpViews = buildCallRemoteViews(
                context,
                "notification_incoming_call_heads_up",
                title,
                callName,
                acceptButtonLabel,
                rejectButtonLabel,
                acceptButtonBgColor,
                acceptButtonTextColor,
                rejectButtonBgColor,
                rejectButtonTextColor,
                rejectIntent,
                acceptIntent,
                bitmap
            )
            val bigViews = buildCallRemoteViews(
                context,
                "notification_incoming_call",
                title,
                callName,
                acceptButtonLabel,
                rejectButtonLabel,
                acceptButtonBgColor,
                acceptButtonTextColor,
                rejectButtonBgColor,
                rejectButtonTextColor,
                rejectIntent,
                acceptIntent,
                bitmap
            )
            applyCustomCallRemoteViews(builder, headsUpViews, bigViews)
        } else {
            builder.setLargeIcon(bitmap)
        }
        postNotification(notificationId, notificationManager, builder)
    }
}

fun getLaunchIntent(context: Context): Intent? {
    val packageName = context.packageName
    val packageManager: PackageManager = context.packageManager
    return packageManager.getLaunchIntentForPackage(packageName)
}

fun hasCustomCallButtons(
    acceptButtonLabel: String?,
    rejectButtonLabel: String?,
    acceptButtonBgColor: String?,
    acceptButtonTextColor: String?,
    rejectButtonBgColor: String?,
    rejectButtonTextColor: String?
): Boolean {
    return !TextUtils.isEmpty(acceptButtonLabel) ||
        !TextUtils.isEmpty(rejectButtonLabel) ||
        !TextUtils.isEmpty(acceptButtonBgColor) ||
        !TextUtils.isEmpty(acceptButtonTextColor) ||
        !TextUtils.isEmpty(rejectButtonBgColor) ||
        !TextUtils.isEmpty(rejectButtonTextColor)
}

fun createCallNotification(
    context: Context,
    title: String,
    callName: String?,
    ringtone: Uri,
    isVideoCall: Boolean,
    callData: Bundle,
    durationMs: Long,
    acceptButtonLabel: String? = null,
    rejectButtonLabel: String? = null,
    acceptButtonBgColor: String? = null,
    acceptButtonTextColor: String? = null,
    rejectButtonBgColor: String? = null,
    rejectButtonTextColor: String? = null,
    appIcon: Bitmap? = null
): NotificationCompat.Builder {
    val person = Person.Builder()
        .setName(title)
        .setImportant(true)
        .build()

    val requestCode = (callData.getString(EXTRA_CALL_ID) ?: title).hashCode()
    val rejectIntent = getRejectCallIntent(context, callData, requestCode)
    val acceptIntent = getAcceptCallIntent(context, callData, requestCode)

    val notificationBuilder = NotificationCompat.Builder(context, CALL_CHANNEL_ID)
    notificationBuilder
        .addPerson(person)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setAutoCancel(false)
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_CALL)
        .setSound(ringtone)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setTimeoutAfter(durationMs)
        .setContentTitle(title)
        .setContentText(callName)

    // Same pattern as flutter_callkit_incoming:
    //  - heads-up / collapsed → compact horizontal layout
    //  - expanded drawer → full Decline/Accept pills with labels
    //  - DecoratedCustomViewStyle for system light/dark card chrome
    val headsUpViews = buildCallRemoteViews(
        context,
        "notification_incoming_call_heads_up",
        title,
        callName,
        acceptButtonLabel,
        rejectButtonLabel,
        acceptButtonBgColor,
        acceptButtonTextColor,
        rejectButtonBgColor,
        rejectButtonTextColor,
        rejectIntent,
        acceptIntent,
        appIcon
    )
    val bigViews = buildCallRemoteViews(
        context,
        "notification_incoming_call",
        title,
        callName,
        acceptButtonLabel,
        rejectButtonLabel,
        acceptButtonBgColor,
        acceptButtonTextColor,
        rejectButtonBgColor,
        rejectButtonTextColor,
        rejectIntent,
        acceptIntent,
        appIcon
    )
    applyCustomCallRemoteViews(notificationBuilder, headsUpViews, bigViews)

    // Body tap opens the full-screen incoming UI (Accept/Decline still use their own intents).
    val contentPendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        createStartIncomingScreenIntent(
            context,
            callData.getString(EXTRA_CALL_ID) ?: "",
            callData.getInt(EXTRA_CALL_TYPE, 0),
            callData.getInt(EXTRA_CALL_INITIATOR_ID, 0),
            callData.getString(EXTRA_CALL_INITIATOR_NAME) ?: title,
            callData.getIntegerArrayList(EXTRA_CALL_OPPONENTS) ?: arrayListOf(),
            callData.getString(EXTRA_CALL_PHOTO),
            callData.getString(EXTRA_CALL_USER_INFO) ?: "{}",
            callData.getString(EXTRA_ACCEPT_BUTTON_LABEL),
            callData.getString(EXTRA_REJECT_BUTTON_LABEL),
            callData.getString(EXTRA_ACCEPT_BUTTON_BG_COLOR),
            callData.getString(EXTRA_ACCEPT_BUTTON_TEXT_COLOR),
            callData.getString(EXTRA_REJECT_BUTTON_BG_COLOR),
            callData.getString(EXTRA_REJECT_BUTTON_TEXT_COLOR),
            callData.getLong(EXTRA_CALL_DURATION, durationMs),
            callData.getString(EXTRA_BACKGROUND_COLOR),
            callData.getString(EXTRA_BACKGROUND_URL),
            callData.getString(EXTRA_LOGO_URL),
            callData.getBoolean(EXTRA_IS_SHOW_LOGO, false),
            callData.getString(EXTRA_TEXT_COLOR)
        ),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
    )
    notificationBuilder.setContentIntent(contentPendingIntent)

    return notificationBuilder
}

/**
 * Applies custom layouts like flutter_callkit_incoming:
 * content + heads-up = compact, big = expanded drawer.
 */
fun applyCustomCallRemoteViews(
    notificationBuilder: NotificationCompat.Builder,
    headsUpOrCollapsed: RemoteViews,
    expanded: RemoteViews = headsUpOrCollapsed
) {
    notificationBuilder
        .setStyle(NotificationCompat.DecoratedCustomViewStyle())
        .setCustomContentView(RemoteViews(headsUpOrCollapsed))
        .setCustomHeadsUpContentView(RemoteViews(headsUpOrCollapsed))
        .setCustomBigContentView(RemoteViews(expanded))
}

fun parseColorOr(color: String?, fallbackColor: String): Int {
    return try {
        Color.parseColor(if (TextUtils.isEmpty(color)) fallbackColor else color)
    } catch (_: Exception) {
        Color.parseColor(fallbackColor)
    }
}

fun buildCallRemoteViews(
    context: Context,
    title: String,
    callName: String?,
    acceptButtonLabel: String?,
    rejectButtonLabel: String?,
    acceptButtonBgColor: String?,
    acceptButtonTextColor: String?,
    rejectButtonBgColor: String?,
    rejectButtonTextColor: String?,
    rejectIntent: PendingIntent,
    acceptIntent: PendingIntent,
    appIcon: Bitmap? = null
): RemoteViews {
    return buildCallRemoteViews(
        context,
        "notification_incoming_call",
        title,
        callName,
        acceptButtonLabel,
        rejectButtonLabel,
        acceptButtonBgColor,
        acceptButtonTextColor,
        rejectButtonBgColor,
        rejectButtonTextColor,
        rejectIntent,
        acceptIntent,
        appIcon
    )
}

fun buildCallRemoteViews(
    context: Context,
    layoutName: String,
    title: String,
    callName: String?,
    acceptButtonLabel: String?,
    rejectButtonLabel: String?,
    acceptButtonBgColor: String?,
    acceptButtonTextColor: String?,
    rejectButtonBgColor: String?,
    rejectButtonTextColor: String?,
    rejectIntent: PendingIntent,
    acceptIntent: PendingIntent,
    appIcon: Bitmap? = null
): RemoteViews {
    val res = context.resources
    val pkg = context.packageName
    val layoutId = res.getIdentifier(layoutName, "layout", pkg)
    val remoteViews = RemoteViews(pkg, layoutId)
    val appIconId = res.getIdentifier("notification_app_icon_img", "id", pkg)
    val callerNameTxtId = res.getIdentifier("notification_caller_name_txt", "id", pkg)
    val callTypeTxtId = res.getIdentifier("notification_call_type_txt", "id", pkg)
    val rejectActionId = res.getIdentifier("notification_reject_action", "id", pkg)
    val acceptActionId = res.getIdentifier("notification_accept_action", "id", pkg)
    val rejectBtnId = res.getIdentifier("notification_reject_btn", "id", pkg)
    val acceptBtnId = res.getIdentifier("notification_accept_btn", "id", pkg)

    if (appIcon != null && appIconId != 0) {
        remoteViews.setImageViewBitmap(appIconId, appIcon)
        remoteViews.setViewVisibility(appIconId, android.view.View.VISIBLE)
    } else if (appIconId != 0) {
        val appIconRes = context.applicationInfo.icon
        if (appIconRes != 0) {
            remoteViews.setImageViewResource(appIconId, appIconRes)
            remoteViews.setViewVisibility(appIconId, android.view.View.VISIBLE)
        } else {
            remoteViews.setViewVisibility(appIconId, android.view.View.GONE)
        }
    }

    remoteViews.setTextViewText(callerNameTxtId, title)
    remoteViews.setTextViewText(callTypeTxtId, callName ?: "")
    if (rejectBtnId != 0) {
        remoteViews.setTextViewText(rejectBtnId, rejectButtonLabel ?: "Decline")
    }
    if (acceptBtnId != 0) {
        remoteViews.setTextViewText(acceptBtnId, acceptButtonLabel ?: "Accept")
    }

    // Clicks on the whole action container (like flutter_callkit_incoming llDecline/llAccept).
    val rejectClickId = if (rejectActionId != 0) rejectActionId else rejectBtnId
    val acceptClickId = if (acceptActionId != 0) acceptActionId else acceptBtnId
    if (rejectClickId != 0) remoteViews.setOnClickPendingIntent(rejectClickId, rejectIntent)
    if (acceptClickId != 0) remoteViews.setOnClickPendingIntent(acceptClickId, acceptIntent)

    // Fallback explicit colors when Compat TextAppearance fails on some OEMs.
    val (titleColor, subtitleColor) = notificationTextColors(context)
    if (callerNameTxtId != 0) remoteViews.setTextColor(callerNameTxtId, titleColor)
    if (callTypeTxtId != 0) remoteViews.setTextColor(callTypeTxtId, subtitleColor)

    val declineColor = parseColorOr(rejectButtonBgColor, "#F44336")
    val acceptColor = parseColorOr(acceptButtonBgColor, "#4CAF50")
    val declineText = parseColorOr(rejectButtonTextColor, "#FFFFFF")
    val acceptText = parseColorOr(acceptButtonTextColor, "#FFFFFF")

    if (rejectActionId != 0) {
        applyRemoteBackgroundTint(remoteViews, rejectActionId, declineColor)
    }
    if (acceptActionId != 0) {
        applyRemoteBackgroundTint(remoteViews, acceptActionId, acceptColor)
    }
    if (rejectBtnId != 0) remoteViews.setTextColor(rejectBtnId, declineText)
    if (acceptBtnId != 0) remoteViews.setTextColor(acceptBtnId, acceptText)

    return remoteViews
}

/** Light/dark text colors for custom call notification content. */
fun notificationTextColors(context: Context): Pair<Int, Int> {
    val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    return if (night) {
        Color.parseColor("#E8EAED") to Color.parseColor("#9AA0A6")
    } else {
        Color.parseColor("#1F1F1F") to Color.parseColor("#5F6368")
    }
}

fun applyRemoteBackgroundTint(
    remoteViews: RemoteViews,
    viewId: Int,
    backgroundColor: Int
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        remoteViews.setColorStateList(
            viewId,
            "setBackgroundTintList",
            ColorStateList.valueOf(backgroundColor)
        )
    } else {
        remoteViews.setInt(viewId, "setBackgroundColor", backgroundColor)
    }
}

fun applyRemoteButtonColors(
    remoteViews: RemoteViews,
    viewId: Int,
    backgroundColor: Int,
    textColor: Int
) {
    remoteViews.setTextColor(viewId, textColor)
    applyRemoteBackgroundTint(remoteViews, viewId, backgroundColor)
}

fun getAcceptCallIntent(
    context: Context,
    callData: Bundle,
    requestCode: Int
): PendingIntent {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        return PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context.applicationContext, NotificationTrampolineActivity::class.java)
                .setAction(ACTION_CALL_ACCEPT)
                .putExtras(callData),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    return PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, EventReceiver::class.java)
            .setAction(ACTION_CALL_ACCEPT)
            .putExtras(callData),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
    )
}

fun getRejectCallIntent(
    context: Context,
    callData: Bundle,
    requestCode: Int
): PendingIntent {
    return PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, EventReceiver::class.java)
            .setAction(ACTION_CALL_REJECT)
            .putExtras(callData),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
    )
}

fun addCallFullScreenIntent(
    context: Context,
    notificationBuilder: NotificationCompat.Builder,
    callId: String,
    callType: Int,
    callInitiatorId: Int,
    callInitiatorName: String,
    callOpponents: ArrayList<Int>,
    callPhoto: String?,
    userInfo: String,
    acceptButtonLabel: String? = null,
    rejectButtonLabel: String? = null,
    acceptButtonBgColor: String? = null,
    acceptButtonTextColor: String? = null,
    rejectButtonBgColor: String? = null,
    rejectButtonTextColor: String? = null,
    durationMs: Long = DEFAULT_CALL_DURATION_MS,
    backgroundColor: String? = null,
    backgroundUrl: String? = null,
    logoUrl: String? = null,
    isShowLogo: Boolean = false,
    textColor: String? = null
) {
    val callFullScreenIntent: Intent = createStartIncomingScreenIntent(
        context,
        callId,
        callType,
        callInitiatorId,
        callInitiatorName,
        callOpponents,
        callPhoto,
        userInfo,
        acceptButtonLabel,
        rejectButtonLabel,
        acceptButtonBgColor,
        acceptButtonTextColor,
        rejectButtonBgColor,
        rejectButtonTextColor,
        durationMs,
        backgroundColor,
        backgroundUrl,
        logoUrl,
        isShowLogo,
        textColor
    )
    val fullScreenPendingIntent = PendingIntent.getActivity(
        context,
        callId.hashCode(),
        callFullScreenIntent,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
    )
    notificationBuilder.setFullScreenIntent(fullScreenPendingIntent, true)
}

fun addCancelCallNotificationIntent(
    appContext: Context?,
    notificationBuilder: NotificationCompat.Builder,
    requestCode: Int,
    callData: Bundle
) {

    val deleteCallNotificationPendingIntent = PendingIntent.getBroadcast(
        appContext,
        requestCode,
        Intent(appContext, EventReceiver::class.java)
            .setAction(ACTION_CALL_NOTIFICATION_CANCELED)
            .putExtras(callData),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
    )
    notificationBuilder.setDeleteIntent(deleteCallNotificationPendingIntent)
}

fun createCallNotificationChannel(
    notificationManager: NotificationManagerCompat,
    sound: Uri,
    channelName: String = CALL_CHANNEL_NAME
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CALL_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.setSound(
            sound, AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()
        )
        channel.enableVibration(true)
        channel.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(channel)
    }
}

fun createMissedCallNotificationChannel(
    notificationManager: NotificationManagerCompat,
    channelName: String
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            MISSED_CALL_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.enableVibration(true)
        channel.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(channel)
    }
}

fun setNotificationSmallIcon(
    context: Context,
    notificationBuilder: NotificationCompat.Builder,
    isVideoCall: Boolean
) {
    val appMetadata = context.packageManager.getApplicationInfo(
        context.packageName,
        PackageManager.GET_META_DATA
    ).metaData

    var iconId =
        if (isVideoCall) appMetadata.getInt("com.connectycube.flutter.connectycube_flutter_call_kit.video_call_notification_icon") else appMetadata.getInt(
            "com.connectycube.flutter.connectycube_flutter_call_kit.audio_call_notification_icon"
        )
    if (iconId == 0) {
        iconId =
            appMetadata.getInt("com.connectycube.flutter.connectycube_flutter_call_kit.app_notification_icon")
    }

    if (iconId == 0) {
        try {
            val customIconOld = getString(context, "notification_icon")
            iconId = context.resources.getIdentifier(customIconOld, "drawable", context.packageName)
        } catch (e: Exception) {
            iconId = context.applicationInfo.icon
        }
    }

    if (iconId == 0) {
        iconId = context.applicationInfo.icon
    }

    notificationBuilder.setSmallIcon(iconId)
}

fun setNotificationLargeIcon(
    notificationBuilder: NotificationCompat.Builder,
    largeIcon: Bitmap
) {
    notificationBuilder.setLargeIcon(largeIcon)
}

fun setNotificationColor(
    context: Context,
    notificationBuilder: NotificationCompat.Builder,
    actionColor: String? = null
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val color = if (!TextUtils.isEmpty(actionColor)) {
            actionColor
        } else {
            getString(context, "color")
        }

        if (!TextUtils.isEmpty(color)) {
            try {
                notificationBuilder.color = Color.parseColor(color)
            } catch (_: Exception) {
                notificationBuilder.color = Color.parseColor("#4CAF50")
            }
        } else {
            val accentID = context.resources.getIdentifier(
                "call_notification_color_accent",
                "color",
                context.packageName
            )
            if (accentID != 0) {
                notificationBuilder.color = context.resources.getColor(accentID, null)
            } else {
                notificationBuilder.color = Color.parseColor("#4CAF50")
            }
        }
    }
}

fun canUseFullScreenIntent(context: Context): Boolean {
    return NotificationManagerCompat.from(context).canUseFullScreenIntent()
}

fun processCallTimeout(context: Context, extras: Bundle?) {
    if (extras == null) return
    val callId = extras.getString(EXTRA_CALL_ID) ?: return
    val state = getCallState(context, callId)
    if (state != CALL_STATE_PENDING) {
        Log.d("NotificationsManager", "[processCallTimeout] skip, state=$state")
        return
    }

    cancelCallNotification(context, callId)
    saveCallState(context, callId, CALL_STATE_REJECTED)

    val broadcastIntent = Intent(ACTION_CALL_TIMEOUT)
    broadcastIntent.putExtras(extras)
    androidx.localbroadcastmanager.content.LocalBroadcastManager
        .getInstance(context.applicationContext)
        .sendBroadcast(broadcastIntent)

    // Also notify IncomingCallActivity to close
    val endedIntent = Intent(ACTION_CALL_ENDED)
    endedIntent.putExtra(EXTRA_CALL_ID, callId)
    androidx.localbroadcastmanager.content.LocalBroadcastManager
        .getInstance(context.applicationContext)
        .sendBroadcast(endedIntent)

    showMissCallNotification(
        context,
        callId,
        extras.getInt(EXTRA_CALL_TYPE, 0),
        extras.getInt(EXTRA_CALL_INITIATOR_ID, 0),
        extras.getString(EXTRA_CALL_INITIATOR_NAME) ?: "",
        extras.getIntegerArrayList(EXTRA_CALL_OPPONENTS) ?: arrayListOf(),
        extras.getString(EXTRA_CALL_PHOTO),
        extras.getString(EXTRA_CALL_USER_INFO) ?: "{}",
        extras.getBoolean(EXTRA_MISSED_SHOW, true),
        extras.getString(EXTRA_MISSED_SUBTITLE) ?: "Missed call",
        extras.getString(EXTRA_MISSED_CALLBACK_TEXT) ?: "Call back",
        extras.getBoolean(EXTRA_MISSED_SHOW_CALLBACK, true),
        extras.getInt(EXTRA_MISSED_COUNT, 1),
        extras.getString(EXTRA_MISSED_ID) ?: callId,
        extras.getString(EXTRA_MISSED_CHANNEL_NAME) ?: "Missed Call",
        extras.getString(EXTRA_ACTION_COLOR)
    )
}
