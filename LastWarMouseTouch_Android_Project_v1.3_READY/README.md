# LastWar MouseTouch

Galaxy Tab utility app for Last War.

- Forces Last War into landscape using the existing orientation-overlay helper.
- Converts Bluetooth mouse primary-click input to an Android touch tap while Last War is active (Android 14/API 34+).
- Uses an AccessibilityService only for input conversion; it does not read screen contents.
- No boot auto-start.
- Custom cute launcher icon.

## Build
GitHub Actions automatically builds `ScreenRotation_MouseTouch_LastWar_v1.3.apk` after a push to `main`.
