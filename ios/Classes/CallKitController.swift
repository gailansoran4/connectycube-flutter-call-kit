//
//  CallKitController.swift
//  connectycube_flutter_call_kit
//
//  Created by Tereha on 19.11.2021.
//

import Foundation
import AVFoundation
import CallKit
import Flutter
import UIKit

enum CallEvent : String {
    case incomingCall = "incomingCall"
    case answerCall = "answerCall"
    case endCall = "endCall"
    case setHeld = "setHeld"
    case reset = "reset"
    case startCall = "startCall"
    case setMuted = "setMuted"
    case setUnMuted = "setUnMuted"
    case timeoutCall = "timeoutCall"
    case missedCallCallback = "missedCallCallback"
}

enum CallEndedReason : String {
    case failed = "failed"
    case unanswered = "unanswered"
    case remoteEnded = "remoteEnded"
}

enum CallState : String {
    case pending = "pending"
    case accepted = "accepted"
    case rejected = "rejected"
    case unknown = "unknown"
}

class CallKitController : NSObject {
    private let provider : CXProvider
    private let callController : CXCallController
    var actionListener : ((CallEvent, UUID, [String:Any]?)->Void)?
    var currentCallData: [String: Any] = [:]
    private var callStates: [String:CallState] = [:]
    private var callsData: [String:[String:Any]] = [:]
    private static var sharedDefaultDurationMs: Int = 60000
    private static var sharedMissedShow: Bool = true
    private static var sharedMissedSubtitle: String = "Missed call"
    private static var sharedMissedCallbackText: String = "Call back"
    private static var sharedMissedShowCallback: Bool = true

    private var defaultDurationMs: Int {
        get { CallKitController.sharedDefaultDurationMs }
        set { CallKitController.sharedDefaultDurationMs = newValue }
    }
    private var defaultMissedShow: Bool {
        get { CallKitController.sharedMissedShow }
        set { CallKitController.sharedMissedShow = newValue }
    }
    private var defaultMissedSubtitle: String {
        get { CallKitController.sharedMissedSubtitle }
        set { CallKitController.sharedMissedSubtitle = newValue }
    }
    private var defaultMissedCallbackText: String {
        get { CallKitController.sharedMissedCallbackText }
        set { CallKitController.sharedMissedCallbackText = newValue }
    }
    private var defaultMissedShowCallback: Bool {
        get { CallKitController.sharedMissedShowCallback }
        set { CallKitController.sharedMissedShowCallback = newValue }
    }
    private var timeoutTimers: [String: Timer] = [:]
    private var pendingTimeoutHandled: Set<String> = []
    private static weak var sharedInstance: CallKitController?
    
    override init() {
        self.provider = CXProvider(configuration: CallKitController.providerConfiguration)
        self.callController = CXCallController()
        
        super.init()
        CallKitController.sharedInstance = self
        self.provider.setDelegate(self, queue: nil)

        MissedCallNotificationManager.shared.callbackListener = { [weak self] args in
            guard let self = self else { return }
            let uuidString = (args["session_id"] as? String) ?? UUID().uuidString
            self.actionListener?(
                .missedCallCallback,
                UUID(uuidString: uuidString) ?? UUID(),
                args
            )
        }
    }
    
    //TODO: construct configuration from flutter. pass into init over method channel
    static var providerConfiguration: CXProviderConfiguration = {
        let appName = Bundle.main.infoDictionary?[kCFBundleNameKey as String] as! String
        var providerConfiguration: CXProviderConfiguration
        if #available(iOS 14.0, *) {
            providerConfiguration = CXProviderConfiguration.init()
        } else {
            providerConfiguration = CXProviderConfiguration(localizedName: appName)
        }
        
        providerConfiguration.supportsVideo = true
        providerConfiguration.maximumCallsPerCallGroup = 1
        providerConfiguration.maximumCallGroups = 1;
        providerConfiguration.supportedHandleTypes = [.generic]
        
