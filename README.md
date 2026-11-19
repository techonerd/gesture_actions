# Gesture Actions

Motion gestures for any Android phone, inspired by the quick gestures found on Motorola phones.
Chop twice to toggle the flashlight, twist twice to open the camera — no need to unlock the phone or find a button.

> Working name. The final app name may change before the Play Store release.

## Gestures

| Gesture | Motion | Action | Status |
|---|---|---|---|
| **Chop Chop** | Two quick downward chops, like swinging a hammer | Toggle flashlight | ✅ Available |
| **Double Twist** | Twist your wrist back and forth twice | Open camera | ✅ Available |
| **Flip to Shhh** | Place the phone face-down on a table | Turn on Do Not Disturb; restores when picked up | 🛠️ Planned |
| **Pick Up to Quiet** | Pick up the phone while it is ringing | Switch the ringer to vibrate | 🛠️ Planned |
| **Back Tap** | Double-tap the back of the phone | Run a chosen action | 🛠️ Planned |
| **Flick to Skip** | Quick wrist flick while music is playing | Next track, even with the screen off | 🛠️ Planned |

### Chop Chop → Flashlight

- **State-aware toggle.** The app listens to the system torch state, so the gesture always does the opposite of what the torch is doing right now — even if you turned it on from Quick Settings or another app.
- If the camera is in use by another app, the torch can't be controlled, so the gesture is ignored instead of getting out of sync.
- Short vibration on toggle (can be turned off).

### Double Twist → Camera

- Opens the camera directly, including from the lock screen (secure camera mode, your phone stays locked).
- Twist again while the camera is open to switch between front and rear camera.
- Choose the default camera: rear, front, or automatic.

## Settings

- Turn each gesture on or off individually
- Chop sensitivity slider (twist sensitivity coming soon)
- Vibration feedback on/off
- Theme: matches your device style (One UI, HyperOS) or a default theme, with light/dark mode

## Free and Pro

| | Free | Pro *(planned)* |
|---|---|---|
| Chop Chop → Flashlight | ✅ | ✅ |
| Double Twist → Camera | ✅ | ✅ |
| New gestures as they ship | ✅ | ✅ |
| **Assign any action to any gesture** (open an app, toggle DND, play/pause media, and more) | — | ✅ |

## How it works

Gestures are detected from the accelerometer and gyroscope using Android's `SensorManager`.
A foreground service (with a small status notification) keeps detection running in the background.

- **Chop:** looks for two linear-acceleration peaks within a short time window.
- **Twist:** looks for two gyroscope rotation peaks in opposite directions around the phone's long axis.

## Permissions

| Permission | Why |
|---|---|
| Foreground service | Keep gesture detection running in the background |
| Display over other apps | Allow the camera to open from the background |
| Vibrate | Haptic feedback when a gesture triggers |
| Run at startup | Resume gesture detection after the phone restarts |
| Ignore battery optimizations | Stop aggressive battery savers from killing detection |

The app works fully offline. No data leaves your phone.

> The current build still declares a few extra permissions (internet, camera, write settings, disable keyguard) that are being removed before the Play Store release. The app does not use an accessibility service.

## Build

Requirements: JDK 17+, Android SDK platform 37.2 (compile) — the app targets API 36.

- **Android Studio:** open the project and run the `app` configuration.
- **Command line:** `./gradlew assembleDebug`, or `./build_apk.sh` to also set up the SDK without Android Studio.

## Roadmap

- Planned gestures from the table above
- Custom action mapping (Pro)
- Play Store release

## Made by

Built by [techonerd](https://github.com/techonerd), with quality assurance by Casper 🐈 (mostly by sitting on the keyboard).
