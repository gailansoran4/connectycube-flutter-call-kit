import Foundation
import UserNotifications
import UIKit

/// Local missed-call notifications (parity with flutter_callkit_incoming).
class MissedCallNotificationManager: NSObject, UNUserNotificationCenterDelegate {
    static let shared = MissedCallNotificationManager()

    static let missedCallCategory = "CQ_MISSED_CALL_CATEGORY"
    static let callbackAction = "CQ_CALLBACK_ACTION"

    var callbackListener: (([String: Any]) -> Void)?

    private var previousDelegate: UNUserNotificationCenterDelegate?

    private override init() {
        super.init()
    }

    func setup(callbackTitle: String = "Call back") {
        let center = UNUserNotificationCenter.current()
        if previousDelegate == nil && center.delegate !== self {
            previousDelegate = center.delegate
            center.delegate = self
        }

        let callback = UNNotificationAction(
            identifier: MissedCallNotificationManager.callbackAction,
            title: callbackTitle,
            options: [.foreground]
        )
        let category = UNNotificationCategory(
            identifier: MissedCallNotificationManager.missedCallCategory,
            actions: [callback],
            intentIdentifiers: [],
            options: []
        )
        center.getNotificationCategories { categories in
            var updated = categories
            updated.insert(category)
            center.setNotificationCategories(updated)
        }

        center.requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
    }

    func showMissedCall(
        sessionId: String,
        callerName: String,
        callType: Int,
        callerId: Int,
        opponents: String,
        photoUrl: String?,
        userInfo: String?,
        showNotification: Bool,
        subtitle: String,
        callbackText: String,
        isShowCallback: Bool,
        count: Int
    ) {
        guard showNotification else { return }

        setup(callbackTitle: callbackText)

        let content = UNMutableNotificationContent()
        content.title = callerName
        content.subtitle = subtitle
        content.body = callType == 1 ? "Missed video call" : "Missed audio call"
        content.sound = .default
        content.categoryIdentifier = isShowCallback
            ? MissedCallNotificationManager.missedCallCategory
            : ""
        if count > 1 {
            content.badge = NSNumber(value: count)
        }
        content.userInfo = [
            "session_id": sessionId,
            "caller_name": callerName,
            "call_type": callType,
            "caller_id": callerId,
            "call_opponents": opponents,
            "photo_url": photoUrl ?? "",
            "user_info": userInfo ?? "{}",
            "cq_missed_call": true
        ]

        let request = UNNotificationRequest(
            identifier: "missing_\(sessionId)",
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(request, withCompletionHandler: nil)
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        if previousDelegate?.userNotificationCenter?(
            center,
            willPresent: notification,
            withCompletionHandler: completionHandler
        ) != nil {
            return
        }
        if #available(iOS 14.0, *) {
            completionHandler([.banner, .sound, .badge])
        } else {
            completionHandler([.alert, .sound, .badge])
        }
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        let isMissed = (userInfo["cq_missed_call"] as? Bool) == true

        if isMissed && response.actionIdentifier == MissedCallNotificationManager.callbackAction {
            var args: [String: Any] = [:]
            args["session_id"] = userInfo["session_id"] as? String ?? ""
            args["caller_name"] = userInfo["caller_name"] as? String ?? ""
            args["call_type"] = userInfo["call_type"] as? Int ?? 0
            args["caller_id"] = userInfo["caller_id"] as? Int ?? 0
            args["call_opponents"] = userInfo["call_opponents"] as? String ?? ""
            args["photo_url"] = userInfo["photo_url"] as? String ?? ""
            args["user_info"] = userInfo["user_info"] as? String ?? "{}"
            callbackListener?(args)
        }

        if let previous = previousDelegate {
            previous.userNotificationCenter?(
                center,
                didReceive: response,
                withCompletionHandler: completionHandler
            )
        } else {
            completionHandler()
        }
    }
}
