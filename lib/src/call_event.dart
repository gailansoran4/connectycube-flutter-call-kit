import 'dart:convert';

import 'package:flutter/foundation.dart';

import 'android_call_kit_params.dart';
import 'ios_call_kit_params.dart';
import 'missed_call_notification_params.dart';

/// {@template call_event}
/// Information about the call events (e.g. CallAccepted / CallRejected)
/// {@endtemplate}
@immutable
class CallEvent {
  /// {@macro call_event}
  const CallEvent({
    required this.sessionId,
    required this.callType,
    required this.callerId,
    required this.callerName,
    required this.opponentsIds,
    this.callPhoto,
    this.userInfo,
    this.acceptButtonLabel,
    this.rejectButtonLabel,
    this.acceptButtonBackgroundColor,
    this.acceptButtonTextColor,
    this.rejectButtonBackgroundColor,
    this.rejectButtonTextColor,
    this.duration,
    this.missedCallNotification,
    this.android,
    this.ios,
  });

  final String sessionId;
  final int callType;
  final int callerId;
  final String callerName;
  final Set<int> opponentsIds;
  final String? callPhoto;

  /// Custom label for the Accept/Reject buttons.
  ///
  /// Android: applied to the full-screen incoming UI and to the heads-up
  /// notification actions.
  /// iOS: ignored — the CallKit system UI does not allow custom button text.
  final String? acceptButtonLabel;
  final String? rejectButtonLabel;

  /// Background color (`#RRGGBB` or `#AARRGGBB`) of the Accept/Reject buttons.
  ///
  /// Android: tints the round Accept/Reject buttons on the full-screen
  /// incoming UI and the button backgrounds on the heads-up notification.
  /// iOS: ignored — the CallKit system UI does not allow custom button colors.
  final String? acceptButtonBackgroundColor;
  final String? rejectButtonBackgroundColor;

  /// Foreground/text color (`#RRGGBB` or `#AARRGGBB`) of the Accept/Reject
  /// buttons.
  ///
  /// Android: applied to the button labels on the full-screen incoming UI and
  /// to the button text on the heads-up notification.
  /// iOS: ignored — the CallKit system UI does not allow custom button colors.
  final String? acceptButtonTextColor;
  final String? rejectButtonTextColor;

  /// Ring timeout in milliseconds. When elapsed without accept/reject,
  /// the incoming UI is dismissed and a missed-call notification may be shown.
  /// Defaults to 60000 on the native side when unset.
  final int? duration;

  /// Missed-call notification options (shown after timeout or via
  /// [ConnectycubeFlutterCallKit.showMissCallNotification]).
  final MissedCallNotificationParams? missedCallNotification;

  /// Per-call Android customization (logo, background, ringtone, colors…).
  final AndroidCallKitParams? android;

  /// Per-call iOS CallKit options (icon, ringtone name, handle type…).
  /// Does **not** include custom full-screen UI (system CallKit only).
  final IOSCallKitParams? ios;

  /// Used for exchanging additional data between the Call notification and your app,
  /// you will get this data in event callbacks (e.g. onCallAcceptedWhenTerminated,
  /// onCallAccepted, onCallRejectedWhenTerminated, or onCallRejected)
  /// after setting it in method showCallNotification
  final Map<String, String>? userInfo;

  CallEvent copyWith({
    String? sessionId,
    int? callType,
    int? callerId,
    String? callerName,
    Set<int>? opponentsIds,
    String? callPhoto,
    Map<String, String>? userInfo,
    String? acceptButtonLabel,
    String? rejectButtonLabel,
    String? acceptButtonBackgroundColor,
    String? acceptButtonTextColor,
    String? rejectButtonBackgroundColor,
    String? rejectButtonTextColor,
    int? duration,
    MissedCallNotificationParams? missedCallNotification,
    AndroidCallKitParams? android,
    IOSCallKitParams? ios,
  }) {
    return CallEvent(
      sessionId: sessionId ?? this.sessionId,
      callType: callType ?? this.callType,
      callerId: callerId ?? this.callerId,
      callerName: callerName ?? this.callerName,
      opponentsIds: opponentsIds ?? this.opponentsIds,
      callPhoto: callPhoto ?? this.callPhoto,
      userInfo: userInfo ?? this.userInfo,
      acceptButtonLabel: acceptButtonLabel ?? this.acceptButtonLabel,
      rejectButtonLabel: rejectButtonLabel ?? this.rejectButtonLabel,
      acceptButtonBackgroundColor:
          acceptButtonBackgroundColor ?? this.acceptButtonBackgroundColor,
      acceptButtonTextColor:
          acceptButtonTextColor ?? this.acceptButtonTextColor,
      rejectButtonBackgroundColor:
          rejectButtonBackgroundColor ?? this.rejectButtonBackgroundColor,
      rejectButtonTextColor:
          rejectButtonTextColor ?? this.rejectButtonTextColor,
      duration: duration ?? this.duration,
      missedCallNotification:
          missedCallNotification ?? this.missedCallNotification,
      android: android ?? this.android,
      ios: ios ?? this.ios,
    );
  }