        if #available(iOS 11.0, *) {
            providerConfiguration.includesCallsInRecents = false
        }
        
        return providerConfiguration
    }()
    
    static func updateConfig(
        ringtone: String?,
        icon: String?,
        defaultDurationMs: Int? = nil,
        missedSubtitle: String? = nil,
        missedCallbackText: String? = nil,
        showMissedCallNotification: Bool? = nil,
        showMissedCallCallback: Bool? = nil
    ) {
        if let ringtone = ringtone, !ringtone.isEmpty {
            if let resolved = resolveRingtone(ringtone) {
                providerConfiguration.ringtoneSound = resolved
            }
        }
        
        if let icon = icon, !icon.isEmpty {
            if let image = loadCallKitIcon(named: icon) {
                providerConfiguration.iconTemplateImageData = image.pngData()
            }
        }

        if let duration = defaultDurationMs {
            sharedDefaultDurationMs = duration
        }
        if let subtitle = missedSubtitle {
            sharedMissedSubtitle = subtitle
        }
        if let callbackText = missedCallbackText {
            sharedMissedCallbackText = callbackText
        }
        if let show = showMissedCallNotification {
            sharedMissedShow = show
        }
        if let showCallback = showMissedCallCallback {
            sharedMissedShowCallback = showCallback
        }

        MissedCallNotificationManager.shared.setup(
            callbackTitle: sharedMissedCallbackText
        )

        refreshProviderConfiguration()
    }

    /// Pushes the (possibly mutated) static configuration to the live CXProvider,
    /// otherwise ringtone/icon changes made after init would never take effect.
    static func refreshProviderConfiguration() {
        sharedInstance?.provider.configuration = providerConfiguration
    }

    /// Resolves a ringtone value to something CallKit can play.
    /// - Plain names (e.g. "Ringtone.caf") are used as-is (Xcode bundle lookup).
    /// - Flutter asset paths (e.g. "assets/ringtone/call_ring.mp3", extension optional)
    ///   are resolved inside the app bundle via the Flutter asset registry and passed
    ///   as a bundle-relative path. The file is also copied to Library/Sounds so the
    ///   missed-call notification can reuse the same sound.
    private static func resolveRingtone(_ ringtone: String) -> String? {
        if !ringtone.contains("/") {
            return ringtone
        }

        let candidates: [String]
        if (ringtone as NSString).pathExtension.isEmpty {
            candidates = ["\(ringtone).caf", "\(ringtone).wav", "\(ringtone).aiff",
                          "\(ringtone).mp3", "\(ringtone).m4a", ringtone]
        } else {
            candidates = [ringtone]
        }

        let bundlePath = Bundle.main.bundlePath
        for candidate in candidates {
            let lookupKey = FlutterDartProject.lookupKey(forAsset: candidate)
            guard let path = Bundle.main.path(forResource: lookupKey, ofType: nil) else {
                continue
            }

            let fileName = (path as NSString).lastPathComponent
            if let soundName = copyToLibrarySounds(from: path, name: fileName) {
                MissedCallNotificationManager.missedSoundName = soundName
            }

            if path.hasPrefix(bundlePath) {
                // CallKit resolves ringtoneSound relative to the main bundle.
                return String(path.dropFirst(bundlePath.count + 1))
            }
            return fileName
        }

        print("[CallKitController] ringtone asset not found in bundle: \(ringtone)")
        return nil
    }

    /// Copies a sound file to Library/Sounds so UNNotificationSound(named:) can find it.
    /// Returns the sound name usable with UNNotificationSound, or nil when the format
    /// is not supported for notifications (e.g. mp3).
    @discardableResult
    private static func copyToLibrarySounds(from sourcePath: String, name: String) -> String? {
        let ext = (name as NSString).pathExtension.lowercased()
        guard ["caf", "wav", "aiff", "aif"].contains(ext) else { return nil }

        guard let libraryDir = FileManager.default.urls(
            for: .libraryDirectory, in: .userDomainMask
        ).first else { return nil }

        let soundsDir = libraryDir.appendingPathComponent("Sounds", isDirectory: true)
        let destination = soundsDir.appendingPathComponent(name)
        do {
            try FileManager.default.createDirectory(
                at: soundsDir, withIntermediateDirectories: true
            )
            if FileManager.default.fileExists(atPath: destination.path) {
                try FileManager.default.removeItem(at: destination)
            }
            try FileManager.default.copyItem(
                at: URL(fileURLWithPath: sourcePath), to: destination
            )
            return name
        } catch {
            print("[CallKitController] failed to copy sound to Library/Sounds: \(error)")
            return nil
        }
    }

    /// Loads CallKit template icon from Assets.xcassets name or Flutter asset path.
    private static func loadCallKitIcon(named icon: String) -> UIImage? {
        if let fromCatalog = UIImage(named: icon) {
            return fromCatalog
        }
        // Flutter asset: assets/image/call_icon.png
        let candidates: [String]
        if icon.contains(".") {
            candidates = [icon]
        } else {
            candidates = ["\(icon).png", "\(icon).jpg", "\(icon).jpeg", "\(icon).webp", icon]
        }
        for candidate in candidates {
            let lookupKey = FlutterDartProject.lookupKey(forAsset: candidate)
            if let path = Bundle.main.path(forResource: lookupKey, ofType: nil),
               let image = UIImage(contentsOfFile: path) {
                return image
            }
            // Fallback for some embedder layouts
            if let url = Bundle.main.url(
                forResource: (candidate as NSString).deletingPathExtension,
                withExtension: (candidate as NSString).pathExtension.isEmpty
                    ? "png"
                    : (candidate as NSString).pathExtension,
                subdirectory: "Frameworks/App.framework/flutter_assets"
            ), let image = UIImage(contentsOfFile: url.path) {
                return image
            }
        }
        print("[CallKitController][updateConfig] icon not found: \(icon)")
        return nil
    }
    
    static func applyIOSParams(_ params: [String: Any]?) {
        guard let params = params else { return }

        if let ringtone = params["ringtone_path"] as? String, !ringtone.isEmpty {
            if let resolved = resolveRingtone(ringtone) {
                providerConfiguration.ringtoneSound = resolved
            }
        }
        if let icon = params["icon_name"] as? String, !icon.isEmpty {
            if let image = loadCallKitIcon(named: icon) {
                providerConfiguration.iconTemplateImageData = image.pngData()
            }
        }
        if let supportsVideo = params["supports_video"] as? Bool {
            providerConfiguration.supportsVideo = supportsVideo
        }
        if let maxGroups = params["maximum_call_groups"] as? Int {
            providerConfiguration.maximumCallGroups = maxGroups
        }
        if let maxPerGroup = params["maximum_calls_per_call_group"] as? Int {
            providerConfiguration.maximumCallsPerCallGroup = maxPerGroup
        }
        if let includes = params["includes_calls_in_recents"] as? Bool {
            if #available(iOS 11.0, *) {
                providerConfiguration.includesCallsInRecents = includes
            }
        }
        if let handleType = params["handle_type"] as? String {
            switch handleType.lowercased() {
            case "number":
                providerConfiguration.supportedHandleTypes = [.phoneNumber]
            case "email":
                providerConfiguration.supportedHandleTypes = [.emailAddress]
            default:
                providerConfiguration.supportedHandleTypes = [.generic]
            }
        }

        refreshProviderConfiguration()
    }

    // Not @objc: optional Int/Bool parameters cannot be represented in Objective-C.
    func reportIncomingCall(
        uuid: String,
        callType: Int,
        callInitiatorId: Int,
        callInitiatorName: String,
        opponents: [Int],
        userInfo: String?,
        durationMs: Int? = nil,
        missedShow: Bool? = nil,
        missedSubtitle: String? = nil,
        missedCallbackText: String? = nil,
        missedShowCallback: Bool? = nil,
        iosParams: [String: Any]? = nil,
        completion: ((Error?) -> Void)?
    ) {
        print("[CallKitController][reportIncomingCall] call data: \(uuid), \(callType), \(callInitiatorId), \(callInitiatorName), \(opponents), \(userInfo ?? "nil")")

        CallKitController.applyIOSParams(iosParams)
        
        let update = CXCallUpdate()
        update.localizedCallerName = callInitiatorName

        let handleType: CXHandle.HandleType
        switch (iosParams?["handle_type"] as? String)?.lowercased() {
        case "number":
            handleType = .phoneNumber
        case "email":
            handleType = .emailAddress
        default:
            handleType = .generic
        }
        update.remoteHandle = CXHandle(type: handleType, value: uuid)

        let supportsVideo = iosParams?["supports_video"] as? Bool
        update.hasVideo = supportsVideo ?? (callType == 1)
        update.supportsGrouping = iosParams?["supports_grouping"] as? Bool ?? false
        update.supportsUngrouping = iosParams?["supports_ungrouping"] as? Bool ?? false
        update.supportsHolding = iosParams?["supports_holding"] as? Bool ?? false
        update.supportsDTMF = iosParams?["supports_dtmf"] as? Bool ?? false
        
        guard let callUUID = Self.parseUUID(uuid) else {
            print("[CallKitController][reportIncomingCall] invalid session_id (need UUID): \(uuid)")
            completion?(NSError(
                domain: "ConnectycubeFlutterCallKit",
                code: 1,
                userInfo: [NSLocalizedDescriptionKey: "session_id must be a valid UUID string"]
            ))
            return
        }

        if (self.currentCallData["session_id"] == nil || self.currentCallData["session_id"] as! String != uuid) {
            print("[CallKitController][reportIncomingCall] report new call: \(uuid)")
            
            provider.reportNewIncomingCall(with: callUUID, update: update) { error in
                completion?(error)
                
                if(error == nil){
                    self.configureAudioSession(active: true)
                    
                    self.currentCallData["session_id"] = uuid
                    self.currentCallData["call_type"] = callType
                    self.currentCallData["caller_id"] = callInitiatorId
                    self.currentCallData["caller_name"] = callInitiatorName
                    self.currentCallData["call_opponents"] = opponents.map { String($0) }.joined(separator: ",")
                    self.currentCallData["user_info"] = userInfo
                    self.currentCallData["missed_show"] = missedShow ?? self.defaultMissedShow
                    self.currentCallData["missed_subtitle"] = missedSubtitle ?? self.defaultMissedSubtitle
                    self.currentCallData["missed_callback_text"] = missedCallbackText ?? self.defaultMissedCallbackText
                    self.currentCallData["missed_show_callback"] = missedShowCallback ?? self.defaultMissedShowCallback
                    
                    self.callStates[uuid] = .pending
                    self.callsData[uuid] = self.currentCallData
                    self.pendingTimeoutHandled.remove(uuid)

                    self.actionListener?(.incomingCall, callUUID, self.currentCallData)
                    self.scheduleTimeout(uuid: uuid, durationMs: durationMs ?? self.defaultDurationMs)
                }
            }
        } else if (self.currentCallData["session_id"] as! String == uuid) {
            print("[CallKitController][reportIncomingCall] update existing call: \(uuid)")
            
            provider.reportCall(with: callUUID, updated: update)
            
            completion?(nil)
        }
    }

    /// CallKit requires a real UUID; returns nil for timestamps / other non-UUID ids.
    static func parseUUID(_ value: String) -> UUID? {
        return UUID(uuidString: value)
    }

    private func scheduleTimeout(uuid: String, durationMs: Int) {
        cancelTimeout(uuid: uuid)
        let interval = TimeInterval(max(durationMs, 1000)) / 1000.0
        let timer = Timer.scheduledTimer(withTimeInterval: interval, repeats: false) { [weak self] _ in
            self?.handleRingTimeout(uuid: uuid)
        }
        timeoutTimers[uuid] = timer
    }

    private func cancelTimeout(uuid: String) {
        timeoutTimers[uuid]?.invalidate()
        timeoutTimers.removeValue(forKey: uuid)
    }

    private func handleRingTimeout(uuid: String) {
        guard callStates[uuid] == .pending, !pendingTimeoutHandled.contains(uuid) else { return }
        pendingTimeoutHandled.insert(uuid)
        cancelTimeout(uuid: uuid)

        let data = callsData[uuid] ?? currentCallData
        let callUUID = Self.parseUUID(uuid) ?? UUID()
        actionListener?(.timeoutCall, callUUID, data)
        showMissedFromCallData(data)
        reportCallEnded(uuid: callUUID, reason: .unanswered)
    }

    func showMissedFromCallData(_ data: [String: Any]) {
        let sessionId = data["session_id"] as? String ?? ""
        let show = data["missed_show"] as? Bool ?? defaultMissedShow
        MissedCallNotificationManager.shared.showMissedCall(
            sessionId: sessionId,
            callerName: data["caller_name"] as? String ?? "",
            callType: data["call_type"] as? Int ?? 0,
            callerId: data["caller_id"] as? Int ?? 0,
            opponents: data["call_opponents"] as? String ?? "",
            photoUrl: data["photo_url"] as? String,
            userInfo: data["user_info"] as? String,
            showNotification: show,
            subtitle: data["missed_subtitle"] as? String ?? defaultMissedSubtitle,
            callbackText: data["missed_callback_text"] as? String ?? defaultMissedCallbackText,
            isShowCallback: data["missed_show_callback"] as? Bool ?? defaultMissedShowCallback,
            count: data["missed_count"] as? Int ?? 1
        )
    }
    
    func reportOutgoingCall(uuid : UUID, finishedConnecting: Bool){
        print("[CallKitController][reportOutgoingCall] uuid: \(uuid.uuidString.lowercased()) connected: \(finishedConnecting)")
        
        if !finishedConnecting {
            self.provider.reportOutgoingCall(with: uuid, startedConnectingAt: nil)
        } else {
            self.provider.reportOutgoingCall(with: uuid, connectedAt: nil)
        }
    }
    
    func reportCallEnded(uuid : UUID, reason: CallEndedReason){
        print("[CallKitController][reportCallEnded] uuid: \(uuid.uuidString.lowercased())")
        
        var cxReason : CXCallEndedReason
        switch reason {
        case .unanswered:
            cxReason = CXCallEndedReason.unanswered
        case .remoteEnded:
            cxReason = CXCallEndedReason.remoteEnded
        default:
            cxReason = CXCallEndedReason.failed
        }
        
        self.callStates[uuid.uuidString.lowercased()] = .rejected
        self.provider.reportCall(with: uuid, endedAt: Date.init(), reason: cxReason)
    }
    
    func getCallState(uuid: String) -> CallState {
        print("[CallKitController][getCallState] uuid: \(uuid), state: \(self.callStates[uuid.lowercased()] ?? .unknown)")
        
        return self.callStates[uuid.lowercased()] ?? .unknown
    }
    
    func setCallState(uuid: String, callState: String){
        self.callStates[uuid.lowercased()] = CallState(rawValue: callState)
    }
    
    func getCallData(uuid: String) -> [String: Any]{
        return self.callsData[uuid.lowercased()] ?? [:]
    }
    
    func clearCallData(uuid: String){
        self.callStates.removeAll()
        self.callsData.removeAll()
    }
    
    func sendAudioInterruptionNotification(){
        print("[CallKitController][sendAudioInterruptionNotification]")
        var userInfo : [AnyHashable : Any] = [:]
        let intrepEndeRaw = AVAudioSession.InterruptionType.ended.rawValue
        userInfo[AVAudioSessionInterruptionTypeKey] = intrepEndeRaw
        userInfo[AVAudioSessionInterruptionOptionKey] = AVAudioSession.InterruptionOptions.shouldResume.rawValue
        
        NotificationCenter.default.post(name: AVAudioSession.interruptionNotification, object: self, userInfo: userInfo)
    }
    
    func configureAudioSession(active: Bool){
        print("[CallKitController][configureAudioSession] active: \(active)")
        
        let audioSession = AVAudioSession.sharedInstance()
        
        do {
            try audioSession.setCategory(
                AVAudioSession.Category.playAndRecord,
                options: [
                    .allowBluetooth,
                    .allowBluetoothA2DP,
                ])
            try audioSession.setMode(AVAudioSession.Mode.videoChat)
            try audioSession.setPreferredSampleRate(44100.0)
            try audioSession.setPreferredIOBufferDuration(0.005)
            try audioSession.setActive(active)
        } catch {
            print(error)
        }
    }
}

