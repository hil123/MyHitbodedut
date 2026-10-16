#!/usr/bin/env python3
"""
Captures d'ecran reelles de l'application, sur l'emulateur du flux Android
(job « captures », declenche a la main avec captures=true).

Chaque etape est independante : un element introuvable donne une capture
de ce qui est a l'ecran, nommee « NN-nom-INTROUVABLE », et l'arbre
uiautomator de l'ecran est imprime dans le journal. Le job n'echoue jamais
a cause d'une etape.
"""
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

OUT = os.environ.get("CAPTURES_OUT", "out")
os.makedirs(OUT, exist_ok=True)

PKG = None
ROOT = False
PREFS = "shared_prefs/myhitbodedut.xml"


def log(*a):
    print(*a, flush=True)


def adb(*args, input=None, timeout=90, binary=False):
    try:
        r = subprocess.run(["adb", *args], input=input, capture_output=True, timeout=timeout)
    except subprocess.TimeoutExpired:
        log("  adb timeout:", " ".join(args))
        return b"" if binary else ""
    if binary:
        return r.stdout
    out = r.stdout.decode("utf-8", "replace")
    if r.returncode != 0:
        log("  adb", " ".join(args), "->", r.returncode, r.stderr.decode("utf-8", "replace").strip()[:400])
    return out


def sh(cmd, **kw):
    return adb("shell", cmd, **kw)


def pause(s):
    time.sleep(s)


# --- Ecran -----------------------------------------------------------------

def dump():
    """L'arbre de l'ecran, ou None. uiautomator echoue parfois sur un ecran
    qui ne se pose pas : on reessaie."""
    for essai in range(4):
        sh("rm -f /sdcard/ui.xml")
        sh("uiautomator dump /sdcard/ui.xml", timeout=40)
        brut = adb("exec-out", "cat", "/sdcard/ui.xml", binary=True)
        if brut and brut.strip().startswith(b"<?xml"):
            try:
                return ET.fromstring(brut)
            except ET.ParseError as e:
                log("  xml illisible:", e)
        pause(1.0)
    return None


def bornes(n):
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", n.get("bounds", ""))
    if not m:
        return None
    x1, y1, x2, y2 = map(int, m.groups())
    if x2 <= x1 or y2 <= y1:
        return None
    return x1, y1, x2, y2


def correspond(n, text=None, desc=None, rid=None, contains=False):
    def eq(valeur, attendu):
        if attendu is None:
            return True
        valeur = valeur or ""
        if contains:
            return attendu.lower() in valeur.lower()
        return valeur == attendu

    if rid is not None and not (n.get("resource-id", "") or "").endswith(":id/" + rid):
        return False
    if text is not None and not eq(n.get("text"), text):
        return False
    if desc is not None and not eq(n.get("content-desc"), desc):
        return False
    return True


def ecran_texte(racine):
    """L'ecran en clair pour le journal : une ligne par noeud parlant."""
    if racine is None:
        return "(aucun arbre)"
    lignes = []
    for n in racine.iter("node"):
        t, d, r = n.get("text", ""), n.get("content-desc", ""), n.get("resource-id", "")
        if t or d or r:
            lignes.append(f"  [{n.get('class','').split('.')[-1]}] id={r.split('/')[-1]!s} text={t!r} desc={d!r} {n.get('bounds')}"
                          + (" clickable" if n.get("clickable") == "true" else ""))
    return "\n".join(lignes)


def journal_ecran(pourquoi):
    racine = dump()
    log(f"---- ecran ({pourquoi}) ----")
    log(ecran_texte(racine))
    if racine is not None:
        log("---- xml brut ----")
        log(ET.tostring(racine, encoding="unicode")[:20000])
    log("---- fin ----")


def taille_ecran():
    m = re.search(r"(\d+)x(\d+)", sh("wm size"))
    return (int(m.group(1)), int(m.group(2))) if m else (1080, 2400)


W, H = 1080, 2400


def defiler(vers_le_bas=True):
    x = W // 2
    if vers_le_bas:
        sh(f"input swipe {x} {int(H*0.72)} {x} {int(H*0.32)} 350")
    else:
        sh(f"input swipe {x} {int(H*0.32)} {x} {int(H*0.72)} 350")
    pause(0.8)


