#!/usr/bin/env bash
# Le niggoun d'essai de l'application Java, compressé pour un téléphone à
# touches : MP3 mono 24 kb/s (le format espéré) et AMR-NB 12,2 kb/s (le
# format que presque tous les téléphones à touches lisent). Les fichiers
# produits sont gardés dans le dépôt (nokia/app/res/m) : la construction du
# .jar n'a pas besoin de ffmpeg.
#
#   FFMPEG=/chemin/ffmpeg nokia/app/genere-musique.sh
set -euo pipefail
ICI="$(cd "$(dirname "$0")" && pwd)"
SRC="$ICI/../../android/app/src/main/res/raw/niggoun_33.mp3"
FFMPEG="${FFMPEG:-ffmpeg}"
mkdir -p "$ICI/res/m"
"$FFMPEG" -v error -y -i "$SRC" -map_metadata -1 -ac 1 -ar 22050 -c:a libmp3lame -b:a 24k "$ICI/res/m/niggoun.mp3"
"$FFMPEG" -v error -y -i "$SRC" -map_metadata -1 -ac 1 -ar 8000 -c:a libopencore_amrnb -b:a 12.2k "$ICI/res/m/niggoun.amr"
ls -la "$ICI/res/m"
