#!/usr/bin/env bash
# Compile les deux éditions signées et publie la release, depuis un GitHub
# Codespace, quand GitHub Actions ne démarre plus (quota de minutes épuisé,
# 11/10/2026). Mêmes étapes que .github/workflows/android.yml (build) :
# JDK 17, plateforme Android 35, build-tools 35.0.0, signature par la clé
# du dépôt (android/keystore/myhitbodedut-dev.jks, comme la CI).
#
#   bash tools/apk_codespace.sh            # numéro de version : 224
#   bash tools/apk_codespace.sh 225        # ou celui qu'on veut
set -euo pipefail

NUM="${1:-224}"
RACINE="$(cd "$(dirname "$0")/.." && pwd)"
SDK="$HOME/android-sdk"

# Une seule fois : la release de ce numéro existe déjà, rien à faire.
if gh release view "v1.$NUM" >/dev/null 2>&1; then
  echo "La release v1.$NUM existe déjà : rien à faire."
  exit 0
fi

echo "== JDK 17"
if ! java -version 2>&1 | grep -q '"17'; then
  sudo apt-get update -qq && sudo apt-get install -y -qq openjdk-17-jdk-headless >/dev/null
  export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
  export PATH="$JAVA_HOME/bin:$PATH"
fi
java -version 2>&1 | head -1

echo "== SDK Android (dl.google.com)"
if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
  mkdir -p "$SDK/cmdline-tools"
  curl -sSL -o /tmp/cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  unzip -q -o /tmp/cmdline-tools.zip -d "$SDK/cmdline-tools"
  rm -rf "$SDK/cmdline-tools/latest"
  mv "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
fi
export ANDROID_HOME="$SDK" ANDROID_SDK_ROOT="$SDK"
yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null || true
"$SDK/cmdline-tools/latest/bin/sdkmanager" "platforms;android-35" "build-tools;35.0.0" >/dev/null

echo "== Compilation (version $NUM)"
cd "$RACINE/android"
VERSION_CODE="$NUM" ./gradlew --no-daemon assembleFullRelease assembleLightRelease
VERSION_CODE="$NUM" ./gradlew --no-daemon testFullDebugUnitTest

echo "== Release v1.$NUM"
cp app/build/outputs/apk/full/release/app-full-release.apk MyHitbodedut.apk
cp app/build/outputs/apk/light/release/app-light-release.apk MyHitbodedut-Light.apk
cp app/build/outputs/mapping/fullRelease/mapping.txt mapping-full.txt
SHA="$(git rev-parse HEAD)"
gh release create "v1.$NUM" MyHitbodedut.apk MyHitbodedut-Light.apk mapping-full.txt \
  --target "$SHA" \
  --title "MyHitbodedut 1.$NUM" \
  --notes "Deux editions signees, construites depuis le commit $SHA (Codespace, Actions indisponible). Light n'embarque pas la barriere du matin ni ses permissions." \
  --latest
rm -f MyHitbodedut.apk MyHitbodedut-Light.apk mapping-full.txt

echo
echo "Fait : https://github.com/hil123/MyHitbodedut/releases/latest/download/MyHitbodedut.apk"
