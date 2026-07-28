import 'package:connectycube_flutter_call_kit/connectycube_flutter_call_kit.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('CallEvent serialization', () {
    test('round-trips core and customization fields', () {
      const event = CallEvent(
        sessionId: 'abc-123',
        callType: 1,
        callerId: 42,
        callerName: 'Alice',
        opponentsIds: {7, 8},
        callPhoto: 'https://example.com/a.png',
        userInfo: {'k': 'v'},
        acceptButtonLabel: 'Accept',
        rejectButtonLabel: 'Decline',
        duration: 30000,
        missedCallNotification: MissedCallNotificationParams(
          id: 9,
          showNotification: true,
          subtitle: 'Missed call',
          callbackText: 'Call back',
          isShowCallback: true,
          count: 2,
        ),
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
          durationMs: 30000,
          missedCallNotificationChannelName: 'Missed Call',
        ),
      );

      final map = Map<String, dynamic>.from(event.toMap());
      final restored = CallEvent.fromMap(map);

      expect(restored.sessionId, 'abc-123');
      expect(restored.callType, 1);
      expect(restored.callerId, 42);
      expect(restored.callerName, 'Alice');
      expect(restored.opponentsIds, {7, 8});
      expect(restored.callPhoto, 'https://example.com/a.png');
      expect(restored.userInfo, {'k': 'v'});
      expect(restored.duration, 30000);
      expect(restored.missedCallNotification?.subtitle, 'Missed call');
      expect(restored.missedCallNotification?.callbackText, 'Call back');
      expect(restored.missedCallNotification?.isShowCallback, isTrue);
      expect(restored.android?.isShowLogo, isTrue);
      expect(restored.android?.logoUrl, 'call_logo');
      expect(restored.android?.ringtonePath, 'ringtone_default');
      expect(restored.android?.backgroundColor, '#0955FA');
      expect(restored.android?.durationMs, 30000);

      expect(map['missed_call_notification'], isA<Map>());
      expect(map['android'], isA<Map>());
      expect(map['duration'], 30000);
      expect(map['session_id'], 'abc-123');
    });

    test('accept/reject labels fall back to android textAccept/textDecline', () {
      const event = CallEvent(
        sessionId: 's1',
        callType: 0,
        callerId: 1,
        callerName: 'Bob',
        opponentsIds: {},
        android: AndroidCallKitParams(
          textAccept: 'Yes',
          textDecline: 'No',
        ),
      );
      final map = event.toMap();
      expect(map['accept_button_label'], 'Yes');
      expect(map['reject_button_label'], 'No');
    });

    test('omits nested maps when unset', () {
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

  group('MissedCallNotificationParams', () {
    test('toMap/fromMap', () {
      const params = MissedCallNotificationParams(
        id: 3,
        showNotification: false,
        subtitle: 'Missed',
        callbackText: 'Return',
        isShowCallback: false,
        count: 4,
      );
      final restored = MissedCallNotificationParams.fromMap(
        params.toMap().cast<String, dynamic>(),
      );
      expect(restored, params);
    });
  });

  group('AndroidCallKitParams', () {
    test('toMap/fromMap', () {
      const params = AndroidCallKitParams(
        isShowLogo: true,
        logoUrl: 'logo',
        ringtonePath: 'tone',
        backgroundColor: '#000000',
        durationMs: 15000,
      );
      final restored = AndroidCallKitParams.fromMap(
        params.toMap().cast<String, dynamic>(),
      );
      expect(restored.isShowLogo, isTrue);
      expect(restored.logoUrl, 'logo');
      expect(restored.ringtonePath, 'tone');
      expect(restored.backgroundColor, '#000000');
      expect(restored.durationMs, 15000);
    });
  });

  group('showMissCallNotification / showCallNotification payload', () {
    test('channel argument map matches flutter_callkit_incoming-style keys', () {
      const event = CallEvent(
        sessionId: 'sess-1',
        callType: 1,
        callerId: 10,
        callerName: 'Caller',
        opponentsIds: {20},
        duration: 12000,
        missedCallNotification: MissedCallNotificationParams(
          showNotification: true,
          subtitle: 'Missed call',
          callbackText: 'Call back',
          isShowCallback: true,
        ),
        android: AndroidCallKitParams(
          isShowLogo: true,
          logoUrl: 'call_logo',
          ringtonePath: 'ringtone_default',
          backgroundColor: '#0955FA',
          backgroundUrl: 'call_background',
          durationMs: 12000,
        ),
      );

      final args = event.toMap();
      expect(args['session_id'], 'sess-1');
      expect(args['duration'], 12000);
      expect(args['missed_call_notification'], isA<Map>());
      expect(
        (args['missed_call_notification'] as Map)['callback_text'],
        'Call back',
      );
      expect(args['android'], isA<Map>());
      expect((args['android'] as Map)['ringtone_path'], 'ringtone_default');
      expect((args['android'] as Map)['is_show_logo'], isTrue);
      expect((args['android'] as Map)['background_url'], 'call_background');
    });
  });

  group('BackgroundCallbackName', () {
    test('includes missed callback name', () {
      expect(
        BackgroundCallbackName.MISSED_CALLBACK_IN_BACKGROUND,
        'missed_callback_in_background',
      );
    });
  });
}