def fermer_anr(racine):
    """Une fenetre « ... isn't responding » (lanceur de l'emulateur) : on touche « Wait »."""
    if racine is None:
        return racine
    if not any("responding" in (n.get("text") or "") or "ne répond pas" in (n.get("text") or "")
               for n in racine.iter("node")):
        return racine
    for n in racine.iter("node"):
        if (n.get("text") or "") in ("Wait", "Attendre", "Close app", "Fermer l'application") and bornes(n):
            b = bornes(n)
            log("  fenetre ANR fermee")
            sh(f"input tap {(b[0] + b[2]) // 2} {(b[1] + b[3]) // 2}")
            pause(1.0)
            return dump()
    return racine


def trouver(text=None, desc=None, rid=None, contains=False, defilements=0, index=0):
    for tour in range(defilements + 1):
        racine = fermer_anr(dump())
        if racine is not None:
            trouves = [n for n in racine.iter("node")
                       if correspond(n, text, desc, rid, contains) and bornes(n)]
            if len(trouves) > index:
                return bornes(trouves[index])
        if tour < defilements:
            defiler(True)
    return None


def toucher(text=None, desc=None, rid=None, contains=False, defilements=0, index=0, attente=1.5):
    quoi = " ".join(f"{k}={v!r}" for k, v in (("text", text), ("desc", desc), ("id", rid)) if v is not None)
    b = trouver(text, desc, rid, contains, defilements, index)
    if b is None:
        log(f"  INTROUVABLE : {quoi}")
        journal_ecran(f"recherche de {quoi}")
        return False
    x, y = (b[0] + b[2]) // 2, (b[1] + b[3]) // 2
    log(f"  toucher {quoi} -> ({x},{y})")
    sh(f"input tap {x} {y}")
    pause(attente)
    return True


def retour(n=1):
    for _ in range(n):
        sh("input keyevent KEYCODE_BACK")
        pause(1.0)


DENSITE = None


def accessibilite(racine, nom):
    """Audit d'accessibilite (16/10, point 9) : cibles touchables de moins de
    48 dp, et elements touchables sans texte ni description (muets pour
    TalkBack). Note dans accessibilite.txt, sans jamais faire echouer."""
    global DENSITE
    if racine is None:
        return
    if DENSITE is None:
        m = re.search(r"(\d+)\s*$", sh("wm density").strip())
        DENSITE = (int(m.group(1)) / 160.0) if m else 2.625
    lignes = []
    for n in racine.iter("node"):
        if n.get("clickable") != "true" and n.get("long-clickable") != "true" and n.get("checkable") != "true":
            continue
        b = bornes(n)
        if not b:
            continue
        l, h = (b[2] - b[0]) / DENSITE, (b[3] - b[1]) / DENSITE
        etiquette = (n.get("text") or n.get("content-desc") or "").strip()
        if not etiquette:
            etiquette = " ".join((c.get("text") or c.get("content-desc") or "").strip() for c in n.iter("node") if c is not n).strip()
        cls = n.get("class", "").rsplit(".", 1)[-1]
        rid = n.get("resource-id", "").rsplit("/", 1)[-1]
        if l < 47.5 or h < 47.5:
            lignes.append(f"PETIT {round(l)}x{round(h)}dp {cls} {rid} «{etiquette[:30]}»")
        if not etiquette:
            lignes.append(f"MUET {cls} {rid} {b}")
    with open(os.path.join(OUT, "accessibilite.txt"), "a") as f:
        f.write(f"== {nom}\n" + "".join(x + "\n" for x in lignes))


def capture(nom, attente=1.2, ok=True):
    pause(attente)
    try:
        racine = fermer_anr(dump())
        accessibilite(racine, nom)
    except Exception as e:
        log("  audit:", e)
    if not ok:
        nom += "-INTROUVABLE"
    chemin = os.path.join(OUT, nom + ".png")
    png = adb("exec-out", "screencap", "-p", binary=True)
    with open(chemin, "wb") as f:
        f.write(png)
    log(f"  capture {chemin} ({len(png)} octets)")


# --- Application -----------------------------------------------------------

