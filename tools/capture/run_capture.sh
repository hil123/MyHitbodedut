#!/usr/bin/env bash
# Runs inside the booted emulator job. Installs the released APK untouched,
# then drives it in English, light theme, then dark theme.
set -x
OUT=video-assets/capture
PKG=com.myhitbodedut
adb root || true; sleep 3; adb wait-for-device
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell "setprop persist.sys.locale en-US; setprop ctl.restart zygote" || true
sleep 20; adb wait-for-device
until adb shell getprop sys.boot_completed | grep -q 1; do sleep 2; done
sleep 5
adb install -r -g apk/MyHitbodedut.apk
adb shell cmd locale set-app-locales $PKG --locales en-US || true
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow || true
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
adb shell cmd uimode night no
adb shell input keyevent 3
MODE=full python3 tools/capture/drive_app.py $PKG $OUT ""
# Dark theme pass, content screens only
adb shell cmd uimode night yes
adb shell am force-stop $PKG
sleep 2
MODE=content python3 tools/capture/drive_app.py $PKG $OUT "-dark"
adb shell am start -n $PKG/.MainActivity; sleep 3
adb exec-out screencap -p > $OUT/99-home-dark.png
# Launcher icon as the system draws it
adb shell cmd uimode night no
adb shell input keyevent 3; sleep 2
adb exec-out screencap -p > $OUT/99-launcher.png
exit 0