  Map<String, Object?> toMap() {
    final acceptLabel =
        acceptButtonLabel ?? android?.textAccept;
    final rejectLabel =
        rejectButtonLabel ?? android?.textDecline;
    final acceptBgColor =
        acceptButtonBackgroundColor ?? android?.acceptButtonBackgroundColor;
    final acceptTxtColor =
        acceptButtonTextColor ?? android?.acceptButtonTextColor;
    final rejectBgColor =
        rejectButtonBackgroundColor ?? android?.declineButtonBackgroundColor;
    final rejectTxtColor =
        rejectButtonTextColor ?? android?.declineButtonTextColor;
    return {
      'session_id': sessionId,
      'call_type': callType,
      'caller_id': callerId,
      'caller_name': callerName,
      'call_opponents': opponentsIds.join(','),
      'photo_url': callPhoto,
      'user_info': jsonEncode(userInfo ?? <String, String>{}),
      if (acceptLabel != null && acceptLabel.isNotEmpty)
        'accept_button_label': acceptLabel,
      if (rejectLabel != null && rejectLabel.isNotEmpty)
        'reject_button_label': rejectLabel,
      if (acceptBgColor != null && acceptBgColor.isNotEmpty)
        'accept_button_background_color': acceptBgColor,
      if (acceptTxtColor != null && acceptTxtColor.isNotEmpty)
        'accept_button_text_color': acceptTxtColor,
      if (rejectBgColor != null && rejectBgColor.isNotEmpty)
        'reject_button_background_color': rejectBgColor,
      if (rejectTxtColor != null && rejectTxtColor.isNotEmpty)
        'reject_button_text_color': rejectTxtColor,
      if (duration != null) 'duration': duration,
      if (missedCallNotification != null)
        'missed_call_notification': missedCallNotification!.toMap(),
      if (android != null) 'android': android!.toMap(),
      if (ios != null) 'ios': ios!.toMap(),
    };
  }

  static int readInt(dynamic value, {int fallback = 0}) {
    if (value is int) return value;
    return int.tryParse(value?.toString() ?? '') ?? fallback;
  }

  static int? readNullableInt(dynamic value) {
    if (value == null) return null;
    if (value is int) return value;
    return int.tryParse(value.toString());
  }

  static Set<int> readOpponents(dynamic value) {
    if (value == null) return {};
    final text = value.toString().trim();
    if (text.isEmpty) return {};
    return text
        .split(',')
        .map((part) => int.tryParse(part.trim()))
        .whereType<int>()
        .toSet();
  }

  static Map<String, String>? readUserInfo(dynamic value) {
    if (value == null) return null;
    if (value is Map) {
      return value.map((key, item) => MapEntry(key.toString(), item.toString()));
    }
    final text = value.toString().trim();
    if (text.isEmpty) return null;
    try {
      final decoded = jsonDecode(text);
      if (decoded is Map) {
        return decoded.map((key, item) => MapEntry(key.toString(), item.toString()));
      }
    } catch (_) {}
    return null;
  }

  static Map<String, dynamic>? _asStringKeyedMap(dynamic value) {
    if (value == null) return null;
    if (value is Map) {
      return value.map((key, item) => MapEntry(key.toString(), item));
    }
    if (value is String && value.trim().isNotEmpty) {
      try {
        final decoded = jsonDecode(value);
        if (decoded is Map) {
          return decoded.map((key, item) => MapEntry(key.toString(), item));
        }
      } catch (_) {}
    }
    return null;
  }

