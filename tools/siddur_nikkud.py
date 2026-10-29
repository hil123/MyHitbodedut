"""Corrige les fautes de saisie du nikkoud dans les trois siddourim.

Signe doublé (הֶבֶֶל), dagesh mal placé, holam saisi pour le point du sin,
marque LRM collée dans les mots du Tahanoun ashkenaze (elle détache le
meteg de sa lettre). Les graphies voulues restent : יְהוָֹה, יְרוּשָׁלִָם,
שׁוֶֹה. Remplacement dans le texte brut : le format du fichier ne bouge
pas, et une seconde passe ne change rien.
"""
import pathlib

RAW = pathlib.Path(__file__).resolve().parent.parent / "android/app/src/main/res/raw"

CORRECTIONS = [
    ("הֶבֶֶל", "הֶבֶל"),
    ("תִּפְאֶֶֽרֶת", "תִּפְאֶֽרֶת"),
    ("וְיִתְמְשָָׁךְ", "וְיִתְמְשָׁךְ"),
    ("מִבְטַחֽוֹֹ", "מִבְטַחֽוֹ"),
    ("שֶׁצִּּוִיתָֽנוּ", "שֶׁצִּוִּיתָֽנוּ"),
    ("וַַיִּתְיַצֵּב", "וַיִּתְיַצֵּב"),
    ("מַשְֹׁטִין", "מַשְׂטִין"),
    ("לֺֹא", "לֹא"),
    ("‎", ""),
]

for nusah in ("edot", "sefard", "ashkenaz"):
    f = RAW / f"siddur_{nusah}.json"
    texte = f.read_text(encoding="utf-8")
    avant = texte
    for faux, juste in CORRECTIONS:
        n = texte.count(faux)
        if n:
            print(f"{nusah}: {n} × {faux!r} → {juste!r}")
            texte = texte.replace(faux, juste)
    if texte != avant:
        f.write_text(texte, encoding="utf-8")