//MARK: user actions
extension CallKitController {
    
    func end(uuid: UUID) {
        print("[CallKitController][end] uuid: \(uuid.uuidString.lowercased())")
        
        let endCallAction = CXEndCallAction(call: uuid)
        let transaction = CXTransaction(action: endCallAction)
        
        self.callStates[uuid.uuidString.lowercased()] = .rejected
        
        requestTransaction(transaction)
    }
    
    private func requestTransaction(_ transaction: CXTransaction) {
        callController.request(transaction) { error in
            if let error = error {
                print("[CallKitController][requestTransaction] Error: \(error.localizedDescription)")
            } else {
                print("[CallKitController][requestTransaction] successfully")
            }
        }
    }
    
    func setHeld(uuid: UUID, onHold: Bool) {
        print("[CallKitController][setHeld] uuid: \(uuid.uuidString.lowercased()), onHold: \(onHold)")
        
        let setHeldCallAction = CXSetHeldCallAction(call: uuid, onHold: onHold)
        
        let transaction = CXTransaction()
        transaction.addAction(setHeldCallAction)
        
        requestTransaction(transaction)
    }
    
    func setMute(uuid: UUID, muted: Bool){
        print("[CallKitController][setMute] uuid: \(uuid.uuidString.lowercased()), muted: \(muted)")
        
        let muteCallAction = CXSetMutedCallAction(call: uuid, muted: muted);
        let transaction = CXTransaction()
        transaction.addAction(muteCallAction)
        
        requestTransaction(transaction)
    }
    