def paquet():
    paquets = [l.split(":", 1)[1].strip() for l in sh("pm list packages myhitbodedut").splitlines() if ":" in l]
    log("paquets installes :", paquets)
    for p in ("com.myhitbodedut.debug", "com.myhitbodedut"):
        if p in paquets:
            return p
    return paquets[0] if paquets else "com.myhitbodedut.debug"


def arreter():
    sh(f"am force-stop {PKG}")
    pause(1.0)


def lancer(attente=4.0):
    sh(f"am start -W -n {PKG}/com.myhitbodedut.MainActivity", timeout=60)
    pause(attente)
    # Si la presentation est la malgre la preference, on la passe.
    if trouver(rid="onboarding_skip"):
        log("  presentation affichee : on la passe")
        toucher(rid="onboarding_skip")


def lire_prefs():
    return adb("exec-out", "run-as", PKG, "cat", PREFS, binary=True).decode("utf-8", "replace")


def ecrire_prefs(contenu):
    sh(f"run-as {PKG} mkdir -p shared_prefs")
    adb("shell", f"run-as {PKG} sh -c 'cat > {PREFS}'", input=contenu.encode("utf-8"))


def regler(valeurs):
    """Ecrit des preferences (application arretee). valeurs : nom -> (type, valeur)."""
    arreter()
    xml = lire_prefs()
    if "<map" not in xml:
        xml = "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n</map>\n"
    for nom, (typ, val) in valeurs.items():
        xml = re.sub(rf'\s*<[^>]+name="{re.escape(nom)}"[^>]*/>', "", xml)
        xml = re.sub(rf'\s*<string name="{re.escape(nom)}">.*?</string>', "", xml, flags=re.S)
        if typ == "string":
            ligne = f'    <string name="{nom}">{val}</string>'
        else:
            ligne = f'    <{typ} name="{nom}" value="{val}" />'
        xml = xml.replace("</map>", ligne + "\n</map>")
    ecrire_prefs(xml)
    log("  preferences :", lire_prefs().replace("\n", " ")[:600])


def langue(etiquette):
    arreter()
    sh(f"cmd locale set-app-locales {PKG} --locales {etiquette}")
    log("  langue :", sh(f"cmd locale get-app-locales {PKG}").strip())


def activite(classe, extras=""):
    """Ouvre une activite non exportee : il faut adb root (images google_apis)."""
    if not ROOT:
        return False
    sh(f"am start -W -n {PKG}/com.myhitbodedut.{classe} {extras}", timeout=60)
    pause(2.5)
    return True


def etape(titre, fonction):
    log(f"\n=== {titre} ===")
    try:
        fonction()
    except Exception as e:  # une etape ratee ne doit pas arreter les suivantes
        log(f"  ERREUR dans {titre} : {e!r}")
        try:
            journal_ecran("erreur")
            capture(re.sub(r"\W+", "-", titre).strip("-") + "-ERREUR", attente=0.2)
        except Exception:
            pass


def onglet_textes():
    ok = toucher(rid="nav_texts", attente=1.5)
    return ok


def ouvrir_siddour():
    lancer()
    onglet_textes()
    return toucher(rid="siddur_button", defilements=2, attente=2.0)


def ouvrir_priere(fr=True):
    if fr:
        ok = toucher(text="Cha’harit", defilements=3, attente=3.0)
        if not ok:
            ok = toucher(text="harit", contains=True, defilements=2, attente=3.0)
    else:
        ok = toucher(text="שַׁחֲרִית", contains=True, defilements=3, attente=3.0)
    if not ok:
        ok = activite("SiddurLectureActivity", "--es id chaharit")
    return ok


# --- Le parcours -----------------------------------------------------------

