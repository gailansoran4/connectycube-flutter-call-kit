import 'dart:async';

import 'package:connectycube_flutter_call_kit/connectycube_flutter_call_kit.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'dart:io' show Platform;

@pragma('vm:entry-point')
Future<void> onCallAcceptedWhenTerminated(CallEvent event) async {
  print('[BG] accepted: $event');
}

@pragma('vm:entry-point')
Future<void> onCallRejectedWhenTerminated(CallEvent event) async {
  print('[BG] rejected: $event');
}

@pragma('vm:entry-point')
Future<void> onCallIncomingWhenTerminated(CallEvent event) async {
  print('[BG] incoming: $event');
}

@pragma('vm:entry-point')
Future<void> onMissedCallCallbackWhenTerminated(CallEvent event) async {
  print('[BG] missed callback: $event');
}

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const CallKitExampleApp());
}

class CallKitExampleApp extends StatefulWidget {
  const CallKitExampleApp({Key? key}) : super(key: key);

  @override
  State<CallKitExampleApp> createState() => _CallKitExampleAppState();
}

class _CallKitExampleAppState extends State<CallKitExampleApp> {
  final List<String> _logs = <String>[];
  String? _lastSessionId;
  bool _configured = false;

  void _log(String message) {
    final line = '${DateTime.now().toIso8601String().substring(11, 19)}  $message';
    print(line);
    setState(() {
      _logs.insert(0, line);
      if (_logs.length > 40) _logs.removeLast();
    });
  }

  @override
  void initState() {
    super.initState();
    _initCallKit();
  }

