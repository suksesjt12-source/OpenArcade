# AstraNS Launcher

![Android](https://img.shields.io/badge/platform-Android-3DDC84)
![Build](https://img.shields.io/badge/build-GitHub_Actions-blue)
![License](https://img.shields.io/badge/license-MIT-green)

AstraNS Launcher is an Android launcher prototype focused on organizing a Windows game setup for use with a compatible Android runtime such as Winlator.

## Current features

- AstraNS home screen with a dedicated Naruto Shippuden: Ultimate Ninja Storm Revolution profile.
- Select and persist a game folder reference using Android's Storage Access Framework.
- Select and persist a local `.exe` or `.zip` file reference.
- Detect and open an installed app whose launcher label contains "Winlator".
- Local profile preferences for target resolution (854×480, 960×540, 1280×720), target FPS (30/60), and a touchscreen-controls guide preference.
- Existing OpenArcade Android application library remains accessible from the home screen.
- GitHub Actions workflow to build and upload a debug APK artifact.

## Important runtime limitation

AstraNS is a launcher and file-reference manager. It does not contain the Windows game, a Windows emulator, Box64, Wine, DXVK, or Vulkan drivers. The selected executable is not automatically imported into or launched by Winlator. Open Winlator and select the game executable from inside the runtime.

The resolution, FPS, and control preferences in AstraNS are saved locally as a profile guide. They do not directly modify Winlator containers, VSync, DXVK, Box64, or the runtime's touch overlay.

Only use game files that you own or are otherwise authorized to use.

## Build APK with GitHub Actions

1. Open the **Actions** tab in this repository.
2. Select **Android APK**.
3. Select **Run workflow** when building manually, or wait for the workflow to run after a push to `master` or `main`.
4. Open the completed workflow run and download the artifact named **AstraNS-Launcher-debug**.

The workflow runs `./gradlew assembleGithubDebug`. The generated APK is uploaded as an Actions artifact.

## Local build

Requirements: JDK 17 and Android SDK platform 37.

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
