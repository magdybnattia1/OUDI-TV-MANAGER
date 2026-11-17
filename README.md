# OUDI TV Manager 0.3.0 — ADB integration milestone

Android companion project for a Tornado Android TV (reported Android 10).

## Current implementation
- Arabic Compose UI and local-network discovery for legacy ADB TCP port 5555.
- Adds the `com.tananaev:adblib:1.3` ADB protocol library dependency. This is a real ADB host protocol implementation (not just a TCP port probe).
- The current UI still needs the ADB authentication/key lifecycle and command workflow wired and tested on the actual Tornado TV before it can claim authenticated shell access or change the HOME app.

## Important
A port responding on 5555 is not proof of ADB authorization. The TV must have network ADB enabled and accept the app's ADB host key. Android 10 normally uses legacy TCP ADB rather than Android 11+ Wireless Debugging pairing.

No destructive commands are run. The stock launcher must not be disabled until the OUDI launcher is installed, verified as a HOME activity, and a rollback path is tested on this exact firmware.

## Build
GitHub Actions builds a debug APK on each push to main. Download it from the workflow run's Artifacts section. The APK is a development build and must be tested on a real device.