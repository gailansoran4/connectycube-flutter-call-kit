package com.connectycube.flutter.connectycube_flutter_call_kit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.ContextHolder
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject


class ConnectycubeFCMReceiver : BroadcastReceiver() {
    private val TAG = "ConnectycubeFCMReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "broadcast received for message")

        ContextHolder.applicationContext = context!!.applicationContext

        if (intent!!.extras == null) {
            Log.d(
                TAG,
                "broadcast received but intent contained no extras to process RemoteMessage. Operation cancelled."
            )
            return
        }

        val remoteMessage = RemoteMessage(intent.extras!!)

        val data = remoteMessage.data
        if (data.containsKey("signal_type")) {
            when (data["signal_type"]) {
                "startCall" -> {
                    processInviteCallEvent(context.applicationContext, data)
                }

                "endCall" -> {
                    processEndCallEvent(context.applicationContext, data)
                }

                "rejectCall" -> {
                    processEndCallEvent(context.applicationContext, data)
                }
            }

        }
    }

    private fun processEndCallEvent(applicationContext: Context, data: Map<String, String>) {
        Log.d(TAG, "[processEndCallEvent]")

        val callId = data["session_id"] ?: return


        processCallEnded(applicationContext, callId)
    }

    private fun processInviteCallEvent(applicationContext: Context, data: Map<String, String>) {
        Log.d(TAG, "[processInviteCallEvent]")
        val callId = data["session_id"]

        if (callId == null || CALL_STATE_UNKNOWN != getCallState(
                applicationContext,
                callId
            )
        ) {
            Log.d(TAG, "[processInviteCallEvent] callId == null || CALL_STATE_UNKNOWN != getCallState(applicationContext, callId)")
            return
        }

        val callType = data["call_type"]?.toInt()
        val callInitiatorId = data["caller_id"]?.toInt()
        val callInitiatorName = data["caller_name"]
        val callPhoto = data["photo_url"]
        val callOpponentsString = data["call_opponents"]
        var callOpponents = ArrayList<Int>()
        if (callOpponentsString != null) {
            callOpponents = ArrayList(callOpponentsString.split(',').map { it.toInt() })
        }
        // Prefer explicit user_info JSON; otherwise pass through flat FCM data keys
        // (e.g. order_id) so accept/reject handlers still receive them.
        val userInfo = data["user_info"] ?: JSONObject(data as Map<*, *>).toString()

        if (callType == null || callInitiatorId == null || callInitiatorName == null || callOpponents.isEmpty()) {
            Log.d(TAG, "[processInviteCallEvent] callType == null || callInitiatorId == null || callInitiatorName == null || callOpponents.isEmpty()")
            return
        }

        // Push payload + persisted updateConfig defaults (same resolution as Dart API).
        val args: Map<String, Any?> = data.mapValues { it.value as Any? }
        val acceptButtonLabel = CallParamsHelper.acceptLabel(args)
        val rejectButtonLabel = CallParamsHelper.rejectLabel(args)
        val acceptButtonBgColor = CallParamsHelper.acceptBackgroundColor(args)
        val acceptButtonTextColor = CallParamsHelper.acceptTextColor(args)
        val rejectButtonBgColor = CallParamsHelper.rejectBackgroundColor(args)
        val rejectButtonTextColor = CallParamsHelper.rejectTextColor(args)
        val durationMs = CallParamsHelper.durationMs(args, applicationContext)
        val ringtonePath = CallParamsHelper.ringtonePath(args, applicationContext)
        val backgroundColor = CallParamsHelper.backgroundColor(args, applicationContext)
        val backgroundUrl = CallParamsHelper.backgroundUrl(args, applicationContext)
        val logoUrl = CallParamsHelper.logoUrl(args, applicationContext)
        val isShowLogo = CallParamsHelper.isShowLogo(args, applicationContext)
        val textColor = CallParamsHelper.textColor(args, applicationContext)
        val actionColor = CallParamsHelper.actionColor(args, applicationContext)
        val channelName = CallParamsHelper.incomingChannelName(args, applicationContext)
        val missedShow = CallParamsHelper.missedShow(args, applicationContext)
        val missedSubtitle = CallParamsHelper.missedSubtitle(args, applicationContext)
        val missedCallbackText = CallParamsHelper.missedCallbackText(args, applicationContext)
        val missedShowCallback = CallParamsHelper.missedShowCallback(args, applicationContext)
        val missedCount = CallParamsHelper.missedCount(args)
        val missedId = CallParamsHelper.missedId(args, callId)
        val missedChannelName = CallParamsHelper.missedChannelName(args, applicationContext)

        notifyAboutIncomingCall(
            applicationContext,
            callId,
            callType,
            callInitiatorId,
            callInitiatorName,
            callOpponents,
            callPhoto,
            userInfo
        )

        showCallNotification(
            applicationContext,
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
            ringtonePath,
            backgroundColor,
            backgroundUrl,
            logoUrl,
            isShowLogo,
            textColor,
            actionColor,
            channelName,
            missedShow,
            missedSubtitle,
            missedCallbackText,
            missedShowCallback,
            missedCount,
            missedId,
            missedChannelName
        )

        saveCallState(applicationContext, callId, CALL_STATE_PENDING)
        saveCallData(applicationContext, callId, data)
        saveCallId(applicationContext, callId)
    }
}
