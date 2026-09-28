#!/usr/bin/env bash
# Le site dans Safari sur un iPhone simulé (Xcode), sur la machine macOS
# de la CI. Le simulateur partage le réseau du Mac : 127.0.0.1 est le Mac.
#
#   web/test/simulateur.sh <url du site> <dossier de sortie>
#
# Ce que le simulateur ne remplace pas : l'installation sur l'écran
# d'accueil, et la musique écran verrouillé. Cela reste à essayer sur un
# vrai iPhone.
set -euo pipefail
URL="$1"
SORTIE="$2"
mkdir -p "$SORTIE"

# Le plus récent des iPhone disponibles.
UDID=$(xcrun simctl list devices available -j | python3 -c '
import json, sys
appareils = [d for r, ds in json.load(sys.stdin)["devices"].items() if "iOS" in r for d in ds if d["name"].startswith("iPhone")]
print(appareils[-1]["udid"])')
NOM=$(xcrun simctl list devices available | grep "$UDID" | sed -E 's/^ *//; s/ \(.*//')
RUNTIME=$(xcrun simctl list devices available -j | python3 -c "
import json, sys
for r, ds in json.load(sys.stdin)['devices'].items():
    if any(d['udid'] == '$UDID' for d in ds): print(r.split('.')[-1])")
echo "Appareil : $NOM ($RUNTIME)" | tee "$SORTIE/iphone-rapport.txt"

xcrun simctl boot "$UDID" || true
xcrun simctl bootstatus "$UDID" -b

capture() {
  xcrun simctl openurl "$UDID" "$1"
  sleep "${3:-12}"
  xcrun simctl io "$UDID" screenshot "$SORTIE/iphone-$2.png"
  echo "Capture : iphone-$2.png ($1)" | tee -a "$SORTIE/iphone-rapport.txt"
}

# L'iPhone simulé est en anglais : l'application suit sa langue.
capture "$URL" accueil 20
capture "$URL#musique" musique
capture "$URL#reglages" reglages

# Le même téléphone passé en hébreu : l'application doit suivre, de droite à gauche.
xcrun simctl terminate "$UDID" com.apple.mobilesafari || true
xcrun simctl spawn "$UDID" defaults write -g AppleLanguages -array he en
xcrun simctl spawn "$UDID" defaults write -g AppleLocale -string he_IL
xcrun simctl shutdown "$UDID"
xcrun simctl boot "$UDID"
xcrun simctl bootstatus "$UDID" -b
capture "$URL#" accueil-he 20

xcrun simctl shutdown "$UDID" || true
