#!/usr/bin/env bash
# Lance le parcours de captures sur l'emulateur deja demarre (job « captures »
# de android.yml). Installe l'APK de test, puis laisse captures.py naviguer.
# Ne fait jamais echouer le job : une capture manquante se voit dans le
# journal, pas par une croix rouge.
set -u
cd "$(dirname "$0")/../.."
mkdir -p out
export CAPTURES_OUT="$PWD/out"

APK=$(ls android/app/build/outputs/apk/full/debug/*.apk 2>/dev/null | head -1)
echo "APK : ${APK:-introuvable}"
adb wait-for-device
adb shell getprop ro.build.version.sdk
adb shell settings put global window_animation_scale 0 || true
adb shell settings put global transition_animation_scale 0 || true
adb shell settings put global animator_duration_scale 0 || true
# Pas de verrou d'ecran ni de veille pendant le parcours.
adb shell svc power stayon true || true
adb shell input keyevent KEYCODE_WAKEUP || true
adb shell wm dismiss-keyguard || true
# Barre d'etat propre (mode demo) : heure fixe, batterie pleine.
adb shell settings put global sysui_demo_allowed 1 || true
adb shell am broadcast -a com.android.systemui.demo -e command enter || true
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0930 || true
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false || true
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false || true

# Le lanceur Pixel de l'emulateur ne repondait plus (ANR) et sa fenetre
# couvrait toutes les captures : pas de fenetres d'erreur, pas de lanceur.
adb shell settings put global hide_error_dialogs 1 || true
# Pas de bulle « Viewing full screen » par-dessus l'ecran de seance (19/10).
adb shell settings put secure immersive_mode_confirmations confirmed || true
adb shell pm disable-user --user 0 com.google.android.apps.nexuslauncher || true
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS || true

if [ -n "${APK:-}" ]; then
  adb install -r -g "$APK" || adb install -r "$APK" || echo "installation echouee"
fi

python3 .github/captures/captures.py || true
ls -la out || true
exit 0
