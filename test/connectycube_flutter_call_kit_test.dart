import 'package:connectycube_flutter_call_kit/connectycube_flutter_call_kit.dart';
import 'package:flutter_test/flutter_test.dart';

CallEvent _sampleIncoming({
  int duration = 30000,
  bool showMissed = true,
  bool showCallback = true,
}) {
  return CallEvent(
    sessionId: 'session-full-1',
    callType: 1,
    callerId: 1001,
    callerName: 'Alice Demo',
    opponentsIds: {2002, 2003},
    callPhoto: 'https://example.com/avatar.png',
    userInfo: {'source': 'test'},
    acceptButtonLabel: 'Accept',
    rejectButtonLabel: 'Decline',
    duration: duration,
    missedCallNotification: MissedCallNotificationParams(
      id: 42,
      showNotification: showMissed,
      subtitle: 'Missed call',
      callbackText: 'Call back',
      isShowCallback: showCallback,
      count: 1,
    ),
    android: const AndroidCallKitParams(
      isShowLogo: true,
      logoUrl: 'call_logo',
      ringtonePath: 'ringtone_default',
      backgroundColor: '#0955FA',
      backgroundUrl: 'call_background',
      actionColor: '#0955FA',
      textColor: '#FFFFFF',
      textAccept: 'Accept',
      textDecline: 'Decline',
      incomingCallNotificationChannelName: 'Incoming Calls',
      missedCallNotificationChannelName: 'Missed Call',
      isShowFullLockedScreen: true,
      isFullScreen: true,
      durationMs: 30000,
    ),
  );
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('MissedCallNotificationParams', () {
    test('defaults match flutter_callkit_incoming NotificationParams', () {
      const params = MissedCallNotificationParams();
      expect(params.showNotification, isTrue);
      expect(params.isShowCallback, isTrue);
      expect(params.id, isNull);
      expect(params.subtitle, isNull);
      expect(params.callbackText, isNull);
      expect(params.count, isNull);
    });

    test('toMap includes required keys for native missed notification', () {
      const params = MissedCallNotificationParams(
        id: 7,
        showNotification: true,
        subtitle: 'Missed call',
        callbackText: 'Call back',
        isShowCallback: true,
        count: 3,
      );
      final map = params.toMap();
      expect(map['id'], 7);
      expect(map['show_notification'], isTrue);
      expect(map['subtitle'], 'Missed call');
      expect(map['callback_text'], 'Call back');
      expect(map['is_show_callback'], isTrue);
      expect(map['count'], 3);
    });

    test('fromMap accepts camelCase keys from reference-style payloads', () {
      final params = MissedCallNotificationParams.fromMap({
        'id': 5,
        'showNotification': false,
        'subtitle': 'Missed',
        'callbackText': 'Return',
        'isShowCallback': false,
        'count': '2',
      });
      expect(params.id, 5);
      expect(params.showNotification, isFalse);
      expect(params.subtitle, 'Missed');
      expect(params.callbackText, 'Return');
      expect(params.isShowCallback, isFalse);
      expect(params.count, 2);
    });

    test('tryFromMap returns null for null input', () {
      expect(MissedCallNotificationParams.tryFromMap(null), isNull);
    });

    test('equality / hashCode', () {
      const a = MissedCallNotificationParams(
        id: 1,
        subtitle: 'Missed call',
        callbackText: 'Call back',
      );
      const b = MissedCallNotificationParams(
        id: 1,
        subtitle: 'Missed call',
        callbackText: 'Call back',
      );
      const c = MissedCallNotificationParams(id: 2, subtitle: 'Missed call');
      expect(a, b);
      expect(a.hashCode, b.hashCode);
      expect(a == c, isFalse);
    });
  });

  group('AndroidCallKitParams', () {
    test('toMap emits all flutter_callkit_incoming-style Android keys', () {
      const params = AndroidCallKitParams(
        isShowLogo: true,
        logoUrl: 'call_logo',
        ringtonePath: 'ringtone_default',
        backgroundColor: '#0955FA',
        backgroundUrl: 'call_background',
        actionColor: '#4CAF50',
        textColor: '#FFFFFF',
        textAccept: 'Accept',
        textDecline: 'Decline',
        incomingCallNotificationChannelName: 'Incoming',
        missedCallNotificationChannelName: 'Missed',
        isShowFullLockedScreen: true,
        isFullScreen: false,
        durationMs: 45000,
      );
      final map = params.toMap();
      expect(map['is_show_logo'], isTrue);
      expect(map['logo_url'], 'call_logo');
      expect(map['ringtone_path'], 'ringtone_default');
      expect(map['background_color'], '#0955FA');
      expect(map['background_url'], 'call_background');
      expect(map['action_color'], '#4CAF50');
      expect(map['text_color'], '#FFFFFF');
      expect(map['text_accept'], 'Accept');
      expect(map['text_decline'], 'Decline');
      expect(map['incoming_call_notification_channel_name'], 'Incoming');
      expect(map['missed_call_notification_channel_name'], 'Missed');
      expect(map['is_show_full_locked_screen'], isTrue);
      expect(map['is_full_screen'], isFalse);
      expect(map['duration_ms'], 45000);
    });

    test('fromMap accepts camelCase AndroidParams keys', () {
      final params = AndroidCallKitParams.fromMap({
        'isShowLogo': true,
        'logoUrl': 'logo',
        'ringtonePath': 'tone',
        'backgroundColor': '#000',
        'backgroundUrl': 'bg',
        'actionColor': '#111',
        'textColor': '#fff',
        'textAccept': 'Yes',
        'textDecline': 'No',
        'incomingCallNotificationChannelName': 'In',
        'missedCallNotificationChannelName': 'Miss',
        'isShowFullLockedScreen': false,
        'isFullScreen': true,
        'durationMs': '15000',
      });
      expect(params.isShowLogo, isTrue);
      expect(params.logoUrl, 'logo');
      expect(params.ringtonePath, 'tone');
      expect(params.backgroundColor, '#000');
      expect(params.backgroundUrl, 'bg');
      expect(params.textAccept, 'Yes');
      expect(params.textDecline, 'No');
      expect(params.isShowFullLockedScreen, isFalse);
      expect(params.isFullScreen, isTrue);
      expect(params.durationMs, 15000);
    });

    test('round-trip preserves full Android customization', () {
      const original = AndroidCallKitParams(
        isShowLogo: true,
        logoUrl: 'call_logo',
        ringtonePath: 'ringtone_default',
        backgroundColor: '#0955FA',
        backgroundUrl: 'call_background',
        actionColor: '#0955FA',
        textColor: '#FFFFFF',
        textAccept: 'Accept',
        textDecline: 'Decline',
        incomingCallNotificationChannelName: 'Incoming Calls',
        missedCallNotificationChannelName: 'Missed Call',
        isShowFullLockedScreen: true,
        isFullScreen: true,
        durationMs: 30000,
      );
      final restored = AndroidCallKitParams.fromMap(
        original.toMap().cast<String, dynamic>(),
      );
      expect(restored, original);
    });
  });

  group('CallEvent new-feature serialization', () {
    test('full incoming payload round-trips through toMap/fromMap/json', () {
      final event = _sampleIncoming();
      final restored = CallEvent.fromMap(
        Map<String, dynamic>.from(event.toMap()),
      );
      expect(restored.sessionId, event.sessionId);
      expect(restored.duration, 30000);
      expect(restored.missedCallNotification?.showNotification, isTrue);
      expect(restored.missedCallNotification?.callbackText, 'Call back');
      expect(restored.android?.logoUrl, 'call_logo');
      expect(restored.android?.ringtonePath, 'ringtone_default');
      expect(restored.android?.backgroundUrl, 'call_background');
      expect(restored.android?.durationMs, 30000);

      final fromJson = CallEvent.fromJson(event.toJson());
      expect(fromJson.sessionId, event.sessionId);
      expect(fromJson.duration, event.duration);
      expect(fromJson.android?.isShowLogo, isTrue);
    });

    test('copyWith updates missed + android + duration', () {
      final event = _sampleIncoming().copyWith(
        duration: 10000,
        missedCallNotification: const MissedCallNotificationParams(
          showNotification: false,
        ),
        android: const AndroidCallKitParams(ringtonePath: 'other_tone'),
      );
      expect(event.duration, 10000);
      expect(event.missedCallNotification?.showNotification, isFalse);
      expect(event.android?.ringtonePath, 'other_tone');
    });

    test('accept/reject labels prefer explicit over android text*', () {
      const event = CallEvent(
        sessionId: 's',
        callType: 0,
        callerId: 1,
        callerName: 'N',
        opponentsIds: {},
        acceptButtonLabel: 'Join',
        rejectButtonLabel: 'Ignore',
        android: AndroidCallKitParams(
          textAccept: 'Accept',
          textDecline: 'Decline',
        ),
      );
      final map = event.toMap();
      expect(map['accept_button_label'], 'Join');
      expect(map['reject_button_label'], 'Ignore');
    });

    test('parses nested missedCallNotification camelCase alias', () {
      final event = CallEvent.fromMap({
        'session_id': 'x',
        'call_type': '1',
        'caller_id': '9',
        'caller_name': 'Bob',
        'call_opponents': '1,2',
        'user_info': '{}',
        'missedCallNotification': {
          'showNotification': true,
          'callbackText': 'Call back',
          'isShowCallback': true,
          'subtitle': 'Missed call',
        },
        'android': {
          'isShowLogo': true,
          'logoUrl': 'call_logo',
        },
      });
      expect(event.callType, 1);
      expect(event.callerId, 9);
      expect(event.missedCallNotification?.callbackText, 'Call back');
      expect(event.android?.logoUrl, 'call_logo');
    });

    test('omits nested maps when feature fields unset', () {
      const event = CallEvent(
        sessionId: 'plain',
        callType: 0,
        callerId: 1,
        callerName: 'Plain',
        opponentsIds: {},
      );
      final map = event.toMap();
      expect(map.containsKey('missed_call_notification'), isFalse);
      expect(map.containsKey('android'), isFalse);
      expect(map.containsKey('duration'), isFalse);
    });
  });

  group('Channel payloads for new APIs', () {
    test('showCallNotification map carries timeout + missed + android UI', () {
      final args = _sampleIncoming(duration: 10000).toMap();
      expect(args['duration'], 10000);
      final missed = args['missed_call_notification'] as Map;
      expect(missed['show_notification'], isTrue);
      expect(missed['callback_text'], 'Call back');
      expect(missed['is_show_callback'], isTrue);
      final android = args['android'] as Map;
      expect(android['is_show_logo'], isTrue);
      expect(android['logo_url'], 'call_logo');
      expect(android['ringtone_path'], 'ringtone_default');
      expect(android['background_color'], '#0955FA');
      expect(android['background_url'], 'call_background');
      expect(android['text_accept'], 'Accept');
      expect(android['text_decline'], 'Decline');
      expect(android['missed_call_notification_channel_name'], 'Missed Call');
    });

    test('showMissCallNotification can hide callback action', () {
      final args = _sampleIncoming(showCallback: false).toMap();
      final missed = args['missed_call_notification'] as Map;
      expect(missed['is_show_callback'], isFalse);
      expect(missed['show_notification'], isTrue);
    });

    test('showMissCallNotification can disable missed entirely', () {
      final args = _sampleIncoming(showMissed: false).toMap();
      final missed = args['missed_call_notification'] as Map;
      expect(missed['show_notification'], isFalse);
    });

    test('updateConfig args include logo/background/ringtone/missed defaults',
        () {
      final args = ConnectycubeFlutterCallKit.debugBuildUpdateConfigArgs(
        ringtone: 'ringtone_default',
        icon: 'call_logo',
        background: 'call_background',
        color: '#0955FA',
        logo: 'call_logo',
        backgroundColor: '#0955FA',
        actionColor: '#0955FA',
        textColor: '#FFFFFF',
        missedCallSubtitle: 'Missed call',
        missedCallCallbackText: 'Call back',
        showMissedCallNotification: true,
        showMissedCallCallback: true,
        missedCallNotificationChannelName: 'Missed Call',
        defaultDurationMs: 30000,
      );
      expect(args['ringtone'], 'ringtone_default');
      expect(args['logo'], 'call_logo');
      expect(args['background'], 'call_background');
      expect(args['background_color'], '#0955FA');
      expect(args['action_color'], '#0955FA');
      expect(args['text_color'], '#FFFFFF');
      expect(args['missed_call_subtitle'], 'Missed call');
      expect(args['missed_call_callback_text'], 'Call back');
      expect(args['show_missed_call_notification'], isTrue);
      expect(args['show_missed_call_callback'], isTrue);
      expect(args['missed_call_notification_channel_name'], 'Missed Call');
      expect(args['default_duration_ms'], 30000);
    });
  });

  group('Foreground event routing (new features)', () {
    tearDown(() {
      ConnectycubeFlutterCallKit.debugSetForegroundHandlers();
      ConnectycubeFlutterCallKit.onTokenRefreshed = null;
      ConnectycubeFlutterCallKit.onCallMuted = null;
    });

    Map<String, dynamic> _callArgs([String sessionId = 'sess-1']) => {
          'session_id': sessionId,
          'call_type': 1,
          'caller_id': 10,
          'caller_name': 'Alice',
          'call_opponents': '20',
          'photo_url': null,
          'user_info': '{}',
        };

    test('timeoutCall invokes onCallTimeout', () async {
      CallEvent? received;
      ConnectycubeFlutterCallKit.debugSetForegroundHandlers(
        onCallTimeout: (e) async => received = e,
      );

      ConnectycubeFlutterCallKit.debugProcessEvent({
        'event': 'timeoutCall',
        'args': _callArgs('timeout-1'),
      });

      expect(received, isNotNull);
      expect(received!.sessionId, 'timeout-1');
      expect(received!.callerName, 'Alice');
    });

    test('missedCallCallback invokes onMissedCallCallback', () async {
      CallEvent? received;
      ConnectycubeFlutterCallKit.debugSetForegroundHandlers(
        onMissedCallCallback: (e) async => received = e,
      );

      ConnectycubeFlutterCallKit.debugProcessEvent({
        'event': 'missedCallCallback',
        'args': _callArgs('missed-cb-1'),
      });

      expect(received, isNotNull);
      expect(received!.sessionId, 'missed-cb-1');
    });

    test('incoming / accept / reject still route correctly', () async {
      final events = <String>[];
      ConnectycubeFlutterCallKit.debugSetForegroundHandlers(
        onCallIncoming: (e) async => events.add('incoming:${e.sessionId}'),
        onCallAccepted: (e) async => events.add('accepted:${e.sessionId}'),
        onCallRejected: (e) async => events.add('rejected:${e.sessionId}'),
      );

      ConnectycubeFlutterCallKit.debugProcessEvent({
        'event': 'incomingCall',
        'args': _callArgs('a'),
      });
      ConnectycubeFlutterCallKit.debugProcessEvent({
        'event': 'answerCall',
        'args': _callArgs('b'),
      });
      ConnectycubeFlutterCallKit.debugProcessEvent({
        'event': 'endCall',
        'args': _callArgs('c'),
      });

      expect(events, [
        'incoming:a',
        'accepted:b',
        'rejected:c',
      ]);
    });

    test('timeout then missed callback sequence (FG flow)', () async {
      final log = <String>[];
      ConnectycubeFlutterCallKit.debugSetForegroundHandlers(
        onCallTimeout: (e) async => log.add('timeout:${e.sessionId}'),
        onMissedCallCallback: (e) async => log.add('callback:${e.sessionId}'),
      );

      ConnectycubeFlutterCallKit.debugProcessEvent({
        'event': 'timeoutCall',
        'args': _callArgs('flow-1'),
      });
      ConnectycubeFlutterCallKit.debugProcessEvent({
        'event': 'missedCallCallback',
        'args': _callArgs('flow-1'),
      });

      expect(log, ['timeout:flow-1', 'callback:flow-1']);
    });

    test('unrecognized event is ignored (does not throw)', () {
      expect(
        () => ConnectycubeFlutterCallKit.debugProcessEvent({
          'event': 'notARealEvent',
          'args': <String, dynamic>{},
        }),
        returnsNormally,
      );
    });
  });

  group('Flutter asset path payloads', () {
    test('Flutter asset paths pass through for ringtone/icon/logo/background', () {
      const event = CallEvent(
        sessionId: 'asset-1',
        callType: 1,
        callerId: 1,
        callerName: 'Asset',
        opponentsIds: {},
        android: AndroidCallKitParams(
          isShowLogo: true,
          logoUrl: 'assets/image/call_icon.png',
          ringtonePath: 'assets/ringtone/call_ring',
          backgroundUrl: 'assets/image/call_background.png',
        ),
        ios: IOSCallKitParams(
          iconName: 'assets/image/call_icon.png',
          handleType: 'generic',
          supportsVideo: true,
          ringtonePath: 'Ringtone.caf',
        ),
      );
      final android = event.toMap()['android'] as Map;
      expect(android['logo_url'], 'assets/image/call_icon.png');
      expect(android['ringtone_path'], 'assets/ringtone/call_ring');
      expect(android['background_url'], 'assets/image/call_background.png');

      final ios = event.toMap()['ios'] as Map;
      expect(ios['icon_name'], 'assets/image/call_icon.png');
      expect(ios['handle_type'], 'generic');
      expect(ios['supports_video'], isTrue);
      expect(ios['ringtone_path'], 'Ringtone.caf');

      final config = ConnectycubeFlutterCallKit.debugBuildUpdateConfigArgs(
        ringtone: 'assets/ringtone/call_ring',
        icon: 'assets/image/call_icon.png',
        background: 'assets/image/call_background.png',
        logo: 'assets/image/call_icon.png',
      );
      expect(config['ringtone'], 'assets/ringtone/call_ring');
      expect(config['icon'], 'assets/image/call_icon.png');
      expect(config['background'], 'assets/image/call_background.png');
      expect(config['logo'], 'assets/image/call_icon.png');
    });

    test('IOSCallKitParams round-trip', () {
      const params = IOSCallKitParams(
        iconName: 'CallKitLogo',
        handleType: 'number',
        supportsVideo: true,
        maximumCallGroups: 1,
        ringtonePath: 'Ringtone.caf',
        includesCallsInRecents: false,
      );
      final restored =
          IOSCallKitParams.fromMap(params.toMap().cast<String, dynamic>());
      expect(restored, params);
    });
  });

  group('Background callback names (terminated / BG)', () {
    test('exposes all Android isolate callback names including missed', () {
      expect(BackgroundCallbackName.REJECTED_IN_BACKGROUND,
          'rejected_in_background');
      expect(BackgroundCallbackName.ACCEPTED_IN_BACKGROUND,
          'accepted_in_background');
      expect(BackgroundCallbackName.INCOMING_IN_BACKGROUND,
          'incoming_in_background');
      expect(BackgroundCallbackName.MISSED_CALLBACK_IN_BACKGROUND,
          'missed_callback_in_background');
    });
  });

  group('CallState', () {
    test('known states used across timeout/missed native path', () {
      expect(CallState.PENDING, 'pending');
      expect(CallState.ACCEPTED, 'accepted');
      expect(CallState.REJECTED, 'rejected');
      expect(CallState.UNKNOWN, 'unknown');
    });
  });
}
