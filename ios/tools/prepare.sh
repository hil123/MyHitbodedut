#!/usr/bin/env bash
# Prépare les ressources de l'application iOS, sans rien recopier à la main :
#   1. construit la version web (web/build.mjs), qui relit déjà les ressources Android ;
#   2. empaquette le moteur de calcul (ios/moteur/entree.mjs) pour JavaScriptCore ;
#   3. le vérifie contre les vecteurs KosherJava ;
#   4. copie dans ios/MyHitbodedut/Genere/ les données, les illustrations et le moteur.
# Genere/ n'est pas versionné : il se refait à chaque construction.
#
# La musique n'est pas copiée : le 29/10, seul המלך בשדה (libre de droits) est
# retenu, les autres morceaux seront remplacés par des morceaux autorisés par écrit.
set -euo pipefail
ICI="$(cd "$(dirname "$0")/.." && pwd)"
RACINE="$(cd "$ICI/.." && pwd)"
SORTIE="$ICI/MyHitbodedut/Genere"

node "$RACINE/web/build.mjs"
(cd "$ICI" && npm install --no-audit --no-fund --silent)

rm -rf "$SORTIE"
mkdir -p "$SORTIE/data" "$SORTIE/img" "$SORTIE/musique"
(cd "$ICI" && npx esbuild moteur/entree.mjs --bundle --format=iife --target=es2020 --log-level=warning --outfile="$SORTIE/Moteur.js")
node "$ICI/tools/verifie_moteur.mjs" "$SORTIE/Moteur.js"

cp "$RACINE"/web/dist/data/*.json "$SORTIE/data/"
cp "$RACINE"/web/dist/img/* "$SORTIE/img/"
cp "$RACINE/web/dist/musique/hamelech_basade.mp3" "$SORTIE/musique/"
cp "$RACINE/web/dist/version.txt" "$SORTIE/version.txt"
echo "Ressources iOS : $(du -sh "$SORTIE" | cut -f1) dans $SORTIE"
