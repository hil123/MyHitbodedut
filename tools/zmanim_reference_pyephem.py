# -*- coding: utf-8 -*-
# Produit android/app/src/test/resources/zmanim_reference_pyephem.json (10/10/2026) :
# une référence astronomique indépendante de KosherJava et de kosher-zmanim, qui
# suivent tous deux l algorithme de la NOAA. PyEphem 4.2.1 (libastro, VSOP87) ne
# partage rien avec lui. Lancer : python3 tools/zmanim_reference_pyephem.py > fichier
# Référence astronomique indépendante (PyEphem, libastro/VSOP87) pour ZmanimTest.
# Lever et coucher : centre du soleil à 50 minutes d arc sous l horizon (16 de demi-diamètre + 34 de réfraction), pression nulle pour que PyEphem n ajoute pas sa propre réfraction.
import ephem, json, datetime
from zoneinfo import ZoneInfo
CAS=[("Jérusalem",31.76904,35.21633,"Asia/Jerusalem"),("Paris",48.85341,2.3488,"Europe/Paris"),("New York",40.71427,-74.00597,"America/New_York"),("Melbourne",-37.814,144.96332,"Australia/Melbourne")]
DATES=["2026-03-29","2026-06-21","2026-10-10","2026-10-25","2026-12-21"]
out=[]
for nom,lat,lon,tz in CAS:
  for d in DATES:
    y,m,dd=map(int,d.split('-'))
    z=ZoneInfo(tz)
    debut=datetime.datetime(y,m,dd,0,0,tzinfo=z).astimezone(datetime.timezone.utc)
    o=ephem.Observer(); o.lat=str(lat); o.lon=str(lon); o.elevation=0; o.pressure=0
    o.date=ephem.Date(debut.replace(tzinfo=None))
    s=ephem.Sun()
    def ev(h,centre,lever):
      o.horizon=h; o.date=ephem.Date(debut.replace(tzinfo=None))
      t=(o.next_rising if lever else o.next_setting)(s,use_center=centre)
      return ephem.Date(t).datetime().replace(tzinfo=datetime.timezone.utc).astimezone(z).strftime('%H:%M:%S')
    o.date=ephem.Date(debut.replace(tzinfo=None))
    transit=ephem.Date(o.next_transit(s)).datetime().replace(tzinfo=datetime.timezone.utc).astimezone(z).strftime('%H:%M:%S')
    out.append(dict(lieu=nom,date=d,netz=ev('-0:50',True,True),chkia=ev('-0:50',True,False),alot=ev('-16.1',True,True),misheyakir=ev('-11.5',True,True),tset=ev('-8.5',True,False),chatsot=transit))
print(json.dumps(out,ensure_ascii=False,indent=0))
