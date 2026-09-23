#!/usr/bin/env bash
# Runs inside the booted emulator job. Installs the released APK untouched,
# sets English and the dark theme before the first launch, then drives it.
set -x
OUT=video-assets/capture
PKG=com.myhitbodedut
adb root || true; sleep 3; adb wait-for-device
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell cmd uimode night yes
adb install -r -g apk/MyHitbodedut.apk
adb shell cmd locale set-app-locales $PKG --locales en-US || true
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow || true
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
adb shell input keyevent 3
python3 tools/capture/drive_app.py $PKG $OUT ""
adb shell input keyevent 3; sleep 2
adb exec-out screencap -p > $OUT/99-launcher.png
exit 0
