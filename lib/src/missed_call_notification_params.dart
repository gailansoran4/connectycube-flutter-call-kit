import 'package:flutter/foundation.dart';

/// Config for missed-call notifications (mirrors flutter_callkit_incoming NotificationParams).
@immutable
class MissedCallNotificationParams {
  const MissedCallNotificationParams({
    this.id,
    this.showNotification = true,
    this.subtitle,
    this.callbackText,
    this.isShowCallback = true,
    this.count,
  });

  final int? id;
  final bool showNotification;
  final String? subtitle;
  final String? callbackText;
  final bool isShowCallback;
  final int? count;

  Map<String, Object?> toMap() {
    return {
      if (id != null) 'id': id,
      'show_notification': showNotification,
      if (subtitle != null) 'subtitle': subtitle,
      if (callbackText != null) 'callback_text': callbackText,
      'is_show_callback': isShowCallback,
      if (count != null) 'count': count,
    };
  }

  factory MissedCallNotificationParams.fromMap(Map<String, dynamic>? map) {
    if (map == null) {
      return const MissedCallNotificationParams();
    }
    return MissedCallNotificationParams(
      id: _readInt(map['id']),
      showNotification: map['show_notification'] as bool? ??
          map['showNotification'] as bool? ??
          true,
      subtitle: map['subtitle']?.toString(),
      callbackText:
          map['callback_text']?.toString() ?? map['callbackText']?.toString(),
      isShowCallback: map['is_show_callback'] as bool? ??
          map['isShowCallback'] as bool? ??
          true,
      count: _readInt(map['count']),
    );
  }

  static MissedCallNotificationParams? tryFromMap(Map<String, dynamic>? map) {
    if (map == null) return null;
    return MissedCallNotificationParams.fromMap(map);
  }

  static int? _readInt(dynamic value) {
    if (value == null) return null;
    if (value is int) return value;
    return int.tryParse(value.toString());
  }

  @override
  String toString() {
    return 'MissedCallNotificationParams('
        'id: $id, '
        'showNotification: $showNotification, '
        'subtitle: $subtitle, '
        'callbackText: $callbackText, '
        'isShowCallback: $isShowCallback, '
        'count: $count)';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is MissedCallNotificationParams &&
        other.id == id &&
        other.showNotification == showNotification &&
        other.subtitle == subtitle &&
        other.callbackText == callbackText &&
        other.isShowCallback == isShowCallback &&
        other.count == count;
  }

  @override
  int get hashCode =>
      (id?.hashCode ?? 0) ^
      showNotification.hashCode ^
      (subtitle?.hashCode ?? 0) ^
      (callbackText?.hashCode ?? 0) ^
      isShowCallback.hashCode ^
      (count?.hashCode ?? 0);
}
