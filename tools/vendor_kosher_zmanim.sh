#!/bin/sh
# Regroupe kosher-zmanim 0.9.0 (LGPL-3.0), luxon et big.js (MIT) en un module
# ES séparé, servi tel quel par le site : web/src/vendor/kosher-zmanim-0.9.0.min.js.
# À lancer depuis la racine du dépôt. Le site n'a pas d'étape de regroupement ; ce fichier se refait à la main,
# seulement quand on change de version. Les licences sont dans
# web/src/vendor/LICENCES.txt.
set -e
T=$(mktemp -d)
cd "$T"
npm init -y >/dev/null
npm install --no-audit --no-fund kosher-zmanim@0.9.0 >/dev/null
echo 'export { ComplexZmanimCalendar, GeoLocation, JewishCalendar, JewishDate, HebrewDateFormatter, Calendar, DateTime } from "kosher-zmanim";' > entree.mjs
npx --yes esbuild@0.25.10 entree.mjs --bundle --format=esm --minify --legal-comments=inline \
  --outfile="$OLDPWD/web/src/vendor/kosher-zmanim-0.9.0.min.js"
