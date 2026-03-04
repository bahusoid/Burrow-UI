# Old Burrow UI

![](home.png)

Old Burrow UI is an open-source, free launcher designed specifically for E-ink devices.

Based on the original project: [https://github.com/hamsterbase/Burrow-UI](https://github.com/hamsterbase/Burrow-UI)

This mod targets older Android 4.4 devices and includes additional compatibility-focused improvements. Tested on NOOK GlowLight Plus (BNRV510).

## Installation

Download the latest release of Old Burrow UI from the [Releases](https://github.com/bahusoid/Burrow-UI/releases)

### Mod changes

- Android 4.4 (API 19) support.
- Launcher shortcut pinning support (including legacy shortcut install handling).
- New main-screen icon management feature: long-press context menu with Move and Remove actions.
- New main-screen Move functionality: move to top/up/down/bottom, switch moving target by tapping another icon while Move dialog is open, and full-order restore on Cancel.
- App Selection improvements: select-all control and search.

## Build signed release APK (your own key)

This project reads release signing credentials from `signing.properties` in the project root.

1. Create a keystore (example):

	```bash
	keytool -genkeypair -v \
	  -keystore keystore/my-release.keystore \
	  -alias my-key-alias \
	  -keyalg RSA -keysize 2048 -validity 3650
	```

2. Copy `signing.properties.example` to `signing.properties` and set your values:

	```properties
	storeFile=keystore/my-release.keystore
	storePassword=YOUR_STORE_PASSWORD
	keyAlias=my-key-alias
	keyPassword=YOUR_KEY_PASSWORD
	```

3. Build release APK:

	```bash
	./gradlew :app:assembleRelease
	```

APK output path:

`app/build/outputs/apk/release/burrow-ui-release-<version>.apk`

### Mod Versioning

- Uses a legacy suffix in `versionName` (example: `1.2.0-legacy.1`).
- Uses a fork-safe increasing `versionCode` formula: `upstreamCode * 100 + forkPatch`.
- Current release: `versionName=1.2.0-legacy.1`, `versionCode=301`.
- Example next versions: `1.2.0-legacy.2` -> `302`, `1.2.1-legacy.1` -> `401`.

## License

Distributed under the GNU General Public License v3.0 (GPL-3.0) License. See `LICENSE` for more information.

## Acknowledgements
- Original project: [https://github.com/hamsterbase/Burrow-UI](https://github.com/hamsterbase/Burrow-UI)
- Inspired by Niagara Launcher
