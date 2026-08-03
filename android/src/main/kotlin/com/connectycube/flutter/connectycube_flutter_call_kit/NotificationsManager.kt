package com.connectycube.flutter.connectycube_flutter_call_kit

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

    val usesCustomRemoteViews = hasCustomCallButtons(
        acceptButtonLabel,
        rejectButtonLabel,
        acceptButtonBgColor,
        acceptButtonTextColor,
        rejectButtonBgColor,
        rejectButtonTextColor
    )

    if (remoteLogo != null) {
        loadLogoAndPostNotification(
            context,
            notificationManager,
            builder,
            callId.hashCode(),
            remoteLogo,
            logoFallback,
            usesCustomRemoteViews,
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
        if (!usesCustomRemoteViews) {
            // CallStyle path: system largeIcon on the right.
            setNotificationLargeIcon(builder, logoFallback)
        }
        // Custom RemoteViews already embed the app icon — skip largeIcon
        // so the system shade keeps a clean light/dark card.
        postNotification(callId.hashCode(), notificationManager, builder)
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

    val missedSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    val builder = NotificationCompat.Builder(context, MISSED_CALL_CHANNEL_ID)
        .setContentTitle(callInitiatorName)
        .setContentText(if (TextUtils.isEmpty(userInfo)) "" else "")
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
            val rejectIntent = getRejectCallIntent(context, callData, title.hashCode())
            val acceptIntent = getAcceptCallIntent(context, callData, title.hashCode())
            val remoteViews = buildCallRemoteViews(
                context,
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
            builder
                .setCustomContentView(remoteViews)
                .setCustomBigContentView(remoteViews)
                .setCustomHeadsUpContentView(remoteViews)
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

    val rejectIntent = getRejectCallIntent(context, callData, title.hashCode())
    val acceptIntent = getAcceptCallIntent(context, callData, title.hashCode())

    val hasCustomButtons = hasCustomCallButtons(
        acceptButtonLabel,
        rejectButtonLabel,
        acceptButtonBgColor,
        acceptButtonTextColor,
        rejectButtonBgColor,
        rejectButtonTextColor
    )

    val notificationBuilder = NotificationCompat.Builder(context, CALL_CHANNEL_ID)
    notificationBuilder
        .setContentText(callName)
        .addPerson(person)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setAutoCancel(false)
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_CALL)
        .setSound(ringtone)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setTimeoutAfter(durationMs)

    if (hasCustomButtons) {
        // CallStyle does not allow custom action labels/colors, so render the
        // notification with a custom layout when button customization is requested.
        // App icon is drawn inside the RemoteViews; no system largeIcon so the
        // shade keeps native light/dark card chrome.
        val remoteViews = buildCallRemoteViews(
            context,
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
        notificationBuilder
            .setContentTitle(title)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setCustomHeadsUpContentView(remoteViews)
    } else {
        val style = NotificationCompat.CallStyle.forIncomingCall(
            person,
            rejectIntent,
            acceptIntent
        )
        style.setIsVideo(isVideoCall)
        notificationBuilder.setStyle(style)
    }
    return notificationBuilder
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
    val res = context.resources
    val pkg = context.packageName
    val remoteViews =
        RemoteViews(pkg, res.getIdentifier("notification_incoming_call", "layout", pkg))
    val appIconId = res.getIdentifier("notification_app_icon_img", "id", pkg)
    val callerNameTxtId = res.getIdentifier("notification_caller_name_txt", "id", pkg)
    val callTypeTxtId = res.getIdentifier("notification_call_type_txt", "id", pkg)
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
    remoteViews.setTextViewText(rejectBtnId, rejectButtonLabel ?: "Decline")
    remoteViews.setTextViewText(acceptBtnId, acceptButtonLabel ?: "Accept")
    remoteViews.setOnClickPendingIntent(rejectBtnId, rejectIntent)
    remoteViews.setOnClickPendingIntent(acceptBtnId, acceptIntent)

    applyRemoteButtonColors(
        remoteViews,
        rejectBtnId,
        parseColorOr(rejectButtonBgColor, "#E02B00"),
        parseColorOr(rejectButtonTextColor, "#FFFFFF")
    )
    applyRemoteButtonColors(
        remoteViews,
        acceptBtnId,
        parseColorOr(acceptButtonBgColor, "#4CB050"),
        parseColorOr(acceptButtonTextColor, "#FFFFFF")
    )
    return remoteViews
}

fun applyRemoteButtonColors(
    remoteViews: RemoteViews,
    viewId: Int,
    backgroundColor: Int,
    textColor: Int
) {
    remoteViews.setTextColor(viewId, textColor)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Tints the rounded shape drawable, keeping the pill-button look.
        remoteViews.setColorStateList(
            viewId,
            "setBackgroundTintList",
            ColorStateList.valueOf(backgroundColor)
        )
    } else {
        remoteViews.setInt(viewId, "setBackgroundColor", backgroundColor)
    }
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
