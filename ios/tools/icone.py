"""L'icône App Store, 1024 × 1024, sans transparence.

Même composition que web/tools/icones.mjs et l'icône adaptative Android :
le visage (ic_launcher_face.webp) sur la menthe de colors.xml. La source ne
fait que 432 px : c'est un agrandissement, provisoire (décision du 29/10),
à remplacer par une icône dessinée à 1024 px avant la publication.

    python3 ios/tools/icone.py
"""
import re
from pathlib import Path

from PIL import Image

ICI = Path(__file__).resolve().parent
RES = ICI.parent.parent / "android" / "app" / "src" / "main" / "res"
SORTIE = ICI.parent / "MyHitbodedut" / "Assets.xcassets" / "AppIcon.appiconset" / "AppIcon-1024.png"

menthe = re.search(r'<color name="mint">#([0-9A-Fa-f]{6})<', (RES / "values" / "colors.xml").read_text()).group(1)
fond = Image.new("RGB", (1024, 1024), tuple(int(menthe[i:i + 2], 16) for i in (0, 2, 4)))
visage = Image.open(RES / "drawable-nodpi" / "ic_launcher_face.webp").convert("RGBA").resize((1024, 1024), Image.LANCZOS)
fond.paste(visage, (0, 0), visage)
fond.save(SORTIE, optimize=True)
print(f"{SORTIE.name} : 1024 × 1024, fond #{menthe}")
