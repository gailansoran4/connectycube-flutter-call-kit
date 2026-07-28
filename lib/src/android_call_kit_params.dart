import 'package:flutter/foundation.dart';

/// Android-specific incoming-call UI / notification options
/// (mirrors flutter_callkit_incoming AndroidParams).
@immutable
class AndroidCallKitParams {
  const AndroidCallKitParams({
    this.isShowLogo,
    this.logoUrl,
    this.ringtonePath,
    this.backgroundColor,
    this.backgroundUrl,
    this.actionColor,
    this.textColor,
    this.textAccept,
    this.textDecline,
    this.incomingCallNotificationChannelName,
    this.missedCallNotificationChannelName,
    this.isShowFullLockedScreen,
    this.isFullScreen,
    this.durationMs,
  });

  /// Show app logo on the full-screen incoming UI.
  final bool? isShowLogo;

  /// Drawable name, Flutter asset path (`assets/image/logo.png`), or http(s) URL for the logo.
  final String? logoUrl;

  /// Ringtone: Android `res/raw` name, **or** Flutter asset path
  /// (`assets/ringtone/call_ring` / `assets/ringtone/call_ring.mp3`).
  /// For iOS use [IOSCallKitParams.ringtonePath] (bundled name or Flutter asset).
  final String? ringtonePath;

  /// Full-screen background color as `#RRGGBB` or `#AARRGGBB`.
  final String? backgroundColor;

  /// Drawable name, Flutter asset path (`assets/image/call_background.png`), or http(s) URL.
  final String? backgroundUrl;

  /// Accent / action color for notifications and buttons.
  final String? actionColor;

  /// Text color on the full-screen incoming UI.
  final String? textColor;

  /// Accept button label on full-screen UI.
  final String? textAccept;

  /// Decline button label on full-screen UI.
  final String? textDecline;

  final String? incomingCallNotificationChannelName;
  final String? missedCallNotificationChannelName;

  /// Show incoming UI over the lock screen (default true).
  final bool? isShowFullLockedScreen;

  /// Prefer full-screen activity path when possible.
  final bool? isFullScreen;

  /// Ring timeout in milliseconds before treating the call as missed.
  final int? durationMs;

  Map<String, Object?> toMap() {
    return {
      if (isShowLogo != null) 'is_show_logo': isShowLogo,
      if (logoUrl != null) 'logo_url': logoUrl,
      if (ringtonePath != null) 'ringtone_path': ringtonePath,
      if (backgroundColor != null) 'background_color': backgroundColor,
      if (backgroundUrl != null) 'background_url': backgroundUrl,
      if (actionColor != null) 'action_color': actionColor,
      if (textColor != null) 'text_color': textColor,
      if (textAccept != null) 'text_accept': textAccept,
      if (textDecline != null) 'text_decline': textDecline,
      if (incomingCallNotificationChannelName != null)
        'incoming_call_notification_channel_name':
            incomingCallNotificationChannelName,
      if (missedCallNotificationChannelName != null)
        'missed_call_notification_channel_name':
            missedCallNotificationChannelName,
      if (isShowFullLockedScreen != null)
        'is_show_full_locked_screen': isShowFullLockedScreen,
      if (isFullScreen != null) 'is_full_screen': isFullScreen,
      if (durationMs != null) 'duration_ms': durationMs,
    };
  }

  factory AndroidCallKitParams.fromMap(Map<String, dynamic>? map) {
    if (map == null) return const AndroidCallKitParams();
    return AndroidCallKitParams(
      isShowLogo:
          map['is_show_logo'] as bool? ?? map['isShowLogo'] as bool?,
      logoUrl: map['logo_url']?.toString() ?? map['logoUrl']?.toString(),
      ringtonePath:
          map['ringtone_path']?.toString() ?? map['ringtonePath']?.toString(),
      backgroundColor: map['background_color']?.toString() ??
          map['backgroundColor']?.toString(),
      backgroundUrl: map['background_url']?.toString() ??
          map['backgroundUrl']?.toString(),
      actionColor:
          map['action_color']?.toString() ?? map['actionColor']?.toString(),
      textColor: map['text_color']?.toString() ?? map['textColor']?.toString(),
      textAccept:
          map['text_accept']?.toString() ?? map['textAccept']?.toString(),
      textDecline:
          map['text_decline']?.toString() ?? map['textDecline']?.toString(),
      incomingCallNotificationChannelName:
          map['incoming_call_notification_channel_name']?.toString() ??
              map['incomingCallNotificationChannelName']?.toString(),
      missedCallNotificationChannelName:
          map['missed_call_notification_channel_name']?.toString() ??
              map['missedCallNotificationChannelName']?.toString(),
      isShowFullLockedScreen: map['is_show_full_locked_screen'] as bool? ??
          map['isShowFullLockedScreen'] as bool?,
      isFullScreen:
          map['is_full_screen'] as bool? ?? map['isFullScreen'] as bool?,
      durationMs: _readInt(map['duration_ms'] ?? map['durationMs']),
    );
  }

  static int? _readInt(dynamic value) {
    if (value == null) return null;
    if (value is int) return value;
    return int.tryParse(value.toString());
  }

  @override
  String toString() {
    return 'AndroidCallKitParams('
        'isShowLogo: $isShowLogo, '
        'logoUrl: $logoUrl, '
        'ringtonePath: $ringtonePath, '
        'backgroundColor: $backgroundColor, '
        'backgroundUrl: $backgroundUrl, '
        'actionColor: $actionColor, '
        'textColor: $textColor, '
        'textAccept: $textAccept, '
        'textDecline: $textDecline, '
        'durationMs: $durationMs)';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is AndroidCallKitParams &&
        other.isShowLogo == isShowLogo &&
        other.logoUrl == logoUrl &&
        other.ringtonePath == ringtonePath &&
        other.backgroundColor == backgroundColor &&
        other.backgroundUrl == backgroundUrl &&
        other.actionColor == actionColor &&
        other.textColor == textColor &&
        other.textAccept == textAccept &&
        other.textDecline == textDecline &&
        other.incomingCallNotificationChannelName ==
            incomingCallNotificationChannelName &&
        other.missedCallNotificationChannelName ==
            missedCallNotificationChannelName &&
        other.isShowFullLockedScreen == isShowFullLockedScreen &&
        other.isFullScreen == isFullScreen &&
        other.durationMs == durationMs;
  }

  @override
  int get hashCode =>
      (isShowLogo?.hashCode ?? 0) ^
      (logoUrl?.hashCode ?? 0) ^
      (ringtonePath?.hashCode ?? 0) ^
      (backgroundColor?.hashCode ?? 0) ^
      (backgroundUrl?.hashCode ?? 0) ^
      (actionColor?.hashCode ?? 0) ^
      (textColor?.hashCode ?? 0) ^
      (textAccept?.hashCode ?? 0) ^
      (textDecline?.hashCode ?? 0) ^
      (incomingCallNotificationChannelName?.hashCode ?? 0) ^
      (missedCallNotificationChannelName?.hashCode ?? 0) ^
      (isShowFullLockedScreen?.hashCode ?? 0) ^
      (isFullScreen?.hashCode ?? 0) ^
      (durationMs?.hashCode ?? 0);
}