  factory CallEvent.fromMap(Map<String, dynamic> map) {
    print('[CallEvent.fromMap] map: $map');
    return CallEvent(
      sessionId: map['session_id']?.toString() ?? '',
      callType: readInt(map['call_type']),
      callerId: readInt(map['caller_id']),
      callerName: map['caller_name']?.toString() ?? '',
      opponentsIds: readOpponents(map['call_opponents']),
      callPhoto: map['photo_url']?.toString(),
      acceptButtonLabel: map['accept_button_label']?.toString(),
      rejectButtonLabel: map['reject_button_label']?.toString(),
      acceptButtonBackgroundColor:
          map['accept_button_background_color']?.toString(),
      acceptButtonTextColor: map['accept_button_text_color']?.toString(),
      rejectButtonBackgroundColor:
          map['reject_button_background_color']?.toString(),
      rejectButtonTextColor: map['reject_button_text_color']?.toString(),
      userInfo: readUserInfo(map['user_info']),
      duration: readNullableInt(map['duration']),
      missedCallNotification: MissedCallNotificationParams.tryFromMap(
        _asStringKeyedMap(
          map['missed_call_notification'] ?? map['missedCallNotification'],
        ),
      ),
      android: _asStringKeyedMap(map['android']) == null
          ? null
          : AndroidCallKitParams.fromMap(_asStringKeyedMap(map['android'])),
      ios: _asStringKeyedMap(map['ios']) == null
          ? null
          : IOSCallKitParams.fromMap(_asStringKeyedMap(map['ios'])),
    );
  }

  String toJson() => json.encode(toMap());

  factory CallEvent.fromJson(String source) =>
      CallEvent.fromMap(json.decode(source) as Map<String, dynamic>);

  @override
  String toString() {
    return 'CallEvent('
        'sessionId: $sessionId, '
        'callType: $callType, '
        'callerId: $callerId, '
        'callerName: $callerName, '
        'opponentsIds: $opponentsIds, '
        'callPhoto: $callPhoto, '
        'acceptButtonLabel: $acceptButtonLabel, '
        'rejectButtonLabel: $rejectButtonLabel, '
        'acceptButtonBackgroundColor: $acceptButtonBackgroundColor, '
        'acceptButtonTextColor: $acceptButtonTextColor, '
        'rejectButtonBackgroundColor: $rejectButtonBackgroundColor, '
        'rejectButtonTextColor: $rejectButtonTextColor, '
        'duration: $duration, '
        'missedCallNotification: $missedCallNotification, '
        'android: $android, '
        'ios: $ios, '
        'userInfo: $userInfo)';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;

    return other is CallEvent &&
        other.sessionId == sessionId &&
        other.callType == callType &&
        other.callerId == callerId &&
        other.callerName == callerName &&
        setEquals(other.opponentsIds, opponentsIds) &&
        other.callPhoto == callPhoto &&
        other.acceptButtonLabel == acceptButtonLabel &&
        other.rejectButtonLabel == rejectButtonLabel &&
        other.acceptButtonBackgroundColor == acceptButtonBackgroundColor &&
        other.acceptButtonTextColor == acceptButtonTextColor &&
        other.rejectButtonBackgroundColor == rejectButtonBackgroundColor &&
        other.rejectButtonTextColor == rejectButtonTextColor &&
        other.duration == duration &&
        other.missedCallNotification == missedCallNotification &&
        other.android == android &&
        other.ios == ios &&
        mapEquals(other.userInfo, userInfo);
  }

  @override
  int get hashCode {
    return sessionId.hashCode ^
        callType.hashCode ^
        callerId.hashCode ^
        callerName.hashCode ^
        opponentsIds.hashCode ^
        (acceptButtonLabel?.hashCode ?? 0) ^
        (rejectButtonLabel?.hashCode ?? 0) ^
        (acceptButtonBackgroundColor?.hashCode ?? 0) ^
        (acceptButtonTextColor?.hashCode ?? 0) ^
        (rejectButtonBackgroundColor?.hashCode ?? 0) ^
        (rejectButtonTextColor?.hashCode ?? 0) ^
        (duration?.hashCode ?? 0) ^
        (missedCallNotification?.hashCode ?? 0) ^
        (android?.hashCode ?? 0) ^
        (ios?.hashCode ?? 0) ^
        userInfo.hashCode;
  }
}
