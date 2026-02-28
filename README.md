# Burrow UI

![](home.png)

Burrow UI is an open-source, free launcher designed specifically for E-ink devices. Inspired by the Niagara Launcher, Burrow UI offers a minimalist and efficient interface tailored for E-ink screens.

## Features

- **Open Source and Free**: Burrow UI is completely open-source and free to use, with no hidden costs or in-app purchases.
- **No Ads**: Enjoy a clean, distraction-free experience without any advertisements.
- **Offline Functionality**: Works entirely offline, respecting your privacy and conserving battery life.
- **Ultra-Lightweight**: With an installation package of only 130KB, Burrow UI is incredibly light on system resources.
- **E-ink Optimized**: Designed from the ground up for E-ink displays, ensuring optimal readability and performance.

## Installation

Download the latest release of Burrow UI from the [Releases](https://github.com/hamsterbase/Burrow-UI/releases)

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

## Support Us

If you find Burrow UI helpful, consider supporting our work:

[Buy us a coffee](https://buymeacoffee.com/hamsterbase)

## Our Other Products

Check out our other innovative products:

- [HamsterBase](https://hamsterbase.com) - A privacy-focused and offline-friendly tool for deferred reading.

## License

Distributed under the GNU General Public License v3.0 (GPL-3.0) License. See `LICENSE` for more information.

## Contact

HamsterBase - admin@hamsterbase.com

Project Link: [https://github.com/hamsterbase/burrow-ui](https://github.com/hamsterbase/burrow-ui)

## Acknowledgements

- Inspired by Niagara Launcher
