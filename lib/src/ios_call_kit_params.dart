import 'package:flutter/foundation.dart';

/// iOS CallKit options (mirrors flutter_callkit_incoming IOSParams).
///
/// Unlike [AndroidCallKitParams], there is **no custom full-screen UI** on iOS —
/// the system CallKit screen is used (both the full-screen incoming call UI
/// and the banner shown while the device is unlocked). Logo/background/colors
/// from Android do not apply here, and Apple does not allow changing the
/// Accept/Decline button names or colors of the CallKit UI
/// ([CallEvent.acceptButtonLabel], [CallEvent.acceptButtonBackgroundColor],
/// [CallEvent.acceptButtonTextColor], etc. are ignored on iOS).
@immutable
class IOSCallKitParams {
  const IOSCallKitParams({
    this.iconName,
    this.handleType,
    this.supportsVideo,
    this.maximumCallGroups,
    this.maximumCallsPerCallGroup,
    this.supportsDTMF,
    this.supportsHolding,
    this.supportsGrouping,
    this.supportsUngrouping,
    this.includesCallsInRecents,
    this.ringtonePath,
  });

  /// CallKit template icon: Assets.xcassets name **or** Flutter asset path
  /// (e.g. `CallKitLogo` / `assets/image/call_icon.png`).
  final String? iconName;

  /// `generic`, `number`, or `email`.
  final String? handleType;

  final bool? supportsVideo;
  final int? maximumCallGroups;
  final int? maximumCallsPerCallGroup;
  final bool? supportsDTMF;
  final bool? supportsHolding;
  final bool? supportsGrouping;
  final bool? supportsUngrouping;
  final bool? includesCallsInRecents;

  /// Bundled CallKit sound name (e.g. `Ringtone.caf`) **or** a Flutter asset
  /// path (e.g. `assets/ringtone/call_ring`, extension optional). Flutter assets
  /// are resolved inside the app bundle; if the sound is caf/wav/aiff it's also
  /// reused for the missed-call notification sound.
  final String? ringtonePath;

  Map<String, Object?> toMap() {
    return {
      if (iconName != null) 'icon_name': iconName,
      if (handleType != null) 'handle_type': handleType,
      if (supportsVideo != null) 'supports_video': supportsVideo,
      if (maximumCallGroups != null) 'maximum_call_groups': maximumCallGroups,
      if (maximumCallsPerCallGroup != null)
        'maximum_calls_per_call_group': maximumCallsPerCallGroup,
      if (supportsDTMF != null) 'supports_dtmf': supportsDTMF,
      if (supportsHolding != null) 'supports_holding': supportsHolding,
      if (supportsGrouping != null) 'supports_grouping': supportsGrouping,
      if (supportsUngrouping != null) 'supports_ungrouping': supportsUngrouping,
      if (includesCallsInRecents != null)
        'includes_calls_in_recents': includesCallsInRecents,
      if (ringtonePath != null) 'ringtone_path': ringtonePath,
    };
  }

  factory IOSCallKitParams.fromMap(Map<String, dynamic>? map) {
    if (map == null) return const IOSCallKitParams();
    return IOSCallKitParams(
      iconName: map['icon_name']?.toString() ?? map['iconName']?.toString(),
      handleType:
          map['handle_type']?.toString() ?? map['handleType']?.toString(),
      supportsVideo:
          map['supports_video'] as bool? ?? map['supportsVideo'] as bool?,
      maximumCallGroups: _readInt(
          map['maximum_call_groups'] ?? map['maximumCallGroups']),
      maximumCallsPerCallGroup: _readInt(map['maximum_calls_per_call_group'] ??
          map['maximumCallsPerCallGroup']),
      supportsDTMF:
          map['supports_dtmf'] as bool? ?? map['supportsDTMF'] as bool?,
      supportsHolding:
          map['supports_holding'] as bool? ?? map['supportsHolding'] as bool?,
      supportsGrouping: map['supports_grouping'] as bool? ??
          map['supportsGrouping'] as bool?,
      supportsUngrouping: map['supports_ungrouping'] as bool? ??
          map['supportsUngrouping'] as bool?,
      includesCallsInRecents: map['includes_calls_in_recents'] as bool? ??
          map['includesCallsInRecents'] as bool?,
      ringtonePath:
          map['ringtone_path']?.toString() ?? map['ringtonePath']?.toString(),
    );
  }

  static int? _readInt(dynamic value) {
    if (value == null) return null;
    if (value is int) return value;
    return int.tryParse(value.toString());
  }

  @override
  String toString() {
    return 'IOSCallKitParams('
        'iconName: $iconName, '
        'handleType: $handleType, '
        'supportsVideo: $supportsVideo, '
        'ringtonePath: $ringtonePath)';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is IOSCallKitParams &&
        other.iconName == iconName &&
        other.handleType == handleType &&
        other.supportsVideo == supportsVideo &&
        other.maximumCallGroups == maximumCallGroups &&
        other.maximumCallsPerCallGroup == maximumCallsPerCallGroup &&
        other.supportsDTMF == supportsDTMF &&
        other.supportsHolding == supportsHolding &&
        other.supportsGrouping == supportsGrouping &&
        other.supportsUngrouping == supportsUngrouping &&
        other.includesCallsInRecents == includesCallsInRecents &&
        other.ringtonePath == ringtonePath;
  }

  @override
  int get hashCode =>
      (iconName?.hashCode ?? 0) ^
      (handleType?.hashCode ?? 0) ^
      (supportsVideo?.hashCode ?? 0) ^
      (maximumCallGroups?.hashCode ?? 0) ^
      (maximumCallsPerCallGroup?.hashCode ?? 0) ^
      (supportsDTMF?.hashCode ?? 0) ^
      (supportsHolding?.hashCode ?? 0) ^
      (supportsGrouping?.hashCode ?? 0) ^
      (supportsUngrouping?.hashCode ?? 0) ^
      (includesCallsInRecents?.hashCode ?? 0) ^
      (ringtonePath?.hashCode ?? 0);
}
