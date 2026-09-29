#!/usr/bin/env bash
# Construit MyHitbodedut.jar, l'application Java ME (MIDP 2.0, CLDC 1.1)
# pour téléphone à touches : Nokia 3310 3G de 2017 et téléphones Java.
#
#   nokia/app/build.sh
#
# Il faut : Node, un JDK 8 (javac sait encore viser Java 1.3, ce qu'exige
# CLDC), et Java 11 ou plus pour ProGuard. Les API MIDP/CLDC et ProGuard
# sont téléchargées une fois dans nokia/app/outils (ignoré par git).
#
# ProGuard fait la « pré-vérification » que CLDC exige : sans elle, le
# téléphone refuse d'installer le .jar.
set -euo pipefail

ICI="$(cd "$(dirname "$0")" && pwd)"
RACINE="$(cd "$ICI/../.." && pwd)"
OUTILS="$ICI/outils"
TRAVAIL="$ICI/build"
DIST="$RACINE/nokia/dist"
JDK8="${JDK8:-/usr/lib/jvm/java-8-openjdk-amd64}"
JAVA_RECENT="${JAVA_RECENT:-java}"
MAVEN=https://repo1.maven.org/maven2

telecharge() { # chemin maven, fichier
  [ -f "$OUTILS/$2" ] && return
  mkdir -p "$OUTILS"
  for essai in 1 2 3 4; do
    curl -sSfL -o "$OUTILS/$2.part" "$MAVEN/$1/$2" && mv "$OUTILS/$2.part" "$OUTILS/$2" && return
    sleep $((essai * 3))
  done
  echo "Téléchargement impossible : $2" >&2; exit 1
}
telecharge org/microemu/cldcapi11/2.0.4 cldcapi11-2.0.4.jar
telecharge org/microemu/midpapi20/2.0.4 midpapi20-2.0.4.jar
telecharge com/guardsquare/proguard-base/7.6.1 proguard-base-7.6.1.jar
telecharge com/guardsquare/proguard-core/9.1.9 proguard-core-9.1.9.jar
telecharge org/apache/logging/log4j/log4j-api/2.24.3 log4j-api-2.24.3.jar
telecharge org/apache/logging/log4j/log4j-core/2.24.3 log4j-core-2.24.3.jar
telecharge org/jetbrains/kotlin/kotlin-stdlib/2.1.0 kotlin-stdlib-2.1.0.jar
PROGUARD="$OUTILS/proguard-base-7.6.1.jar:$OUTILS/proguard-core-9.1.9.jar:$OUTILS/log4j-api-2.24.3.jar:$OUTILS/log4j-core-2.24.3.jar:$OUTILS/kotlin-stdlib-2.1.0.jar"
API="$OUTILS/cldcapi11-2.0.4.jar:$OUTILS/midpapi20-2.0.4.jar"

rm -rf "$TRAVAIL" && mkdir -p "$TRAVAIL/classes" "$TRAVAIL/res" "$DIST"

# 1. Les textes, depuis les ressources Android.
node "$ICI/genere-textes.mjs" "$TRAVAIL/res/textes.bin"
cp "$ICI/res/icone.png" "$TRAVAIL/res/"

# 2. Compilation contre les seules API du téléphone.
"$JDK8/bin/javac" -nowarn -source 1.3 -target 1.3 -encoding UTF-8 \
  -bootclasspath "$API" -d "$TRAVAIL/classes" $(find "$ICI/src" -name '*.java')

# 3. Le manifeste : ce que le téléphone lit avant d'installer.
VERSION="1.0.$(git -C "$RACINE" rev-list --count HEAD 2>/dev/null || echo 0)"
cat > "$TRAVAIL/MANIFEST.MF" <<MF
Manifest-Version: 1.0
MIDlet-Name: MyHitbodedut
MIDlet-Vendor: MyHitbodedut
MIDlet-Version: $VERSION
MIDlet-Icon: /icone.png
MIDlet-1: MyHitbodedut, /icone.png, myhitbodedut.MyHitbodedut
MIDlet-Permissions-Opt: javax.microedition.io.PushRegistry
MicroEdition-Profile: MIDP-2.0
MicroEdition-Configuration: CLDC-1.1
MF
"$JDK8/bin/jar" cfm "$TRAVAIL/brut.jar" "$TRAVAIL/MANIFEST.MF" -C "$TRAVAIL/classes" . -C "$TRAVAIL/res" .

# 4. Pré-vérification (et allègement) par ProGuard.
"$JAVA_RECENT" -cp "$PROGUARD" proguard.ProGuard \
  -injars "$TRAVAIL/brut.jar" -outjars "$DIST/MyHitbodedut.jar" \
  -libraryjars "$OUTILS/cldcapi11-2.0.4.jar" -libraryjars "$OUTILS/midpapi20-2.0.4.jar" \
  -microedition -dontoptimize -dontnote -dontwarn java.lang.invoke.** \
  -keep 'public class * extends javax.microedition.midlet.MIDlet' \
  -keepattributes ''

echo "MyHitbodedut.jar $VERSION : $(stat -c %s "$DIST/MyHitbodedut.jar") octets"
