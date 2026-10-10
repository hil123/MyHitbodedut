# -*- coding: utf-8 -*-
"""
Produit android/app/src/test/resources/zmanim_reference_noaa.json (10/10/2026) :
les heures selon les formules de la NOAA elle-même, pour vérifier que
KosherJava (NOAACalculator) et kosher-zmanim les suivent.

Source des formules : le calculateur solaire de la NOAA
(www.esrl.noaa.gov/gmd/grad/solcalc, aujourd'hui gml.noaa.gov), dans sa
traduction Python par Sébastien de Larquier, module calcSun du paquet PyPI
hamsci_psws 0.1.3 (licence GPL : utilisé ici pour produire des valeurs de
référence, jamais distribué avec l'application). Le site de la NOAA n'était
pas joignable depuis l'environnement de construction.

Deux écarts volontaires avec ce module :
  - le jour julien est calculé ici (formule de Meeus, ch. 7) : sa fonction
    getJD ne traite pas janvier et février correctement ;
  - l'angle horaire est généralisé à tout zénith (90,833° pour le lever et
    le coucher, 90 + n degrés pour l'aube et la nuit), comme le fait
    NOAACalculator ; calcHourAngleSunrise ne connaît que 90,833°.
Comme calcSunRiseSet, chaque événement est recalculé une fois à sa propre
heure (deux passes).

  pip install hamsci_psws==0.1.3 numpy python-dateutil
  python3 tools/zmanim_reference_noaa.py > android/app/src/test/resources/zmanim_reference_noaa.json
"""
import datetime
import json
import math
from zoneinfo import ZoneInfo

from hamsci_psws import calcSun as noaa

LIEUX = [("Jérusalem", 31.76904, 35.21633, "Asia/Jerusalem"), ("Paris", 48.85341, 2.3488, "Europe/Paris"),
         ("New York", 40.71427, -74.00597, "America/New_York"), ("Melbourne", -37.814, 144.96332, "Australia/Melbourne")]
DATES = ["2026-01-15", "2026-03-29", "2026-06-21", "2026-10-10", "2026-10-25", "2026-12-21", "2027-04-22"]


def jour_julien(a, m, j):
    """Jour julien à 0 h UT (Meeus, Astronomical Algorithms, ch. 7)."""
    if m <= 2:
        a, m = a - 1, m + 12
    A = a // 100
    B = 2 - A + A // 4
    return math.floor(365.25 * (a + 4716)) + math.floor(30.6001 * (m + 1)) + j + B - 1524.5


def evenement(jd, lat, lon, zenith, lever):
    """Minutes depuis 0 h UTC, formule du calculateur NOAA, pour un zénith donné."""
    def une_passe(jdx):
        t = noaa.calcTimeJulianCent(jdx)
        eq = noaa.calcEquationOfTime(t)
        dec = math.radians(noaa.calcSunDeclination(t))
        latr = math.radians(lat)
        arg = math.cos(math.radians(zenith)) / (math.cos(latr) * math.cos(dec)) - math.tan(latr) * math.tan(dec)
        if not -1 <= arg <= 1:
            return None
        ha = math.degrees(math.acos(arg)) * (1 if lever else -1)
        return 720 - 4 * (lon + ha) - eq
    premier = une_passe(jd)
    return None if premier is None else une_passe(jd + premier / 1440)


def midi(jd, lon):
    t = noaa.calcSolNoonUTC(jd, lon)
    return noaa.calcSolNoonUTC(jd + t / 1440, lon)


sortie = []
for nom, lat, lon, fuseau in LIEUX:
    for iso in DATES:
        a, m, j = map(int, iso.split("-"))
        jd = jour_julien(a, m, j)
        minuit = datetime.datetime(a, m, j, tzinfo=datetime.timezone.utc)
        heure = lambda minutes: None if minutes is None else \
            (minuit + datetime.timedelta(minutes=minutes)).astimezone(ZoneInfo(fuseau)).strftime("%H:%M:%S")
        sortie.append({
            "lieu": nom, "date": iso,
            "netz": heure(evenement(jd, lat, lon, 90.833, True)),
            "chkia": heure(evenement(jd, lat, lon, 90.833, False)),
            "alot": heure(evenement(jd, lat, lon, 106.1, True)),
            "misheyakir": heure(evenement(jd, lat, lon, 101.5, True)),
            "tset": heure(evenement(jd, lat, lon, 98.5, False)),
            "chatsot": heure(midi(jd, lon)),
        })
print(json.dumps(sortie, ensure_ascii=False, indent=0))
