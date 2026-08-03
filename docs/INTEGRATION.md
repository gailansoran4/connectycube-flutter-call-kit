# Connectycube Flutter Call Kit (fork) — Integration Guide

This document explains how to use **this fork** of `connectycube_flutter_call_kit`
from another Flutter project, and documents all the extra features added on top
of the upstream plugin (feature parity with `flutter_callkit_incoming`).

- Fork repository: https://github.com/gailansoran4/connectycube-flutter-call-kit
- Upstream: https://github.com/ConnectyCube/connectycube-flutter-call-kit
- Package name: `connectycube_flutter_call_kit` (version 3.0.0)
- Requires: Dart `>=3.12.0`, Flutter `>=3.44.0`

---

## 1. Add the fork to your project

In your app's `pubspec.yaml`, reference the fork as a **git dependency**
(do not use the pub.dev version — it does not contain these features):

```yaml
dependencies:
  connectycube_flutter_call_kit:
    git:
      url: https://github.com/gailansoran4/connectycube-flutter-call-kit.git
      ref: master # or pin a commit SHA for reproducible builds, e.g. ref: e218d84
```

Pinning a commit SHA is recommended for production apps:

```yaml
  connectycube_flutter_call_kit:
    git:
      url: https://github.com/gailansoran4/connectycube-flutter-call-kit.git
      ref: <commit-sha>
```

Then run `flutter pub get`.

---

## 2. Declare your assets

The fork can load the **ringtone, icon, logo, and background** directly from
Flutter assets. Declare them in your app's `pubspec.yaml`:

```yaml
flutter:
  assets:
    - assets/ringtone/call_ring.mp3
    - assets/image/call_icon.png
    - assets/image/call_background.png
```

Asset support per platform:

| Feature | Android | iOS |
|---|---|---|
| Icon | Flutter asset / drawable / URL | Flutter asset or `Assets.xcassets` name (CallKit template icon) |
| Ringtone | Flutter asset or `res/raw` name | Flutter asset or bundled sound name (e.g. `Ringtone.caf`) |
| Logo | Flutter asset / drawable / URL | Not possible (system CallKit UI) |
| Background image/color | Flutter asset / drawable / URL / `#RRGGBB` | Not possible (system CallKit UI) |
| Button labels / text colors | Yes | Not possible (system CallKit UI) |
| Accept/Decline button background + text colors (heads-up notification and full-screen UI) | Yes | Not possible (system CallKit UI) |

iOS notes:
- iOS always shows the **system CallKit screen**. No app can change its
  logo, background, colors, or button labels — this is an Apple restriction
  (the same applies to `flutter_callkit_incoming`).
- Flutter asset ringtones are resolved inside the app bundle. If the sound is
  `.caf`/`.wav`/`.aiff` it is also reused for the **missed-call notification**
  sound; `.mp3` rings for the incoming call but the missed-call notification
  falls back to the default sound.
- If a Flutter asset ringtone ever fails on a physical device, the guaranteed
  route is adding the sound file to the Runner target in Xcode and passing its
  plain name (e.g. `Ringtone.caf`).

---

## 3. Platform setup

### Android

- Put `google-services.json` in `your_app/android/app/`.
- Add to `your_app/android/app/build.gradle`:

```groovy
apply plugin: 'com.google.gms.google-services'
```

- For `targetSdkVersion 33+` request `POST_NOTIFICATIONS` at runtime.
- For Android 14+ full-screen incoming UI, check/request the full-screen
  intent permission (see the API below).

### iOS

Add to `your_app/ios/Runner/Info.plist`:

```xml
<key>UIBackgroundModes</key>
<array>
    <string>remote-notification</string>
    <string>voip</string>
</array>
```

---

## 4. Initialize the plugin

```dart
import 'package:connectycube_flutter_call_kit/connectycube_flutter_call_kit.dart';

ConnectycubeFlutterCallKit.instance.init(
  onCallAccepted: (event) async { /* start your call session */ },
  onCallRejected: (event) async { /* cancel the call */ },
  onCallIncoming: (event) async { /* incoming UI shown */ },
  onCallTimeout: (event) async { /* rang out -> missed */ },
  onMissedCallCallback: (event) async { /* "Call back" tapped */ },

  // Asset-based customization (works on both platforms where possible):
  ringtone: 'assets/ringtone/call_ring',        // extension optional
  icon: 'assets/image/call_icon.png',
  background: 'assets/image/call_background.png', // Android only
  logo: 'assets/image/call_icon.png',             // Android only
  isShowLogo: true,                                // Android full-screen logo
  color: '#0955FA',                                // Android accent
  backgroundColor: '#0955FA',                      // Android only
  actionColor: '#0955FA',                          // Android only
  textColor: '#FFFFFF',                            // Android only

  // Missed-call defaults:
  missedCallSubtitle: 'Missed call',
  missedCallCallbackText: 'Call back',
  showMissedCallNotification: true,
  showMissedCallCallback: true,
  missedCallNotificationChannelName: 'Missed Call',

  // Ring duration before the call becomes "missed":
  defaultDurationMs: 30000,
);
```

The same options can be changed later with
`ConnectycubeFlutterCallKit.instance.updateConfig(...)` (persisted natively, so
they also apply when the app is terminated).

### Terminated / background handlers

Top-level or static functions annotated with `@pragma('vm:entry-point')`:

