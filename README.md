# Between Sets

A gym rest timer for Android. Pick 30s, 1:00, 1:30, 2:00 or 3:00 (or save your own), and it chimes when
the rest is over, even with the phone locked. Turn on auto-repeat and the next rest starts by itself.

- **Home screen widget:** live countdown, Pause/Resume, +15s, Stop, and the five presets.
- **Lock screen:** a notification with the countdown and the same buttons.
- **App icon shortcuts:** long-press the icon for one-tap 30s to 3:00 rests.
- **Chime:** plays at alarm volume, on an alarm-clock alarm, so Android's battery saver doesn't delay it.
- **Set counter:** goes up each time a rest finishes.

## Install on your phone

Every push to `main` builds a signed APK on GitHub and posts it under
[Releases](https://github.com/ngfamclam-cpu/between-sets/releases/latest).

1. On the phone, open the latest release and tap `BetweenSets-vN.apk`.
2. Open the download. If Android asks, allow your browser to install unknown apps.
3. Open Between Sets and allow notifications (needed for the lock-screen controls).
4. Add the widget: long-press the home screen → Widgets → Between Sets.

New versions install over the old one and keep your settings.

## Play Store

See [PLAY_STORE.md](PLAY_STORE.md). Each release also has a `-playstore.aab` file for upload.

## iPhone

Not built yet. An iPhone version needs a separate SwiftUI app (widget + Live Activity), and a Mac or a paid
Apple Developer account to install it.

## Project layout

- `android/app/src/main/java/com/betweensets/app/`
  - `TimerLogic.kt`: the timer's state and every change to it (unit tested)
  - `TimerController.kt`: applies a change, then syncs the alarm, notification and widget
  - `TimerReceiver.kt`: the alarm and all widget/notification buttons
  - `TimerWidget.kt`, `Notifs.kt`, `Chime.kt`, `TimerScreen.kt`
- `.github/workflows/android.yml`: tests, then builds the signed APK and AAB

The signing key is kept outside this repo. GitHub has it as the secrets `BS_KEYSTORE_BASE64`,
`BS_KEYSTORE_PASSWORD` and `BS_KEY_ALIAS`.