def main():
    global PKG, ROOT, W, H
    PKG = paquet()
    log("paquet :", PKG)
    W, H = taille_ecran()
    log("ecran :", W, H)
    r = adb("root")
    log("adb root :", r.strip())
    pause(3)
    adb("wait-for-device")
    ROOT = sh("id -u").strip() == "0"
    log("root :", ROOT)

    for perm in ("android.permission.POST_NOTIFICATIONS",):
        sh(f"pm grant {PKG} {perm}")
    # Pas de presentation au premier lancement (Store.KEY_ONBOARDED, Onboarding.VERSION = 2).
    regler({"onboarded_version": ("int", 2), "langue_app": ("string", "fr")})
    # Un premier lancement, puis la langue : posee avant, elle n'etait pas
    # gardee et l'application s'ouvrait en anglais.
    lancer()
    langue("fr")

    # Sur demande (CAPTURES_TAILLES_SEULES=1) : le seul passage des tailles,
    # deux fois plus court que le parcours entier.
    # L'ecran d'ouverture (15/10) : le 'hassid devant le tziyoun, saisi pendant sa
    # seconde et demie, au demarrage a froid.
    def s00():
        sh(f"am force-stop {PKG}")
        pause(1.0)
        sh(f"am start -n {PKG}/com.myhitbodedut.MainActivity")
        for i, t in enumerate((1.2, 1.3, 1.0)):
            pause(t)
            png = adb("exec-out", "screencap", "-p", binary=True)
            with open(os.path.join(OUT, f"00-ouverture-{i + 1}.png"), "wb") as f:
                f.write(png)
        pause(2.0)
    etape("00 ouverture", s00)

    if os.environ.get("CAPTURES_TAILLES_SEULES") == "1":
        etape("20 tailles d'ecran", lambda: tailles())
        return

    def s01():
        lancer(attente=6)
        capture("01-accueil")
        defiler(True)
        capture("01b-accueil-suite")
    etape("01 accueil", s01)

    def s02():
        ok = onglet_textes()
        defiler(False)
        capture("02-onglet-textes", ok=ok)
    etape("02 onglet textes", s02)

    def s03():
        ok = toucher(rid="siddur_button", defilements=2, attente=2.5)
        capture("03-siddour-liste", ok=ok)
        defiler(True)
        capture("03b-siddour-liste-suite", ok=ok)
        defiler(False)
        defiler(False)
    etape("03 siddour", s03)

    def s04():
        ok = toucher(rid="siddur_heading_nusah", attente=1.5)
        capture("04-choix-nusah", ok=ok)
        if ok:
            retour()
    etape("04 nusah", s04)

    def s05():
        ok = ouvrir_priere()
        capture("05-priere-pastille", attente=2.0, ok=ok)
    etape("05 priere", s05)

    def s06():
        ok = toucher(desc="Réglages de lecture", attente=1.2)
        capture("06-barre-depliee", ok=ok)
    etape("06 barre", s06)

    def s07():
        ok = toucher(desc="Lettres plus grandes", attente=0.8)
        ok = toucher(desc="Lettres plus grandes", attente=1.2) and ok
        capture("07-lettres-plus-grandes", ok=ok)
    etape("07 lettres", s07)

    def s08():
        ok = toucher(desc="Lecture sur fond noir", attente=1.5)
        capture("08-lecture-nuit", ok=ok)
    etape("08 nuit", s08)

    def s09():
        ok = toucher(desc="Masquer le bouton", attente=0.5)
        capture("09-bouton-masque-snackbar", attente=0.3, ok=ok)
    etape("09 masquer", s09)

    def s10():
        pause(4.5)  # la barre d'information s'en va
        capture("10a-texte-sans-bouton", attente=0.2)
        x, y = W // 2, int(H * 0.45)
        sh(f"input swipe {x} {y} {x} {y} 900")
        capture("10-appui-long-barre-revenue", attente=1.2)
    etape("10 appui long", s10)

    def remise_a_zero():
        # Lecture de jour, 100 %, bouton visible : les ecrans suivants
        # montrent l'etat ordinaire.
        regler({"lecture_sombre": ("boolean", "false"),
                "lecture_masquee": ("boolean", "false"),
                "lecture_echelle": ("float", "1.0")})
    etape("remise a zero de la lecture", remise_a_zero)

    def s11():
        lancer()
        onglet_textes()
        ok = toucher(rid="tehilim_button", defilements=2, attente=2.0)
        capture("11a-tehilim", ok=ok)
        ok2 = toucher(rid="tehilim_month_button", attente=3.0)
        if not ok2:
            ok2 = activite("TehilimLectureActivity", "--ei debut 1 --ei fin 1")
        capture("11-tehilim-lecture", ok=ok2)
    etape("11 tehilim", s11)

    def s12():
        lancer()
        onglet_textes()
        ok = toucher(rid="tikkun_button", defilements=2, attente=2.0)
        capture("12a-tikkun", ok=ok)
        ok2 = toucher(rid="tikkun_flow", defilements=1, attente=3.0)
        if not ok2:
            ok2 = activite("TikkunFlowActivity")
        capture("12-tikkun-d-un-trait", ok=ok2)
    etape("12 tikkun d'un trait", s12)

    def s13():
        lancer()
        onglet_textes()
        toucher(rid="tikkun_button", defilements=2, attente=2.0)
        ok = toucher(rid="psalm_title", defilements=3, attente=3.0)
        if not ok:
            ok = activite("PsalmActivity", "--ei psalm_numero 16")
        capture("13-tikkun-psaume", ok=ok)
    etape("13 psaume du tikkun", s13)

    def s14():
        lancer()
        onglet_textes()
        ok = toucher(rid="library_button", defilements=3, attente=2.0)
        capture("14a-bibliotheque", ok=ok)
        ok = toucher(rid="subsection_he", defilements=1, attente=2.0)
        capture("14b-bibliotheque-sous-section", ok=ok)
        ok = toucher(rid="entry_title", defilements=1, attente=2.5)
        if not ok:
            ok = activite("LibraryEntryActivity", "--es entry_id src-lm2-25")
        capture("14-bibliotheque-source", ok=ok)
    etape("14 bibliotheque", s14)

    def vitrine(nom):
        resolu = sh(f"cmd package resolve-activity --brief -n {PKG}/com.myhitbodedut.VitrineWidgetActivity")
        presente = "VitrineWidgetActivity" in sh(f"pm dump {PKG} | grep -i VitrineWidget")
        log("  vitrine :", presente, resolu.strip()[:200])
        if not presente:
            log("  VitrineWidgetActivity absente : etape sautee")
            return
        sh(f"am start -W -n {PKG}/com.myhitbodedut.VitrineWidgetActivity", timeout=60)
        capture(nom, attente=3.0)
        defiler(True)
        capture(nom + "-suite", attente=1.0)
        defiler(True)
        capture(nom + "-fin", attente=1.0)
    etape("17 vitrine des vignettes", lambda: vitrine("17-vitrine-vignettes"))

    def s15():
        lancer()
        ok = toucher(rid="nav_settings", attente=2.0)
        ok = ok and toucher(rid="accent_button", defilements=4, attente=1.5)
        capture("15a-choix-couleur", ok=ok)
        ok = ok and toucher(text="Violet", attente=2.5)
        capture("15b-reglages-violet", ok=ok)
        if not ok:
            regler({"apparence_accent": ("string", "violet")})
        ouvrir_siddour()
        capture("15c-siddour-violet")
        ok = ouvrir_priere()
        capture("15d-priere-pastille-violet", ok=ok)
        ok = toucher(desc="Réglages de lecture", attente=1.2)
        capture("15e-priere-barre-violet", ok=ok)
    etape("15 couleur violette", s15)

    etape("17b vitrine en violet", lambda: vitrine("17b-vitrine-vignettes-violet"))

    def s18():
        # Calendrier hébraïque (10/10) : un lieu connu, Paris, sans passer par le dialogue.
        regler({
            "lieu_nom": ("string", "Paris"), "lieu_latitude": ("string", "48.85341"),
            "lieu_longitude": ("string", "2.3488"), "lieu_altitude": ("string", "0.0"),
            "lieu_fuseau": ("string", "Europe/Paris"), "lieu_ville": ("long", "2988507"),
            "in_israel": ("boolean", "false"),
        })
        lancer()
        ok = toucher(rid="nav_zmanim", attente=2.5)
        capture("18a-zmanim", ok=ok)
        defiler(True)
        capture("18b-zmanim-horaires", ok=ok)
        defiler(True)
        capture("18c-zmanim-fin", ok=ok)
        ok2 = toucher(text="Calendrier", defilements=0, attente=2.5) or activite("CalendrierActivity")
        if not ok2:
            defiler(False)
            defiler(False)
            ok2 = toucher(text="Calendrier", attente=2.5)
        capture("18d-calendrier", ok=ok2)
        defiler(True)
        capture("18e-calendrier-jour", ok=ok2)
        # Le calendrier reste au-dessus de l'accueil : on en sort avant d'ouvrir le siddour.
        retour(2)
        ok4 = ouvrir_siddour()
        capture("18f-siddour-pour-cette-heure", ok=ok4)
        # Le soir de la CI (heure de Paris), c'est Arvit qui porte les ajouts du jour.
        ok3 = toucher(text="Arvit", defilements=3, attente=3.0) or activite("SiddurLectureActivity", "--es id arvit")
        for i in range(12):
            defiler(True)
        capture("18g-priere-ajouts", ok=ok3)
        for i in range(4):
            defiler(True)
        capture("18h-priere-ajouts-suite", ok=ok3)
    etape("18 zmanim, calendrier, ajouts", s18)

    def s19():
        # Les écrans du 11 au 14/10 : halakha (livre au choix), hilloulot, livres, siddour des occasions,
        # paliers, réglages (personnalisation, écran Zmanim). Lieu : Paris, réglé par s18.
        lancer()
        ok = activite("HalakhaActivity")
        capture("19a-halakha", ok=ok)
        defiler(True)
        capture("19b-halakha-suite", ok=ok)
        retour()
        ok = activite("HilloulotActivity")
        capture("19c-hilloulot", ok=ok)
        defiler(True)
        capture("19d-hilloulot-suite", ok=ok)
        retour()
        ok = activite("LibraryActivity", "--ez livres true")
        capture("19e-livres", ok=ok)
        retour()
        ok = ouvrir_siddour()
        for i in range(10):
            defiler(True)
        capture("19f-siddour-occasions", ok=ok)
        retour(2)
        ok = activite("PaliersActivity")
        capture("19g-paliers-trophees", ok=ok)
        retour()
        lancer()
        ok = toucher(rid="nav_settings", attente=2.5)
        capture("19h-reglages", ok=ok)
        for i in range(2):
            defiler(True)
        capture("19i-reglages-apparence", ok=ok)
        defiler(True)
        capture("19j-reglages-ecran-zmanim", ok=ok)
    etape("19 halakha, hilloulot, livres, occasions, reglages", s19)

    def s16():
        regler({"apparence_accent": ("string", "vert")})
        langue("he")
        lancer(attente=5)
        capture("16a-he-accueil")
        onglet_textes()
        capture("16b-he-onglet-textes")
        ok = toucher(rid="siddur_button", defilements=2, attente=2.5)
        capture("16c-he-siddour-liste", ok=ok)
        ok = ouvrir_priere(fr=False)
        capture("16d-he-priere-pastille", attente=1.5, ok=ok)
        ok = toucher(desc="הגדרות קריאה", attente=1.2)
        capture("16e-he-priere-barre-depliee", ok=ok)
        lancer()
        ok = toucher(rid="nav_zmanim", attente=2.5)
        capture("16f-he-zmanim", ok=ok)
        defiler(True)
        capture("16g-he-zmanim-suite", ok=ok)
        ok = activite("HalakhaActivity")
        capture("16h-he-halakha", ok=ok)
        ok = activite("HilloulotActivity")
        capture("16i-he-hilloulot", ok=ok)
    etape("16 passage en hebreu", s16)

    etape("20 tailles d'ecran", lambda: tailles())

    log("\ncaptures :", sorted(os.listdir(OUT)))