```dart
@pragma('vm:entry-point')
Future<void> onCallAcceptedWhenTerminated(CallEvent event) async { /* ... */ }

@pragma('vm:entry-point')
Future<void> onMissedCallCallbackWhenTerminated(CallEvent event) async { /* ... */ }

ConnectycubeFlutterCallKit.onCallAcceptedWhenTerminated = onCallAcceptedWhenTerminated;
ConnectycubeFlutterCallKit.onCallRejectedWhenTerminated = onCallRejectedWhenTerminated;
ConnectycubeFlutterCallKit.onCallIncomingWhenTerminated = onCallIncomingWhenTerminated;
ConnectycubeFlutterCallKit.onMissedCallCallbackWhenTerminated = onMissedCallCallbackWhenTerminated;
```

---

## 5. Show an incoming call (with per-call customization)

```dart
await ConnectycubeFlutterCallKit.showCallNotification(CallEvent(
  sessionId: sessionId,           // UUID string
  callType: 1,                    // 0 = audio, 1 = video
  callerId: 1001,
  callerName: 'Alice',
  opponentsIds: {2002},
  userInfo: {'any': 'data'},

  duration: 30000,                // ring time in ms before timeout -> missed
  acceptButtonLabel: 'Accept',
  rejectButtonLabel: 'Decline',
  // Android only (heads-up notification + full-screen UI); ignored on iOS.
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

  android: const AndroidCallKitParams(
    isShowLogo: true,
    logoUrl: 'assets/image/call_icon.png',
    ringtonePath: 'assets/ringtone/call_ring',
    backgroundColor: '#0955FA',
    backgroundUrl: 'assets/image/call_background.png',
    actionColor: '#0955FA',
    textColor: '#FFFFFF',
    textAccept: 'Accept',
    textDecline: 'Decline',
    // Same as the CallEvent-level fields (CallEvent values win when both set):
    acceptButtonBackgroundColor: '#00C853',
    acceptButtonTextColor: '#FFFFFF',
    declineButtonBackgroundColor: '#D50000',
    declineButtonTextColor: '#FFFFFF',
    missedCallNotificationChannelName: 'Missed Call',
    isShowFullLockedScreen: true,
    durationMs: 30000,
  ),

  ios: const IOSCallKitParams(
    iconName: 'assets/image/call_icon.png', // or Assets.xcassets name
    handleType: 'generic',                  // generic | number | email
    supportsVideo: true,
    ringtonePath: 'assets/ringtone/call_ring', // asset or bundled name
    includesCallsInRecents: false,
  ),
));
```

## 6. Show a missed-call notification manually

```dart
await ConnectycubeFlutterCallKit.showMissCallNotification(callEvent);
```

Tapping the body opens the app; tapping **Call back** fires
`onMissedCallCallback` (foreground) or
`onMissedCallCallbackWhenTerminated` (background/terminated).

## 7. Other useful APIs

```dart
// Tokens (FCM on Android, VoIP on iOS):
final token = await ConnectycubeFlutterCallKit.getToken();
ConnectycubeFlutterCallKit.onTokenRefreshed = (token) { /* resubscribe */ };

// Call state management:
await ConnectycubeFlutterCallKit.reportCallAccepted(sessionId: id);
await ConnectycubeFlutterCallKit.reportCallEnded(sessionId: id);
final state = await ConnectycubeFlutterCallKit.getCallState(sessionId: id);
await ConnectycubeFlutterCallKit.clearCallData(sessionId: id);
final lastId = await ConnectycubeFlutterCallKit.getLastCallId();

// Android 14+ full-screen intent permission:
final canFullScreen = await ConnectycubeFlutterCallKit.canUseFullScreenIntent();
if (!canFullScreen) {
  await ConnectycubeFlutterCallKit.provideFullScreenIntentAccess();
}
```

---

## 8. Event flow summary

| Scenario | Foreground | Background / terminated |
|---|---|---|
| Incoming call shown | `onCallIncoming` | `onCallIncomingWhenTerminated` |
| User accepts | `onCallAccepted` | `onCallAcceptedWhenTerminated` |
| User declines | `onCallRejected` | `onCallRejectedWhenTerminated` |
| Rings out (timeout) | `onCallTimeout` + missed notification | missed notification shown natively |
| Missed "Call back" tapped | `onMissedCallCallback` | `onMissedCallCallbackWhenTerminated` |

## 9. Differences from upstream (what this fork adds)

- `MissedCallNotificationParams`, `AndroidCallKitParams`, `IOSCallKitParams`
  on `CallEvent` for per-call customization.
- Ring-duration timeout with automatic missed-call notification
  (`duration` / `defaultDurationMs`, `onCallTimeout`).
- Missed-call notification with **Call back** action on Android **and** iOS,
  plus `showMissCallNotification` for manual display.
- Flutter asset paths (`assets/...`) accepted for ringtone / icon / logo /
  background on Android, and for ringtone / CallKit icon on iOS.
- Android full-screen incoming UI: background image/color + caller name
  (+ accept/decline buttons). Avatar and call type are hidden; app logo
  shows when `isShowLogo` is true.
- Android heads-up notification left icon: logo only (never caller photo);
  caller name is still shown as the title.
- Android Accept/Decline button customization: label, background color, and
  foreground/text color, applied to both the heads-up notification (custom
  layout replaces the system `CallStyle` when set) and the full-screen UI.
  Not possible on iOS (system CallKit UI).
- iOS: live `CXProvider` configuration refresh (config changes after init now
  take effect), CallKit icon from Flutter assets, asset ringtone resolution.

## 10. Example app

A complete working demo (all flows: incoming, timeout → missed, missed
callback, full-screen intent) lives in the fork:
https://github.com/gailansoran4/connectycube-flutter-call-kit/tree/master/example
