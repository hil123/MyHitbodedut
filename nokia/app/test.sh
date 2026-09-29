#!/usr/bin/env bash
# Les tests de l'application Java : règles (JDK ordinaire), textes (Node),
# puis, si MicroEmulator et xvfb-run sont là, un parcours complet à
# l'écran 240×320 avec des captures dans nokia/app/build/captures.
#
#   nokia/app/build.sh && nokia/app/test.sh
set -euo pipefail
ICI="$(cd "$(dirname "$0")" && pwd)"
RACINE="$(cd "$ICI/../.." && pwd)"
JDK8="${JDK8:-/usr/lib/jvm/java-8-openjdk-amd64}"
OUTILS="$ICI/outils"
API="$OUTILS/cldcapi11-2.0.4.jar:$OUTILS/midpapi20-2.0.4.jar"

node --test "$RACINE"/nokia/test/*.test.mjs

# Les règles, compilées avec le code de l'application.
T="$ICI/build/tests"
rm -rf "$T" && mkdir -p "$T"
"$JDK8/bin/javac" -nowarn -encoding UTF-8 -source 1.3 -target 1.3 -bootclasspath "$API" -d "$T" \
  $(find "$ICI/src" -name '*.java') 2>/dev/null
"$JDK8/bin/javac" -nowarn -encoding UTF-8 -cp "$T:$API" -d "$T" "$ICI/test/TestsRegles.java"
"$JDK8/bin/java" -cp "$T:$API" myhitbodedut.TestsRegles

# Le parcours à l'écran.
EMU="$OUTILS/microemulator-2.0.4.jar"
if [ ! -f "$EMU" ]; then
  curl -sSfL -o "$EMU" https://repo1.maven.org/maven2/org/microemu/microemulator/2.0.4/microemulator-2.0.4.jar || true
fi
if [ -f "$EMU" ] && command -v xvfb-run >/dev/null; then
  P="$ICI/build/pilote"
  mkdir -p "$P"
  "$JDK8/bin/javac" -nowarn -encoding UTF-8 -cp "$EMU" -d "$P" "$ICI/test/Pilote.java"
  PARCOURS="ok attente:800 capture:01-accueil ok capture:02-durees ok capture:03-pour-commencer ok attente:2500 capture:04-decompte ok capture:05-pause gauche capture:06-menu ok capture:07-bloque gauche capture:08-retour-decompte gauche bas ok capture:09-arret ok capture:10-accueil bas ok capture:11-moments bas capture:12-moment-suivant ok capture:13-liste ok capture:14-texte bas capture:15-texte-defile ok capture:16-autre-texte gauche gauche gauche bas ok capture:17-journal"
  (cd "$ICI/test" && timeout 120 xvfb-run -a "$JDK8/bin/java" -Djavax.accessibility.assistive_technologies=" " \
    -cp "$EMU:$P:." Pilote "$RACINE/nokia/dist/MyHitbodedut.jar" "$ICI/build/captures" "$PARCOURS")
  n=$(ls "$ICI/build/captures" | wc -l)
  [ "$n" -eq 17 ] || { echo "Parcours incomplet : $n captures sur 17" >&2; exit 1; }
  # Chaque touche de ces étapes doit changer l'écran : sinon, l'application n'a pas suivi.
  empreinte() { md5sum "$ICI/build/captures/$1.png" | cut -d' ' -f1; }
  for paire in 01-accueil:02-durees 02-durees:03-pour-commencer 03-pour-commencer:04-decompte 04-decompte:05-pause \
      06-menu:07-bloque 08-retour-decompte:09-arret 09-arret:10-accueil 10-accueil:11-moments 11-moments:12-moment-suivant \
      12-moment-suivant:13-liste 13-liste:14-texte 14-texte:15-texte-defile 15-texte-defile:16-autre-texte 16-autre-texte:17-journal; do
    [ "$(empreinte "${paire%%:*}")" != "$(empreinte "${paire##*:}")" ] || { echo "Écran inchangé entre ${paire%%:*} et ${paire##*:}" >&2; exit 1; }
  done
  echo "Parcours à l'écran : 17 captures dans nokia/app/build/captures"
else
  echo "MicroEmulator ou xvfb-run absent : parcours à l'écran non joué."
fi