# --- Tailles d'ecran -----------------------------------------------------------
# Le parcours ci-dessus ne voit qu'un Pixel 6. La presentation s'est revelee
# illisible sur un 480x854 (titre, texte et boutons sous le bord) : chaque
# taille parcourt ici les sept ecrans de la presentation, en verifiant que
# titre, texte et « Suivant » se voient sans defiler, puis capture l'accueil,
# l'onglet des textes et la seance. Un ecran fautif est marque « HORS-ECRAN »
# dans son nom et compte dans le bilan du journal.

TAILLES = [
    # nom, taille physique forcee (ou None), densite (ou None), couche.
    # Les ecrans couches d'abord : ce sont ceux qui ont le plus change, et
    # l'emulateur finit parfois par se figer (« QEMU2 main loop ») sur la
    # grande tablette apres une demi-heure.
    ("petit-couche", "480x854", 240, True),
    ("tablette-couchee", "1600x2560", 320, True),
    # Taille forcee, comme les autres : apres un « wm size reset », le
    # Pixel 6 restait couche et ses captures debout sortaient en paysage.
    ("pixel6-couche", "1080x2400", 420, True),
    ("petit", "480x854", 240, False),
    # Le petit telephone avec le texte agrandi du systeme (15/10, « responsive
    # sur petite taille ») : la combinaison qui casse le plus de lignes.
    ("petit-gros-texte", "480x854", 240, False),
    ("pixel6", "1080x2400", 420, False),
    ("tablette", "1600x2560", 320, False),
]


