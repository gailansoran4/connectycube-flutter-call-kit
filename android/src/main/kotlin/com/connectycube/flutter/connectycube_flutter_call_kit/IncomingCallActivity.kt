package com.connectycube.flutter.connectycube_flutter_call_kit

import android.app.Activity
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.getCallBackgroundResId
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.getLogoResId
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.getPhotoPlaceholderResId
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.resolveDrawableOrAssetUrl
import com.google.android.material.imageview.ShapeableImageView
import com.skyfishjy.library.RippleBackground


fun createStartIncomingScreenIntent(
    context: Context, callId: String, callType: Int, callInitiatorId: Int,
    callInitiatorName: String, opponents: ArrayList<Int>, callPhoto: String?, userInfo: String,
    acceptButtonLabel: String? = null, rejectButtonLabel: String? = null,
    durationMs: Long = DEFAULT_CALL_DURATION_MS,
    backgroundColor: String? = null,
    backgroundUrl: String? = null,
    logoUrl: String? = null,
    isShowLogo: Boolean = false,
    textColor: String? = null
): Intent {
    val intent = Intent(context, IncomingCallActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    intent.putExtra(EXTRA_CALL_ID, callId)
    intent.putExtra(EXTRA_CALL_TYPE, callType)
    intent.putExtra(EXTRA_CALL_INITIATOR_ID, callInitiatorId)
    intent.putExtra(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
    intent.putIntegerArrayListExtra(EXTRA_CALL_OPPONENTS, opponents)
    intent.putExtra(EXTRA_CALL_PHOTO, callPhoto)
    intent.putExtra(EXTRA_CALL_USER_INFO, userInfo)
    intent.putExtra(EXTRA_ACCEPT_BUTTON_LABEL, acceptButtonLabel)
    intent.putExtra(EXTRA_REJECT_BUTTON_LABEL, rejectButtonLabel)
    intent.putExtra(EXTRA_CALL_DURATION, durationMs)
    intent.putExtra(EXTRA_BACKGROUND_COLOR, backgroundColor)
    intent.putExtra(EXTRA_BACKGROUND_URL, backgroundUrl)
    intent.putExtra(EXTRA_LOGO_URL, logoUrl)
    intent.putExtra(EXTRA_IS_SHOW_LOGO, isShowLogo)
    intent.putExtra(EXTRA_TEXT_COLOR, textColor)
    return intent
}

class IncomingCallActivity : Activity() {
    private lateinit var callStateReceiver: BroadcastReceiver
    private lateinit var localBroadcastManager: LocalBroadcastManager

    private var callId: String? = null
    private var callType = -1
    private var callInitiatorId = -1
    private var callInitiatorName: String? = null
    private var callOpponents: ArrayList<Int>? = ArrayList()
    private var callPhoto: String? = null
    private var callUserInfo: String? = null
    private var acceptButtonLabel: String? = null
    private var rejectButtonLabel: String? = null
    private var durationMs: Long = DEFAULT_CALL_DURATION_MS
    private var backgroundColor: String? = null
    private var backgroundUrl: String? = null
    private var logoUrl: String? = null
    private var isShowLogo: Boolean = false
    private var textColor: String? = null

    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        val bundle = Bundle()
        bundle.putString(EXTRA_CALL_ID, callId)
        bundle.putInt(EXTRA_CALL_TYPE, callType)
        bundle.putInt(EXTRA_CALL_INITIATOR_ID, callInitiatorId)
        bundle.putString(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
        bundle.putIntegerArrayList(EXTRA_CALL_OPPONENTS, callOpponents)
        bundle.putString(EXTRA_CALL_PHOTO, callPhoto)
        bundle.putString(EXTRA_CALL_USER_INFO, callUserInfo)
        val timeoutIntent = Intent(this, EventReceiver::class.java)
        timeoutIntent.action = ACTION_CALL_TIMEOUT
        timeoutIntent.putExtras(bundle)
        // Restore missed params from saved call data if available
        callId?.let { id ->
            val data = getCallData(applicationContext, id)
            if (data != null) {
                timeoutIntent.putExtra(
                    EXTRA_MISSED_SHOW,
                    CallParamsHelper.missedShow(data as Map<String, Any?>, applicationContext)
                )
                timeoutIntent.putExtra(
                    EXTRA_MISSED_SUBTITLE,
                    CallParamsHelper.missedSubtitle(data, applicationContext)
                )
                timeoutIntent.putExtra(
                    EXTRA_MISSED_CALLBACK_TEXT,
                    CallParamsHelper.missedCallbackText(data, applicationContext)
                )
                timeoutIntent.putExtra(
                    EXTRA_MISSED_SHOW_CALLBACK,
                    CallParamsHelper.missedShowCallback(data, applicationContext)
                )
                timeoutIntent.putExtra(
                    EXTRA_MISSED_COUNT,
                    CallParamsHelper.missedCount(data)
                )
                timeoutIntent.putExtra(
                    EXTRA_MISSED_ID,
                    CallParamsHelper.missedId(data, id)
                )
                timeoutIntent.putExtra(
                    EXTRA_MISSED_CHANNEL_NAME,
                    CallParamsHelper.missedChannelName(data, applicationContext)
                )
                timeoutIntent.putExtra(
                    EXTRA_ACTION_COLOR,
                    CallParamsHelper.actionColor(data, applicationContext)
                )
            }
        }
        applicationContext.sendBroadcast(timeoutIntent)
    }


    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(resources.getIdentifier("activity_incoming_call", "layout", packageName))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setInheritShowWhenLocked(true)
        }

        with(getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requestDismissKeyguard(this@IncomingCallActivity, object :
                    KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissError() {
                        Log.d("IncomingCallActivity", "[KeyguardDismissCallback.onDismissError]")
                    }

                    override fun onDismissSucceeded() {
                        Log.d(
                            "IncomingCallActivity",
                            "[KeyguardDismissCallback.onDismissSucceeded]"
                        )
                    }

                    override fun onDismissCancelled() {
                        Log.d(
                            "IncomingCallActivity",
                            "[KeyguardDismissCallback.onDismissCancelled]"
                        )
                    }
                })
            }
        }

        processIncomingData(intent)
        initUi()
        initCallStateReceiver()
        registerCallStateReceiver()
        timeoutHandler.postDelayed(timeoutRunnable, durationMs)
    }

    private fun initCallStateReceiver() {
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        callStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null || TextUtils.isEmpty(intent.action)) return
                val action: String? = intent.action

                val callIdToProcess: String? = intent.getStringExtra(EXTRA_CALL_ID)
                if (TextUtils.isEmpty(callIdToProcess) || callIdToProcess != callId) {
                    return
                }
                when (action) {
                    ACTION_CALL_NOTIFICATION_CANCELED, ACTION_CALL_REJECT, ACTION_CALL_ENDED, ACTION_CALL_TIMEOUT -> {
                        timeoutHandler.removeCallbacks(timeoutRunnable)
                        finishAndRemoveTask()
                    }

                    ACTION_CALL_ACCEPT -> {
                        timeoutHandler.removeCallbacks(timeoutRunnable)
                        finishDelayed()
                    }
                }
            }
        }
    }

    private fun finishDelayed() {
        Handler(Looper.getMainLooper()).postDelayed({
            finishAndRemoveTask()
        }, 1000)
    }

    private fun registerCallStateReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_CALL_NOTIFICATION_CANCELED)
        intentFilter.addAction(ACTION_CALL_REJECT)
        intentFilter.addAction(ACTION_CALL_ACCEPT)
        intentFilter.addAction(ACTION_CALL_ENDED)
        intentFilter.addAction(ACTION_CALL_TIMEOUT)
        localBroadcastManager.registerReceiver(callStateReceiver, intentFilter)
    }

    private fun unRegisterCallStateReceiver() {
        localBroadcastManager.unregisterReceiver(callStateReceiver)
    }

    override fun onDestroy() {
        timeoutHandler.removeCallbacks(timeoutRunnable)
        super.onDestroy()
        unRegisterCallStateReceiver()
    }

    private fun processIncomingData(intent: Intent) {
        callId = intent.getStringExtra(EXTRA_CALL_ID)
        callType = intent.getIntExtra(EXTRA_CALL_TYPE, -1)
        callInitiatorId = intent.getIntExtra(EXTRA_CALL_INITIATOR_ID, -1)
        callInitiatorName = intent.getStringExtra(EXTRA_CALL_INITIATOR_NAME)
        callOpponents = intent.getIntegerArrayListExtra(EXTRA_CALL_OPPONENTS)
        callPhoto = intent.getStringExtra(EXTRA_CALL_PHOTO)
        callUserInfo = intent.getStringExtra(EXTRA_CALL_USER_INFO)
        acceptButtonLabel = intent.getStringExtra(EXTRA_ACCEPT_BUTTON_LABEL)
        rejectButtonLabel = intent.getStringExtra(EXTRA_REJECT_BUTTON_LABEL)
        durationMs = intent.getLongExtra(EXTRA_CALL_DURATION, DEFAULT_CALL_DURATION_MS)
        backgroundColor = intent.getStringExtra(EXTRA_BACKGROUND_COLOR)
        backgroundUrl = intent.getStringExtra(EXTRA_BACKGROUND_URL)
        logoUrl = intent.getStringExtra(EXTRA_LOGO_URL)
        isShowLogo = intent.getBooleanExtra(EXTRA_IS_SHOW_LOGO, false)
        textColor = intent.getStringExtra(EXTRA_TEXT_COLOR)
    }

    private fun applyButtonLabel(textViewId: String, label: String?) {
        val labelView: TextView =
            findViewById(resources.getIdentifier(textViewId, "id", packageName))
        if (TextUtils.isEmpty(label)) {
            labelView.visibility = View.GONE
            return
        }
        labelView.text = label
        labelView.visibility = View.VISIBLE
        applyTextColor(labelView)
    }

    private fun applyTextColor(textView: TextView) {
        if (!TextUtils.isEmpty(textColor)) {
            try {
                textView.setTextColor(Color.parseColor(textColor))
            } catch (_: Exception) {
            }
        }
    }

    private fun initUi() {
        val root = findViewById<View>(android.R.id.content)
        if (!TextUtils.isEmpty(backgroundColor)) {
            try {
                root.setBackgroundColor(Color.parseColor(backgroundColor))
            } catch (_: Exception) {
            }
        }

        val backgroundImg: ImageView =
            findViewById(resources.getIdentifier("call_background_img", "id", packageName))
        val bgDrawableName = if (!TextUtils.isEmpty(backgroundUrl) &&
            !backgroundUrl!!.startsWith("http", true) &&
            !backgroundUrl!!.contains("/")
        ) {
            backgroundUrl
        } else {
            null
        }
        val backgroundResId = getCallBackgroundResId(applicationContext, bgDrawableName)
        val remoteBg = resolveDrawableOrAssetUrl(applicationContext, backgroundUrl)
        when {
            remoteBg != null || (backgroundUrl != null && backgroundUrl!!.startsWith("http", true)) -> {
                backgroundImg.visibility = View.VISIBLE
                Glide.with(applicationContext)
                    .load(remoteBg ?: backgroundUrl)
                    .into(backgroundImg)
            }
            backgroundResId != 0 -> {
                backgroundImg.setImageResource(backgroundResId)
                backgroundImg.visibility = View.VISIBLE
            }
            else -> backgroundImg.visibility = View.GONE
        }

        val logoImg: ImageView =
            findViewById(resources.getIdentifier("call_logo_img", "id", packageName))
        if (isShowLogo) {
            val logoDrawableName = if (!TextUtils.isEmpty(logoUrl) &&
                !logoUrl!!.startsWith("http", true) &&
                !logoUrl!!.contains("/")
            ) {
                logoUrl
            } else null
            val logoResId = getLogoResId(applicationContext, logoDrawableName)
            val remoteLogo = resolveDrawableOrAssetUrl(applicationContext, logoUrl)
            when {
                remoteLogo != null || (logoUrl != null && logoUrl!!.startsWith("http", true)) -> {
                    logoImg.visibility = View.VISIBLE
                    Glide.with(applicationContext).load(remoteLogo ?: logoUrl).into(logoImg)
                }
                logoResId != 0 -> {
                    logoImg.setImageResource(logoResId)
                    logoImg.visibility = View.VISIBLE
                }
                else -> logoImg.visibility = View.GONE
            }
        } else {
            logoImg.visibility = View.GONE
        }

        val callTitleTxt: TextView =
            findViewById(resources.getIdentifier("user_name_txt", "id", packageName))
        callTitleTxt.text = callInitiatorName
        applyTextColor(callTitleTxt)
        val callSubTitleTxt: TextView =
            findViewById(resources.getIdentifier("call_type_txt", "id", packageName))
        callSubTitleTxt.text =
            String.format(CALL_TYPE_PLACEHOLDER, if (callType == 1) "Video" else "Audio")
        applyTextColor(callSubTitleTxt)

        val callAcceptButton: ImageView =
            findViewById(resources.getIdentifier("start_call_btn", "id", packageName))
        val acceptButtonIconName = if (callType == 1) "ic_video_call_start" else "ic_call_start"
        callAcceptButton.setImageResource(
            resources.getIdentifier(
                acceptButtonIconName,
                "drawable",
                packageName
            )
        )

        val avatarImg: ShapeableImageView =
            findViewById(resources.getIdentifier("avatar_img", "id", packageName))

        val defaultPhotoResId = getPhotoPlaceholderResId(applicationContext)

        if (!TextUtils.isEmpty(callPhoto)) {
            Glide.with(applicationContext)
                .load(resolveDrawableOrAssetUrl(applicationContext, callPhoto) ?: callPhoto)
                .error(defaultPhotoResId)
                .placeholder(defaultPhotoResId)
                .into(avatarImg)
        } else {
            avatarImg.setImageResource(defaultPhotoResId)
        }

        val acceptButtonAnimation: RippleBackground =
            findViewById(resources.getIdentifier("accept_button_animation", "id", packageName))
        acceptButtonAnimation.startRippleAnimation()

        val rejectButtonAnimation: RippleBackground =
            findViewById(resources.getIdentifier("reject_button_animation", "id", packageName))
        rejectButtonAnimation.startRippleAnimation()

        applyButtonLabel("accept_button_label_txt", acceptButtonLabel)
        applyButtonLabel("reject_button_label_txt", rejectButtonLabel)
    }

    // calls from layout file
    fun onEndCall(view: View?) {
        timeoutHandler.removeCallbacks(timeoutRunnable)
        val bundle = Bundle()
        bundle.putString(EXTRA_CALL_ID, callId)
        bundle.putInt(EXTRA_CALL_TYPE, callType)
        bundle.putInt(EXTRA_CALL_INITIATOR_ID, callInitiatorId)
        bundle.putString(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
        bundle.putIntegerArrayList(EXTRA_CALL_OPPONENTS, callOpponents)
        bundle.putString(EXTRA_CALL_PHOTO, callPhoto)
        bundle.putString(EXTRA_CALL_USER_INFO, callUserInfo)

        val endCallIntent = Intent(this, EventReceiver::class.java)
        endCallIntent.action = ACTION_CALL_REJECT
        endCallIntent.putExtras(bundle)
        applicationContext.sendBroadcast(endCallIntent)
    }

    // calls from layout file
    fun onStartCall(view: View?) {
        timeoutHandler.removeCallbacks(timeoutRunnable)
        val bundle = Bundle()
        bundle.putString(EXTRA_CALL_ID, callId)
        bundle.putInt(EXTRA_CALL_TYPE, callType)
        bundle.putInt(EXTRA_CALL_INITIATOR_ID, callInitiatorId)
        bundle.putString(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
        bundle.putIntegerArrayList(EXTRA_CALL_OPPONENTS, callOpponents)
        bundle.putString(EXTRA_CALL_PHOTO, callPhoto)
        bundle.putString(EXTRA_CALL_USER_INFO, callUserInfo)

        val startCallIntent = Intent(this, EventReceiver::class.java)
        startCallIntent.action = ACTION_CALL_ACCEPT
        startCallIntent.putExtras(bundle)
        applicationContext.sendBroadcast(startCallIntent)
    }
}
