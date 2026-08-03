import Flutter
import UIKit

class CallStreamHandler: NSObject, FlutterStreamHandler {
    
    public func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        print("[CallStreamHandler][onListen]");
        SwiftConnectycubeFlutterCallKitPlugin.callController.actionListener = { event, uuid, args in
            print("[CallStreamHandler][onListen] actionListener: \(event)")
            var data = ["event" : event.rawValue, "uuid": uuid.uuidString.lowercased()] as [String: Any]
            if args != nil{
                data["args"] = args!
            }
            events(data)
        }
        
        SwiftConnectycubeFlutterCallKitPlugin.voipController.tokenListener = { token in
            print("[CallStreamHandler][onListen] tokenListener: \(token)")
            let data: [String: Any] = ["event" : "voipToken", "args": ["voipToken" : token]]
            
            events(data)
        }
        
        return nil
    }
    
    public func onCancel(withArguments arguments: Any?) -> FlutterError? {
        print("[CallStreamHandler][onCancel]")
        SwiftConnectycubeFlutterCallKitPlugin.callController.actionListener = nil
        SwiftConnectycubeFlutterCallKitPlugin.voipController.tokenListener = nil
        return nil
    }
}

public class SwiftConnectycubeFlutterCallKitPlugin: NSObject, FlutterPlugin {
    static let _methodChannelName = "connectycube_flutter_call_kit.methodChannel";
    static let _callEventChannelName = "connectycube_flutter_call_kit.callEventChannel"
    static let callController = CallKitController()
    static let voipController = VoIPController(withCallKitController: callController)
    
    @objc public static func register(with registrar: FlutterPluginRegistrar) {
        print("[SwiftConnectycubeFlutterCallKitPlugin][register]")
        //setup method channels
        let methodChannel = FlutterMethodChannel(name: _methodChannelName, binaryMessenger: registrar.messenger())
        
        //setup event channels
        let callEventChannel = FlutterEventChannel(name: _callEventChannelName, binaryMessenger: registrar.messenger())
        callEventChannel.setStreamHandler(CallStreamHandler())
        
        let instance = SwiftConnectycubeFlutterCallKitPlugin()
        registrar.addMethodCallDelegate(instance, channel: methodChannel)
    }
    
