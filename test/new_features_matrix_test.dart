import 'package:connectycube_flutter_call_kit/connectycube_flutter_call_kit.dart';
import 'package:flutter_test/flutter_test.dart';

/// Documents / guards the feature matrix from the CallKit parity plan:
/// incoming customization, timeout → missed, body-open vs callback.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('Feature matrix: incoming customization', () {
    test('logo + background + ringtone + colors are present in channel map',
        () {
      const event = CallEvent(
        sessionId: 'ui-1',
        callType: 1,
        callerId: 1,
        callerName: 'Caller',
        opponentsIds: {2},
        duration: 30000,
        android: AndroidCallKitParams(
          isShowLogo: true,
          logoUrl: 'call_logo',
          ringtonePath: 'ringtone_default',
          backgroundColor: '#0955FA',
          backgroundUrl: 'call_background',
          actionColor: '#0955FA',
          textColor: '#FFFFFF',
          textAccept: 'Accept',
          textDecline: 'Decline',
        ),
      );

      final android = event.toMap()['android'] as Map;
      expect(android['is_show_logo'], isTrue);
      expect(android['logo_url'], isNotEmpty);
      expect(android['ringtone_path'], isNotEmpty);
      expect(android['background_color'], startsWith('#'));
      expect(android['background_url'], isNotEmpty);
      expect(android['text_accept'], 'Accept');
      expect(android['text_decline'], 'Decline');
    });
  });

  group('Feature matrix: timeout → missed', () {
    test('short duration is encoded for native timeout', () {
      const event = CallEvent(
        sessionId: 't-1',
        callType: 0,
        callerId: 1,
        callerName: 'Caller',
        opponentsIds: {},
        duration: 10000,
        missedCallNotification: MissedCallNotificationParams(
          showNotification: true,
          subtitle: 'Missed call',
        ),
        android: AndroidCallKitParams(durationMs: 10000),
      );
      expect(event.toMap()['duration'], 10000);
      expect(
        (event.toMap()['android'] as Map)['duration_ms'],
        10000,
      );
      expect(
        (event.toMap()['missed_call_notification'] as Map)['show_notification'],
        isTrue,
      );
    });

    test('timeout event delivers CallEvent to app handler', () async {
      CallEvent? timedOut;
      ConnectycubeFlutterCallKit.debugSetForegroundHandlers(
        onCallTimeout: (e) async => timedOut = e,
      );
      ConnectycubeFlutterCallKit.debugProcessEvent({
        'event': 'timeoutCall',
        'args': {
          'session_id': 't-2',
          'call_type': 0,
          'caller_id': 5,
          'caller_name': 'Missed Alice',
          'call_opponents': '',
          'user_info': '{}',
        },
      });
      expect(timedOut?.sessionId, 't-2');
      expect(timedOut?.callerName, 'Missed Alice');
      ConnectycubeFlutterCallKit.debugSetForegroundHandlers();
    });
  });

  group('Feature matrix: missed notification click paths', () {
    test('body-open path keeps showNotification and optional callback flag',
        () {
      // Body tap opens app only (native). Callback action is optional.
      const withCallback = MissedCallNotificationParams(
        showNotification: true,
        isShowCallback: true,
        callbackText: 'Call back',
        subtitle: 'Missed call',
      );
      const bodyOnly = MissedCallNotificationParams(
        showNotification: true,
        isShowCallback: false,
        subtitle: 'Missed call',
      );

      expect(withCallback.toMap()['is_show_callback'], isTrue);
      expect(withCallback.toMap()['callback_text'], 'Call back');
      expect(bodyOnly.toMap()['is_show_callback'], isFalse);
      expect(bodyOnly.toMap().containsKey('callback_text'), isFalse);
    });

    test('Call back action fires missedCallCallback event', () async {
      CallEvent? callbackEvent;
      ConnectycubeFlutterCallKit.debugSetForegroundHandlers(
        onMissedCallCallback: (e) async => callbackEvent = e,
      );
      ConnectycubeFlutterCallKit.debugProcessEvent({
        'event': 'missedCallCallback',
        'args': {
          'session_id': 'cb-1',
          'call_type': 1,
          'caller_id': 9,
          'caller_name': 'Bob',
          'call_opponents': '1',
          'user_info': '{"source":"missed"}',
        },
      });
      expect(callbackEvent?.sessionId, 'cb-1');
      expect(callbackEvent?.userInfo?['source'], 'missed');
      ConnectycubeFlutterCallKit.debugSetForegroundHandlers();
    });
  });

  group('Feature matrix: FG accept / reject still work with new fields', () {
    test('accept and reject handlers receive CallEvent', () async {
      CallEvent? accepted;
      CallEvent? rejected;
      ConnectycubeFlutterCallKit.debugSetForegroundHandlers(
        onCallAccepted: (e) async => accepted = e,
        onCallRejected: (e) async => rejected = e,
      );

      final args = {
        'session_id': 'live-1',
        'call_type': 1,
        'caller_id': 1,
        'caller_name': 'Live',
        'call_opponents': '2',
        'user_info': '{}',
        'duration': 30000,
      };

      ConnectycubeFlutterCallKit.debugProcessEvent({
        'event': 'answerCall',
        'args': args,
      });
      ConnectycubeFlutterCallKit.debugProcessEvent({
        'event': 'endCall',
        'args': {...args, 'session_id': 'live-2'},
      });

      expect(accepted?.sessionId, 'live-1');
      expect(rejected?.sessionId, 'live-2');
      ConnectycubeFlutterCallKit.debugSetForegroundHandlers();
    });
  });
}
