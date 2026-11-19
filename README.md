# AstraNS Launcher

![Android](https://img.shields.io/badge/platform-Android-3DDC84)
![Build](https://img.shields.io/badge/build-GitHub_Actions-blue)
![License](https://img.shields.io/badge/license-MIT-green)

AstraNS Launcher is an Android launcher prototype for organizing a Windows game setup and handing launch requests to an installed compatible Android runtime.

## Current features

- AstraNS home screen with a dedicated Naruto Shippuden: Ultimate Ninja Storm Revolution profile.
- Select and persist a game folder reference using Android's Storage Access Framework.
- Select and persist a local `.exe` or `.zip` file reference.
- Detect installed Winlator builds by app label or package name, including separately named variants.
- Save the selected Winlator package so the launcher can reopen the preferred runtime.
- Open the selected runtime from AstraNS.
- Select an exported `.desktop` shortcut or enter its absolute filesystem path, then forward it using the `frontend_desktop_path` intent extra.
- Local profile preferences for target resolution (854×480, 960×540, 1280×720), target FPS (30/60), and a touchscreen-controls guide preference.
- Existing OpenArcade Android application library remains accessible from the home screen.
- GitHub Actions workflow to build and upload a debug APK artifact.

## Runtime integration and limitations

AstraNS does **not** bundle the Windows runtime. Wine, Box64, DXVK, Vulkan drivers, and the Winlator container environment remain part of the separately installed runtime app. Install and configure a compatible Winlator build on the device before using the runtime controls.

The shortcut hand-off sends the absolute path of an exported `.desktop` file in the `frontend_desktop_path` intent extra. This is a front-end integration convention, not a guaranteed public API supported by every Winlator release. A runtime build that does not recognize this extra may simply open its main screen; in that case, launch the game from inside Winlator. AstraNS does not claim that shortcut auto-launch works on unverified runtime builds.

The resolution, FPS, and control preferences in AstraNS are saved locally as a profile guide. They do not directly modify Winlator containers, VSync, DXVK, Box64 presets, or the runtime's touchscreen overlay. Only use game files that you own or are otherwise authorized to use.

## Build APK with GitHub Actions

1. Open the **Actions** tab in this repository.
2. Select **Android APK**.
3. Select **Run workflow** when building manually, or wait for the workflow to run after a push to `master` or `main`.
4. Open the completed workflow run and download the artifact named **AstraNS-Launcher-debug**.

The workflow runs `./gradlew assembleGithubDebug`. The generated APK is uploaded as an Actions artifact.

## Local build

Requirements: JDK 17 and Android SDK platform 36.

```bash
chmod +x ./gradlew
./gradlew assembleGithubDebug
```

Expected output:

```
app/build/outputs/apk/github/debug/
```

## Upstream and license

This project is based on [OpenArcade](https://github.com/kys0ff/OpenArcade), which is distributed under the MIT License. The upstream license file is retained in this repository. See [LICENSE](LICENSE) for the license text.