    func startCall(handle: String, videoEnabled: Bool, uuid: String? = nil) {
        print("[CallKitController][startCall] handle:\(handle), videoEnabled: \(videoEnabled) uuid: \(uuid ?? "nil")")
        
        let handle = CXHandle(type: .generic, value: handle)
        let callUUID = uuid.flatMap { Self.parseUUID($0) } ?? UUID()
        let startCallAction = CXStartCallAction(call: callUUID, handle: handle)
        startCallAction.isVideo = videoEnabled
        
        let transaction = CXTransaction(action: startCallAction)
        
        let stateKey = (uuid ?? callUUID.uuidString).lowercased()
        self.callStates[stateKey] = .accepted
        
        requestTransaction(transaction);
    }
    
    func answerCall(uuid: String) {
        print("[CallKitController][answerCall] uuid: \(uuid)")
        
        guard let callUUID = Self.parseUUID(uuid) else {
            print("[CallKitController][answerCall] invalid uuid: \(uuid)")
            return
        }
        let answerCallAction = CXAnswerCallAction(call: callUUID)
        let transaction = CXTransaction(action: answerCallAction)
        
        self.callStates[uuid.lowercased()] = .accepted
        
        requestTransaction(transaction);
    }
}

//MARK: System notifications
extension CallKitController: CXProviderDelegate {
    func providerDidReset(_ provider: CXProvider) {
        
    }
    
