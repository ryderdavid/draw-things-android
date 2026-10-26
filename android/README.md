# Draw Remote Android

Native remote-only Draw Things client. No AI models or inference targets are included.
The official Mac app performs generation; Android handles controls, drawing, codecs,
and encrypted private storage. This development APK is not an official Draw Things release.

## Build

Requires JDK 17, Android SDK 37, NDK 28.2.13676358, and CMake 3.22.1.
Set `sdk.dir` in untracked `local.properties`, or set `ANDROID_HOME`.

```sh
cd android
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :protocol:test
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Application ID:
`com.ryderdavid.drawremote`; minimum Android 9 (API 28). Debug builds are debuggable;
use a separately reviewed and signed release before treating this as production storage.

Committed schema bindings build without flatc. To regenerate, install **flatc 25.2.10**
from the official Google FlatBuffers release and run `python3 android/tools/generate_protocol.py`
from repository root. The generator checks that version and reads the upstream proto/FBS
files in this checkout. Gradle generates protobuf-lite bindings during the build.

## Connect

Enable API Server in the real Draw Things Mac app, with TLS and Model Browsing on and
Bridge Mode off. Enter its reachable hostname/IP and port (usually 7859). For a private
certificate, independently verify its SHA-256 fingerprint on the Mac, then enter the
pin. Enter a shared secret if configured. Never accept an unverified fingerprint.
LAN and an existing device-managed Tailscale route use the same connection screen.
There is no VPN management, cloud compute choice, or local generation mode.

Connection verification is mandatory before first use. Saved private projects can be
viewed offline afterward, but generation requires a fresh connection. Results, metadata,
credentials, drafts, inputs, and masks use the private encrypted vault. No Gallery export,
MediaStore insertion, external file provider, or media permission is included. Backup and
device transfer are disabled. Uninstalling removes access to the device-local vault.
Mac-side retention is controlled by Draw Things itself.

## Tests

Install both APKs with `adb install -r`, then run (add `-s SERIAL` for multiple devices):

```sh
adb shell am instrument -w -r -e class com.ryderdavid.drawremote.VaultDeviceTest,com.ryderdavid.drawremote.CodecDeviceTest com.ryderdavid.drawremote.test/androidx.test.runner.AndroidJUnitRunner
```

The opt-in `LiveMacTest` requires instrumentation arguments `macHost`, `macPin`, and
optionally `macPort`. With `macModel` set to an installed model filename it **runs a real
512 × 512 generation on the Mac**, persists the output, verifies encrypted reopen, and
saves the test connection/draft in the app. Without `macModel` it only checks inventory.
Never put shared-secret values on command lines. Ordinary tests do not start generation.
Avoid `connectedDebugAndroidTest` on a device whose existing vault must be retained:
the Gradle runner may uninstall the app. Use direct instrumentation with installed APKs.

The bounded Accessibility helper in `tools/mac_accessibility.py` is a macOS development
utility, not part of the Android app; use only with authorization to change Mac settings.

## Third-party code

`app/src/main/cpp/fpzip` is LLNL fpzip 1.3.0, commit
`4a539c06d98b1c029b08324a086d4b75689a2b72`, from https://github.com/LLNL/fpzip.
Its LICENSE and NOTICE are retained. The Android target builds the reader only.
Local modifications reject EOF on the bounded memory-backed FILE reader and use RAII
for decoder allocations when malformed input throws. JNI validates dimensions/size,
serializes global codec state, and converts decoder exceptions to Kotlin errors.
Protocol definitions and generated schema derive from the retained Draw Things upstream
source and retain that repository's license. See the build files for other dependencies.
