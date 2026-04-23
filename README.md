# Digital Clock

A tiny Android app that shows a 7-segment style clock over a pure-black background.

- Big **HH:MM** centered on the screen, smaller **SS** to the right aligned with the bottom of the primary glyphs.
- Bright green segments on a `#000000` background (OLED-friendly).
- Full immersive mode — no status bar, no nav bar.
- Keeps the screen awake while the app is in the foreground.
- Supports auto-rotation; glyphs re-size to fit landscape and portrait.

## Build locally

```bash
cd android
./gradlew assembleDebug
# APK → android/app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected device:

```bash
./gradlew installDebug
```

## Automatic APK releases (GitHub Actions)

The workflow in [`.github/workflows/android-release.yml`](.github/workflows/android-release.yml) builds the debug APK on every push to `main` that touches `android/**`, and publishes it as a GitHub Release with a dated tag. You can also run it manually from the Actions tab ("Run workflow").

No signing secrets are required — the debug key is generated inside the runner. For a signed release build, add your keystore as a secret and swap `assembleDebug` for `assembleRelease` with signing configured.