def appliquer_taille(taille, densite, couche):
    global W, H
    sh("settings put system accelerometer_rotation 0")
    sh(f"settings put system user_rotation {1 if couche else 0}")
    sh("wm size " + (taille or "reset"))
    sh("wm density " + (str(densite) if densite else "reset"))
    pause(2.5)
    # Apres un ecran couche, le retour debout ne prenait pas toujours : les
    # captures « debout » du petit telephone et du Pixel 6 sortaient en
    # paysage. On regarde l'ecran lui-meme, et on insiste.
    for essai in range(4):
        png = adb("exec-out", "screencap", "-p", binary=True)
        if len(png) > 24 and png[:8] == b"\x89PNG\r\n\x1a\n":
            larg, haut = int.from_bytes(png[16:20], "big"), int.from_bytes(png[20:24], "big")
            if (larg > haut) == couche:
                break
            log(f"  orientation {larg}x{haut} inattendue, essai {essai + 1}")
        sh(f"settings put system user_rotation {0 if couche else 1}")
        pause(1.5)
        sh(f"settings put system user_rotation {1 if couche else 0}")
        pause(2.5)
    m = re.search(r"Override size: (\d+)x(\d+)", sh("wm size")) or re.search(r"(\d+)x(\d+)", sh("wm size"))
    w, h = (int(m.group(1)), int(m.group(2))) if m else (1080, 2400)
    W, H = (max(w, h), min(w, h)) if couche else (min(w, h), max(w, h))
    log(f"  ecran : {W}x{H}, densite {sh('wm density').strip()}")