    func provider(_ provider: CXProvider, perform action: CXAnswerCallAction) {
        let uuid = action.callUUID.uuidString.lowercased()
        print("[CallKitController][CXAnswerCallAction] callUUID: \(uuid)")

        if callStates[uuid] == .accepted {
            print("[CallKitController][CXAnswerCallAction] skip duplicate answerCall")
            action.fulfill()
            return
        }

        cancelTimeout(uuid: uuid)
        configureAudioSession(active: true)
        callStates[uuid] = .accepted
        actionListener?(.answerCall, action.callUUID, self.currentCallData)

        action.fulfill()
    }
    
    func provider(_ provider: CXProvider, didActivate audioSession: AVAudioSession) {
        print("[CallKitController] Audio session activated")
        
        sendAudioInterruptionNotification()
        configureAudioSession(active: true)
    }
    
    func provider(_ provider: CXProvider, didDeactivate audioSession: AVAudioSession) {
        print("[CallKitController] Audio session deactivated")
    }
    
    func provider(_ provider: CXProvider, perform action: CXEndCallAction) {
        let uuid = action.callUUID.uuidString.lowercased()
        print("[CallKitController][CXEndCallAction] callUUID: \(uuid)")

        if callStates[uuid] == .rejected {
            print("[CallKitController][CXEndCallAction] skip duplicate endCall")
            action.fulfill()
            return
        }

        cancelTimeout(uuid: uuid)
        // If still pending and not already handled as timeout, this is a decline.
        if callStates[uuid] == .pending && !pendingTimeoutHandled.contains(uuid) {
            actionListener?(.endCall, action.callUUID, currentCallData)
        }
        callStates[uuid] = .rejected

        action.fulfill()
    }
    
    func provider(_ provider: CXProvider, perform action: CXSetHeldCallAction) {
        print("[CallKitController][CXSetHeldCallAction] callUUID: \(action.callUUID.uuidString.lowercased())")
        
        actionListener?(.setHeld, action.callUUID, ["isOnHold": action.isOnHold])
        
        action.fulfill()
    }
    
    func provider(_ provider: CXProvider, perform action: CXSetMutedCallAction) {
        print("[CallKitController][CXSetMutedCallAction] callUUID: \(action.callUUID.uuidString.lowercased())")
        
        if (action.isMuted){
            actionListener?(.setMuted, action.callUUID, currentCallData)
        } else {
            actionListener?(.setUnMuted, action.callUUID, currentCallData)
        }
        
        action.fulfill()
    }
    
    func provider(_ provider: CXProvider, perform action: CXStartCallAction) {
        print("[CallKitController][CXStartCallAction]: callUUID: \(action.callUUID.uuidString.lowercased())")
        
        actionListener?(.startCall, action.callUUID, currentCallData)
        callStates[action.callUUID.uuidString.lowercased()] = .accepted
        configureAudioSession(active: true)
        
        action.fulfill()
    }
}
