#!/usr/bin/env bash
# Drives the debug edition on an emulator and saves real screenshots to
# promo-screenshots/. Demo data (a streak, journal notes, reminder times) is
# written to the app's own SharedPreferences, the way the app stores it.
set -uo pipefail

PKG=com.myhitbodedut.debug
APK=android/app/build/outputs/apk/full/debug/app-full-debug.apk
OUT=promo-screenshots
mkdir -p "$OUT"

adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
sleep 5

# A Thursday morning, so the week already holds a few days done. Set first,
# then let the system settle: a clock jump mid-run froze System UI (run 3).
adb root >/dev/null 2>&1 || true
sleep 3
adb wait-for-device
adb shell settings put global auto_time 0
adb shell date 100106422026.00 || true
adb shell date
adb shell settings put global hide_error_dialogs 1
sleep 20
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null || true

# A 360 x 780 dp screen (1080 x 2340 px at 480 dpi), dark, in English.
adb shell wm size 1080x2340
adb shell wm density 480
adb shell cmd uimode night yes
# No animations: uiautomator can't read a screen that never stops moving (the
# bravo dances forever), and a still is what we want anyway.
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0


adb install -r -g "$APK"
adb shell cmd locale set-app-locales "$PKG" --locales en-US || true

# Everything the app asks for, so no warning shows.
adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS || true
adb shell appops set "$PKG" SYSTEM_ALERT_WINDOW allow
adb shell appops set "$PKG" GET_USAGE_STATS allow
adb shell dumpsys deviceidle whitelist +"$PKG"

# A clean status bar: 6:42, full battery, no notifications.
adb shell settings put global sysui_demo_allowed 1
demo() { adb shell am broadcast -a com.android.systemui.demo -e command "$@" >/dev/null; }
demo enter
demo clock -e hhmm 0642
demo battery -e level 100 -e plugged false
demo network -e wifi show -e level 4 -e fully true
demo network -e mobile show -e datatype none -e level 4 -e fully true
demo notifications -e visible false

# --- Demo data -------------------------------------------------------------
python3 - <<'PY'
import datetime, json, html, subprocess
out = subprocess.run(["adb", "shell", "date", "+%Y-%m-%d"], capture_output=True, text=True).stdout.strip()
today = datetime.date.fromisoformat(out)
notes = [
    "Thanked Him for my family. Asked for patience at work.",
    "Hard to start. Then the words came.",
    "",
    "Walked outside, spoke out loud. Felt close.",
]
minutes = [20, 35, 15, 60, 20, 25, 20, 30, 20, 45, 20, 25]
days = {}
d = today - datetime.timedelta(days=1)
recent = 0
# Twelve days in a row up to yesterday (Saturdays are rest days), then older
# sessions after a gap, as a real journal would have.
while recent < 12:
    if d.weekday() != 5:
        days[d.isoformat()] = {"minutes": minutes[recent], "note": notes[recent] if recent < len(notes) else ""}
        recent += 1
    d -= datetime.timedelta(days=1)
d -= datetime.timedelta(days=3)
for i in range(26):
    if d.weekday() != 5:
        days[d.isoformat()] = {"minutes": [20, 30, 45, 20, 60, 25][i % 6], "note": ""}
    d -= datetime.timedelta(days=1)

def prefs(entries):
    return "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n" + "\n".join(entries) + "\n</map>\n"

main = [
    '<int name="onboarded_version" value="2" />',
    '<int name="target_minutes" value="20" />',
    '<boolean name="reminder_on" value="true" />',
    '<string name="reminder_times">405,810,1335</string>',
    '<int name="reminder_minutes" value="405" />',
    '<boolean name="guided_hitbodedut" value="true" />',
    '<boolean name="enabled" value="true" />',
    '<set name="blocked_apps"><string>com.android.chrome</string></set>',
    '<boolean name="music_enabled" value="true" />',
    '<string name="music_track">yanuka_nigoun_3</string>',
]
def journal(d):
    return prefs(['<string name="days">' + html.escape(json.dumps(d)) + '</string>'])
open("/tmp/myhitbodedut.xml", "w").write(prefs(main))
open("/tmp/journal.xml", "w").write(journal(days))
with_today = dict(days)
with_today[today.isoformat()] = {"minutes": 20, "note": ""}
open("/tmp/journal_today.xml", "w").write(journal(with_today))
open("/tmp/celebrate.xml", "w").write(prefs(main + ['<int name="celebration_minutes" value="20" />']))
PY

put_prefs() {  # file name in the app, local file
  adb shell "run-as $PKG sh -c 'mkdir -p shared_prefs && cat > shared_prefs/$1'" < "$2"
}
put_prefs myhitbodedut.xml /tmp/myhitbodedut.xml
put_prefs myhitbodedut-journal.xml /tmp/journal.xml

shot() {
  sleep "${2:-3}"
  adb exec-out screencap -p > "$OUT/$1.png"
  echo "captured $1"
}

# Taps the element whose text (or description) is exactly $1, scrolling down
# up to three times if it is below the fold.
tap() {
  for attempt in 1 2 3 4; do
    rm -f /tmp/ui.xml
    adb shell rm -f /sdcard/ui.xml
    adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
    adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
    if [ -f /tmp/ui.xml ] && python3 - "$1" <<'PY'
import re, sys, subprocess, xml.etree.ElementTree as ET
want = sys.argv[1]
root = ET.parse("/tmp/ui.xml").getroot()
def centre(n):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
    return str((x1 + x2) // 2), str((y1 + y2) // 2)
# A "not responding" dialog: wait it out, then look again.
for n in root.iter("node"):
    if n.get("text") == "Wait" and n.get("package") == "android":
        subprocess.run(["adb", "shell", "input", "tap", *centre(n)], check=True)
        sys.exit(1)
for n in root.iter("node"):
    if n.get("text") == want or n.get("content-desc") == want:
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
        subprocess.run(["adb", "shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2)], check=True)
        sys.exit(0)
sys.exit(1)
PY
    then return 0; fi
    adb shell input swipe 540 1700 540 900 300
    sleep 1
  done
  echo "not found on screen: $1" >&2
  adb exec-out screencap -p > "$OUT/debug-missing-${1// /_}.png"
  return 1
}

launch() {
  adb shell am force-stop "$PKG"
  adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null
}

# 1. Home, before today's session.
launch
shot 01-home-start 6

# 2. Reminders (Texts tab, then Reminders).
tap "Texts" && sleep 2
tap "Reminders" && shot 02-reminders 4
adb shell input keyevent KEYCODE_BACK; sleep 1

# 3. Journal tab.
tap "Journal" && shot 03-journal
adb shell input keyevent KEYCODE_BACK; sleep 1

# 4. A running session, with the guided hitbodedut card.
tap "Today"; sleep 2
tap "Start a 20 min session"; sleep 3
shot 04-session-idle 1
tap "Start, 20 min" && shot 05-session-running 8

# 5. The bravo after a session, then home with today done.
adb shell am force-stop "$PKG"
put_prefs myhitbodedut.xml /tmp/celebrate.xml
put_prefs myhitbodedut-journal.xml /tmp/journal_today.xml
launch
shot 06-celebration 5
tap "Continue" && shot 07-home-done 4

# A look at what the screen held, for debugging a missed tap.
adb shell uiautomator dump /sdcard/ui.xml >/dev/null && adb pull /sdcard/ui.xml "$OUT/last-ui.xml" >/dev/null

adb shell wm size reset
adb shell wm density reset
ls -la "$OUT"