FAUTES = []


def faute(texte):
    """Une faute de mise en page : dans le bilan, et tout de suite au journal."""
    FAUTES.append(texte)
    log("  FAUTE : " + texte)

# Chaque activite de l'application (AndroidManifest.xml), avec ce qu'il lui
# faut pour s'ouvrir sur un contenu reel.
ECRANS_APP = [
    ("SettingsActivity", ""),
    ("SessionActivity", ""),
    ("CelebrationActivity", "--ei minutes 20"),
    ("JournalActivity", ""),
    ("TipsActivity", ""),
    ("RemindersActivity", ""),
    ("MusicActivity", ""),
    ("SiddurActivity", ""),
    ("SiddurLectureActivity", "--es id chaharit"),
    ("TefilotActivity", ""),
    ("TehilimActivity", ""),
    ("TehilimLectureActivity", "--ei debut 1 --ei fin 1"),
    ("TikkunActivity", ""),
    ("TikkunFlowActivity", ""),
    ("PsalmActivity", "--ei psalm_numero 16"),
    ("LibraryActivity", ""),
    ("LibraryEntryActivity", "--es entry_id src-lm2-25"),
    ("ZmanimActivity", ""),
    ("CalendrierActivity", ""),
    ("HalakhaActivity", ""),
    ("HilloulotActivity", ""),
    ("RechercheActivity", ""),
    ("PaliersActivity", ""),
]


def serre(nom, ecran):
    """Les textes ecrases (une lettre par ligne) ou colles au bord de l'ecran."""
    racine = dump()
    if racine is None:
        return
    for n in racine.iter("node"):
        b = bornes(n)
        texte = (n.get("text", "") or "").strip()
        if not b or len(texte) < 6 or "TextView" not in n.get("class", "") and "Button" not in n.get("class", ""):
            continue
        larg = b[2] - b[0]
        if larg < W * 0.14 and len(texte) > 10:
            faute(f"{nom} {ecran} : texte ecrase ({larg}px) « {texte[:30]} »")
        elif b[0] <= 0 or b[2] >= W:
            faute(f"{nom} {ecran} : texte colle au bord {b} « {texte[:30]} »")


