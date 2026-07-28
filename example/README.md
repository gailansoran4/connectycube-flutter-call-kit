# Call Kit example

Demo for `connectycube_flutter_call_kit` with flutter_callkit_incoming-style customization.

## Android assets

| Asset | Path |
|-------|------|
| Ringtone | `android/app/src/main/res/raw/ringtone_default.mp3` |
| Logo | `android/app/src/main/res/drawable/call_logo.png` |
| Background | `android/app/src/main/res/drawable/call_background.png` |

## Run

```bash
cd example
flutter pub get
flutter run
```

## Android assets (Flutter)

Declare in `example/pubspec.yaml` (already done):

- `assets/ringtone/call_ring.mp3` — pass as `assets/ringtone/call_ring` (extension optional)
- `assets/image/call_icon.png`
- `assets/image/call_background.png`

Or keep using drawable/raw names under `android/app/src/main/res/`.

## Automated tests

From the package root:

```bash
flutter test
```

Covers:
- `MissedCallNotificationParams` / `AndroidCallKitParams` serialization
- `CallEvent` payloads for incoming, timeout duration, missed notification, Android UI
- `updateConfig` argument map
- Foreground event routing: `timeoutCall`, `missedCallCallback`, accept/reject/incoming
- Background callback name constants (including missed callback)

## Manual test matrix (Android + iOS)

For each platform, repeat in **foreground**, **background** (Home), and **terminated** (swipe away):

| # | Case | Steps | Expected |
|---|------|-------|----------|
| 1 | Incoming UI | Tap **Incoming (30s)** | Ringtone, logo/avatar, background/colors, Accept/Decline |
| 2 | Accept | Accept on UI/notification | `ACCEPTED` log; UI dismissed |
| 3 | Reject | Decline on UI/notification | `REJECTED` log; UI dismissed |
| 4 | Timeout → missed | Tap **Incoming (10s → miss)**, wait | `TIMEOUT`; missed notification |
| 5 | Missed body tap | Tap missed notification body | App opens (launcher) with call extras |
| 6 | Missed Callback | Tap **Call back** on missed | App opens + `MISSED CALLBACK` log |
| 7 | Manual missed | Tap **Show missed** | Missed notification appears |
| 8 | End while ringing | Incoming then **End last** | Incoming cleared; no spurious miss preferred |
| 9 | Full-screen (Android) | **Full-screen intent** + lock device + Incoming | Lock-screen incoming when permission granted |

### Terminated notes

- Register top-level handlers (`onCallAcceptedWhenTerminated`, etc.) before `runApp` / in `main` as shown in `lib/main.dart`.
- Android FCM / iOS PushKit terminated wakes need real push payloads (`signal_type=startCall`). Use device logs to verify isolate callbacks.