  Future<void> _initCallKit() async {
    ConnectycubeFlutterCallKit.onCallAcceptedWhenTerminated =
        onCallAcceptedWhenTerminated;
    ConnectycubeFlutterCallKit.onCallRejectedWhenTerminated =
        onCallRejectedWhenTerminated;
    ConnectycubeFlutterCallKit.onCallIncomingWhenTerminated =
        onCallIncomingWhenTerminated;
    ConnectycubeFlutterCallKit.onMissedCallCallbackWhenTerminated =
        onMissedCallCallbackWhenTerminated;

    ConnectycubeFlutterCallKit.instance.init(
      onCallAccepted: (event) async {
        _log('ACCEPTED ${event.sessionId}');
        await ConnectycubeFlutterCallKit.reportCallAccepted(
            sessionId: event.sessionId);
      },
      onCallRejected: (event) async {
        _log('REJECTED ${event.sessionId}');
        await ConnectycubeFlutterCallKit.reportCallEnded(
            sessionId: event.sessionId);
        await ConnectycubeFlutterCallKit.clearCallData(
            sessionId: event.sessionId);
      },
      onCallIncoming: (event) async {
        _log('INCOMING ${event.sessionId}');
      },
      onCallTimeout: (event) async {
        _log('TIMEOUT → missed ${event.sessionId}');
      },
      onMissedCallCallback: (event) async {
        _log('MISSED CALLBACK ${event.sessionId}');
      },
      // Flutter asset paths work on both platforms (iOS resolves them from the
      // app bundle; note UNNotification sounds need caf/wav/aiff format).
      ringtone: 'assets/ringtone/call_ring',
      icon: 'assets/image/call_icon.png',
      background:
          (!kIsWeb && Platform.isAndroid) ? 'assets/image/call_background.png' : null,
      color: '#0955FA',
      logo: (!kIsWeb && Platform.isAndroid) ? 'assets/image/call_icon.png' : null,
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

    // Enable logo on full-screen UI via persisted config.
    await ConnectycubeFlutterCallKit.instance.updateConfig(
      logo: (!kIsWeb && Platform.isAndroid) ? 'assets/image/call_icon.png' : null,
      background: (!kIsWeb && Platform.isAndroid)
          ? 'assets/image/call_background.png'
          : null,
      ringtone: 'assets/ringtone/call_ring',
      icon: 'assets/image/call_icon.png',
      color: '#0955FA',
      backgroundColor: '#0955FA',
      showMissedCallNotification: true,
      showMissedCallCallback: true,
      defaultDurationMs: 30000,
    );

    setState(() => _configured = true);
    _log('CallKit configured');
  }

  CallEvent _buildEvent({int durationMs = 30000}) {
    final sessionId = DateTime.now().millisecondsSinceEpoch.toString();
    _lastSessionId = sessionId;
    return CallEvent(
      sessionId: sessionId,
      callType: 1,
      callerId: 1001,
      callerName: 'Alice Demo',
      opponentsIds: {2002},
      callPhoto: null,
      userInfo: {'source': 'example'},
      duration: durationMs,
      acceptButtonLabel: 'Accept',
      rejectButtonLabel: 'Decline',
      // Android only: background tints the full-screen round buttons and the
      // heads-up notification buttons; text color is applied to the button
      // labels in both states. iOS CallKit does not allow custom button
      // names/colors (system limitation).
      acceptButtonBackgroundColor: '#00C853',
      acceptButtonTextColor: '#FFFFFF',
      rejectButtonBackgroundColor: '#D50000',
      rejectButtonTextColor: '#FFFFFF',
      missedCallNotification: const MissedCallNotificationParams(
        showNotification: true,
        subtitle: 'Missed call',
        callbackText: 'Call back',
        isShowCallback: true,
      ),
      android: AndroidCallKitParams(
        isShowLogo: true,
        logoUrl: 'assets/image/call_icon.png',
        ringtonePath: 'assets/ringtone/call_ring',
        backgroundColor: '#0955FA',
        backgroundUrl: 'assets/image/call_background.png',
        actionColor: '#0955FA',
        textColor: '#FFFFFF',
        textAccept: 'Accept',
        textDecline: 'Decline',
        acceptButtonBackgroundColor: '#00C853',
        acceptButtonTextColor: '#FFFFFF',
        declineButtonBackgroundColor: '#D50000',
        declineButtonTextColor: '#FFFFFF',
        durationMs: durationMs,
        missedCallNotificationChannelName: 'Missed Call',
        isShowFullLockedScreen: true,
      ),
      // iOS: system CallKit only — no logo/background/colors UI.
      ios: const IOSCallKitParams(
        iconName: 'assets/image/call_icon.png', // or Assets.xcassets name
        handleType: 'generic',
        supportsVideo: true,
        // Flutter asset path also works; system default is used if not found.
        ringtonePath: 'assets/ringtone/call_ring',
        includesCallsInRecents: false,
      ),
    );
  }

  Future<void> _showIncoming({int durationMs = 30000}) async {
    final event = _buildEvent(durationMs: durationMs);
    _log('showCallNotification ${event.sessionId} (${durationMs}ms)');
    await ConnectycubeFlutterCallKit.showCallNotification(event);
  }

  Future<void> _showMissed() async {
    final event = _buildEvent();
    _log('showMissCallNotification ${event.sessionId}');
    await ConnectycubeFlutterCallKit.showMissCallNotification(event);
  }

  Future<void> _endLast() async {
    final id = _lastSessionId;
    if (id == null) {
      _log('No last session');
      return;
    }
    await ConnectycubeFlutterCallKit.reportCallEnded(sessionId: id);
    await ConnectycubeFlutterCallKit.clearCallData(sessionId: id);
    _log('ended $id');
  }

  Future<void> _requestFullScreen() async {
    if (kIsWeb || !Platform.isAndroid) {
      _log('Full-screen intent is Android-only');
      return;
    }
    final can = await ConnectycubeFlutterCallKit.canUseFullScreenIntent();
    _log('canUseFullScreenIntent=$can');
    if (!can) {
      await ConnectycubeFlutterCallKit.provideFullScreenIntentAccess();
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('CallKit example'),
          backgroundColor: const Color(0xFF0955FA),
        ),
        body: Column(
          children: [
            Padding(
              padding: const EdgeInsets.all(12),
              child: Text(
                _configured
                    ? 'Configured · last=$_lastSessionId'
                    : 'Configuring…',
              ),
            ),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              alignment: WrapAlignment.center,
              children: [
                ElevatedButton(
                  onPressed: () => _showIncoming(durationMs: 30000),
                  child: const Text('Incoming (30s)'),
                ),
                ElevatedButton(
                  onPressed: () => _showIncoming(durationMs: 10000),
                  child: const Text('Incoming (10s → miss)'),
                ),
                ElevatedButton(
                  onPressed: _showMissed,
                  child: const Text('Show missed'),
                ),
                ElevatedButton(
                  onPressed: _endLast,
                  child: const Text('End last'),
                ),
                ElevatedButton(
                  onPressed: _requestFullScreen,
                  child: const Text('Full-screen intent'),
                ),
              ],
            ),
            const Divider(),
            const Padding(
              padding: EdgeInsets.all(8),
              child: Text('Event log', style: TextStyle(fontWeight: FontWeight.bold)),
            ),
            Expanded(
              child: ListView.builder(
                itemCount: _logs.length,
                itemBuilder: (_, i) => ListTile(
                  dense: true,
                  title: Text(_logs[i], style: const TextStyle(fontSize: 12)),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