def visibles(racine, rids):
    """Les identifiants dont le noeud est entierement a l'ecran."""
    vus = set()
    if racine is None:
        return vus
    for n in racine.iter("node"):
        b = bornes(n)
        rid = (n.get("resource-id", "") or "").split("/")[-1]
        if rid in rids and b and b[3] <= H and b[1] >= 0 and (b[3] - b[1]) > 8:
            vus.add(rid)
    return vus


def presentation(nom, langue_app="fr"):
    regler({"onboarded_version": ("int", 0), "langue_app": ("string", langue_app)})
    langue(langue_app)
    sh(f"am start -W -n {PKG}/com.myhitbodedut.MainActivity", timeout=60)
    pause(4.0)
    for i in range(1, 8):
        racine = dump()
        voulus = {"onboarding_title", "onboarding_body", "onboarding_next"}
        vus = visibles(racine, voulus)
        manque = sorted(voulus - vus)
        suffixe = "" if not manque else "-HORS-ECRAN"
        if manque:
            faute(f"{nom} ecran {i} : {', '.join(manque)} hors de l'ecran")
        capture(f"20-{nom}-presentation-{i}{suffixe}", attente=0.3)
        if not toucher(rid="onboarding_next", attente=1.2):
            faute(f"{nom} ecran {i} : « Suivant » introuvable")
            break


def tailles():
    for nom, taille, densite, couche in TAILLES:
        log(f"\n--- taille {nom} ---")
        sh("settings put system font_scale " + ("1.3" if nom.endswith("gros-texte") else "1.0"))
        appliquer_taille(taille, densite, couche)
        presentation(nom)
        lancer()
        # « Commencer » doit finir au-dessus des onglets, pas seulement de l'ecran.
        racine = dump()
        bouton = onglets = None
        for n in (racine.iter("node") if racine is not None else []):
            rid = (n.get("resource-id", "") or "").split("/")[-1]
            if rid == "start_session":
                bouton = bornes(n)
            elif rid == "bottom_nav":
                onglets = bornes(n)
        # uiautomator rogne les bornes a la partie visible : un bouton coupe
        # par les onglets finit pile sur leur bord. On exige un peu d'air.
        limite = onglets[1] if onglets else H
        if not bouton or bouton[3] >= limite - 2 or bouton[1] < 0:
            faute(f"{nom} accueil : « Commencer » cache (bouton {bouton}, onglets a {limite})")
        serre(nom, "accueil")
        capture(f"20-{nom}-accueil")
        defiler(True)
        serre(nom, "accueil-suite")
        capture(f"20-{nom}-accueil-suite")
        onglet_textes()
        capture(f"20-{nom}-textes")
        # Tous les autres ecrans de l'application, ouverts directement. Sur
        # tablette, en passage court, seulement la seance et les reglages :
        # l'emulateur et le runner lachent apres une demi-heure de grandes
        # captures, et rien n'est alors publie.
        ecrans = ECRANS_APP
        if nom.startswith("tablette") and os.environ.get("CAPTURES_TAILLES_SEULES") == "1":
            ecrans = [e for e in ECRANS_APP if e[0] in ("SettingsActivity", "SessionActivity")]
        for classe, extras in ecrans:
            arreter()
            if activite(classe, extras):
                # Debout comme couchee, « Commencer » se voit sans defiler.
                if classe == "SessionActivity" and "session_primary" not in visibles(dump(), {"session_primary"}):
                    faute(f"{nom} seance : « Commencer » hors de l'ecran")
                serre(nom, classe)
                capture(f"20-{nom}-{classe.replace('Activity', '').lower()}", attente=0.8)
        if nom in ("petit", "petit-couche", "pixel6-couche"):
            presentation(nom + "-he", "he")
    sh("settings put system font_scale 1.0")
    appliquer_taille(None, None, False)
    regler({"langue_app": ("string", "fr")})
    langue("fr")
    log("\n=== bilan des tailles ===")
    log("\n".join(FAUTES) if FAUTES else
        "presentation (titre, texte, « Suivant »), accueil et seance (« Commencer ») visibles a toutes les tailles")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        log("ERREUR generale :", repr(e))
    sys.exit(0)