    ///useful for integrating with VIOP notifications
    @objc static public func reportIncomingCall(uuid: String,
                                          callType: Int,
                                          callInitiatorId: Int,
                                          callInitiatorName: String,
                                          opponents: [Int],
                                          userInfo: String?, result: FlutterResult?){
        SwiftConnectycubeFlutterCallKitPlugin.callController.reportIncomingCall(uuid: uuid.lowercased(), callType: callType, callInitiatorId: callInitiatorId, callInitiatorName: callInitiatorName, opponents: opponents, userInfo: userInfo) { (error) in
            print("[SwiftConnectycubeFlutterCallKitPlugin] reportIncomingCall ERROR: \(error?.localizedDescription ?? "none")")
            result?(error == nil)
        }
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        print("[SwiftConnectycubeFlutterCallKitPlugin][handle] method: \(call.method)");
        let arguments = call.arguments as? Dictionary<String, Any>
        if call.method == "getVoipToken" {
            let voipToken = SwiftConnectycubeFlutterCallKitPlugin.voipController.getVoIPToken()
            result(voipToken)
        }
        else if call.method == "updateConfig" {
            guard let arguments = arguments else {
                result(FlutterError(code: "invalid_argument", message: "No data was provided.", details: nil))
                return
            }
            let ringtone = arguments["ringtone"] as? String
            let icon = arguments["icon"] as? String
            let defaultDurationMs = arguments["default_duration_ms"] as? Int
            let missedSubtitle = arguments["missed_call_subtitle"] as? String
            let missedCallbackText = arguments["missed_call_callback_text"] as? String
            let showMissed = arguments["show_missed_call_notification"] as? Bool
            let showCallback = arguments["show_missed_call_callback"] as? Bool
            CallKitController.updateConfig(
                ringtone: ringtone,
                icon: icon,
                defaultDurationMs: defaultDurationMs,
                missedSubtitle: missedSubtitle,
                missedCallbackText: missedCallbackText,
                showMissedCallNotification: showMissed,
                showMissedCallCallback: showCallback
            )
            
            result(true)
        }
        else if call.method == "showCallNotification" {
            guard let arguments = arguments else {
                result(FlutterError(code: "invalid_argument", message: "No data was provided.", details: nil))
                return
            }
            let callId = arguments["session_id"] as! String
            let callType = arguments["call_type"] as! Int
            let callInitiatorId = arguments["caller_id"] as! Int
            let callInitiatorName = arguments["caller_name"] as! String
            let callOpponentsString = arguments["call_opponents"] as! String
            let callOpponents = callOpponentsString.components(separatedBy: ",")
                .map { Int($0) ?? 0 }
            let userInfo = arguments["user_info"] as? String
            let duration = arguments["duration"] as? Int
            var missedShow: Bool? = nil
            var missedSubtitle: String? = nil
            var missedCallbackText: String? = nil
            var missedShowCallback: Bool? = nil
            if let missed = arguments["missed_call_notification"] as? [String: Any] {
                missedShow = missed["show_notification"] as? Bool
                missedSubtitle = missed["subtitle"] as? String
                missedCallbackText = missed["callback_text"] as? String
                missedShowCallback = missed["is_show_callback"] as? Bool
            }
            // Prefer top-level duration; fall back to android.duration_ms.
            let durationMs = duration ?? (arguments["android"] as? [String: Any])?["duration_ms"] as? Int
            let iosParams = arguments["ios"] as? [String: Any]
            // Apply per-call iOS ringtone/icon before reporting (also applied inside reportIncomingCall)
            if let ios = iosParams {
                CallKitController.applyIOSParams(ios)
            }
            
            SwiftConnectycubeFlutterCallKitPlugin.callController.reportIncomingCall(
                uuid: callId.lowercased(),
                callType: callType,
                callInitiatorId: callInitiatorId,
                callInitiatorName: callInitiatorName,
                opponents: callOpponents,
                userInfo: userInfo,
                durationMs: durationMs,
                missedShow: missedShow,
                missedSubtitle: missedSubtitle,
                missedCallbackText: missedCallbackText,
                missedShowCallback: missedShowCallback,
                iosParams: iosParams
            ) { (error) in
                print("[SwiftConnectycubeFlutterCallKitPlugin][handle] reportIncomingCall ERROR: \(error?.localizedDescription ?? "none")")
                result(error == nil)
            }
        }
        else if call.method == "showMissCallNotification" {
            guard let arguments = arguments else {
                result(FlutterError(code: "invalid_argument", message: "No data was provided.", details: nil))
                return
            }
            let callId = arguments["session_id"] as! String
            let callType = arguments["call_type"] as! Int
            let callInitiatorId = arguments["caller_id"] as! Int
            let callInitiatorName = arguments["caller_name"] as! String
            let callOpponents = arguments["call_opponents"] as? String ?? ""
            let userInfo = arguments["user_info"] as? String
            let photoUrl = arguments["photo_url"] as? String
            var show = true
            var subtitle = "Missed call"
            var callbackText = "Call back"
            var showCallback = true
            var count = 1
            if let missed = arguments["missed_call_notification"] as? [String: Any] {
                show = missed["show_notification"] as? Bool ?? true
                subtitle = missed["subtitle"] as? String ?? subtitle
                callbackText = missed["callback_text"] as? String ?? callbackText
                showCallback = missed["is_show_callback"] as? Bool ?? true
                count = missed["count"] as? Int ?? 1
            }
            MissedCallNotificationManager.shared.showMissedCall(
                sessionId: callId.lowercased(),
                callerName: callInitiatorName,
                callType: callType,
                callerId: callInitiatorId,
                opponents: callOpponents,
                photoUrl: photoUrl,
                userInfo: userInfo,
                showNotification: show,
                subtitle: subtitle,
                callbackText: callbackText,
                isShowCallback: showCallback,
                count: count
            )
            result(true)
        }
        else if call.method == "reportCallAccepted" {
            guard let arguments = arguments, let callId = arguments["session_id"] as? String else {
                result(FlutterError(code: "invalid_argument", message: "session_id was not provided.", details: nil))
                return
            }
            
            SwiftConnectycubeFlutterCallKitPlugin.callController.answerCall(uuid: callId)
            result(true)
        }
        else if call.method == "reportCallFinished" {
            guard let arguments = arguments else {
                result(FlutterError(code: "invalid_argument", message: "No data was provided.", details: nil))
                return
            }
            let callId = arguments["session_id"] as! String
            let reason = arguments["reason"] as! String

            guard let callUUID = CallKitController.parseUUID(callId),
                  let endedReason = CallEndedReason(rawValue: reason) else {
                result(FlutterError(code: "invalid_argument", message: "session_id must be a UUID and reason must be valid.", details: nil))
                return
            }
            SwiftConnectycubeFlutterCallKitPlugin.callController.reportCallEnded(uuid: callUUID, reason: endedReason)
            result(true);
        }
        else if call.method == "reportCallEnded" {
            guard let arguments = arguments else {
                result(FlutterError(code: "invalid_argument", message: "No data was provided.", details: nil))
                return
            }
            let callId = arguments["session_id"] as! String
            guard let callUUID = CallKitController.parseUUID(callId) else {
                result(FlutterError(code: "invalid_argument", message: "session_id must be a valid UUID string.", details: nil))
                return
            }
            SwiftConnectycubeFlutterCallKitPlugin.callController.end(uuid: callUUID)
            result(true)
        }
        else if call.method == "muteCall" {
            guard let arguments = arguments else {
                result(FlutterError(code: "invalid_argument", message: "No data was provided.", details: nil))
                return
            }
            let callId = arguments["session_id"] as! String
            let muted = arguments["muted"] as! Bool
            guard let callUUID = CallKitController.parseUUID(callId) else {
                result(FlutterError(code: "invalid_argument", message: "session_id must be a valid UUID string.", details: nil))
                return
            }
            SwiftConnectycubeFlutterCallKitPlugin.callController.setMute(uuid: callUUID, muted: muted)
            result(true)
        }
        else if call.method == "getCallState" {
            guard let arguments = arguments, let callId = arguments["session_id"] as? String else {
                result(FlutterError(code: "invalid_argument", message: "session_id was not provided.", details: nil))
                return
            }
            
            result(SwiftConnectycubeFlutterCallKitPlugin.callController.getCallState(uuid: callId).rawValue)
        }
        else if call.method == "setCallState" {
            guard let arguments = arguments else {
                result(FlutterError(code: "invalid_argument", message: "No data was provided.", details: nil))
                return
            }
            let callId = arguments["session_id"] as! String
            let callState = arguments["call_state"] as! String
            
            SwiftConnectycubeFlutterCallKitPlugin.callController.setCallState(uuid: callId, callState: callState)
            result(true)
        }
        
        else if call.method == "getCallData" {
            guard let arguments = arguments, let callId = arguments["session_id"] as? String else {
                result(FlutterError(code: "invalid_argument", message: "session_id was not provided.", details: nil))
                return
            }
            
            result(SwiftConnectycubeFlutterCallKitPlugin.callController.getCallData(uuid: callId))
        }
        else if call.method == "clearCallData" {
            guard let arguments = arguments, let callId = arguments["session_id"] as? String else {
                result(FlutterError(code: "invalid_argument", message: "session_id was not provided.", details: nil))
                return
            }
            
            SwiftConnectycubeFlutterCallKitPlugin.callController.clearCallData(uuid: callId)
            result(true)
        }
        else if call.method == "getLastCallId" {
            result(SwiftConnectycubeFlutterCallKitPlugin.callController.currentCallData["session_id"])
        }
        else if call.method == "canUseFullScreenIntent" {
            result(true)
        }
        else if call.method == "provideFullScreenIntentAccess" {
            result(true)
        }
        else {
            result(FlutterMethodNotImplemented)
        }
    }
}
