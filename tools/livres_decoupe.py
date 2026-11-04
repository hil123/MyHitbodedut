#!/usr/bin/env python3
"""
Découpe livres.json pour l'application Android (04/11).

livres.json (16 Mo, dont 9 Mo pour le Kitsour Choul'han Aroukh) était lu en entier, sur le fil
principal, pour afficher la liste des livres, l'accueil (« Reprendre la lecture », favoris) ou
ouvrir la seule Méguila d'Esther (51 Ko) ; puis gardé en mémoire tant que l'application vivait.

La source reste android/app/src/main/livres/livres.json (hors des ressources : elle n'entre pas
dans l'APK) ; les outils l'écrivent, le site la lit à la construction. D'elle, ce script tire
dans res/raw :
  - livres_titres.json : _meta, groupes, et chaque livre sans son texte (sections vides) avec
    son nombre de chapitres ;
  - livre_<id>.json : chaque livre seul, au format du siddour (_meta, groupes, textes : [livre]).

LivresTest vérifie que ces fichiers redonnent exactement la source.

Usage : python3 tools/livres_decoupe.py
"""
import json
import os

RACINE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
SOURCE = os.path.join(RACINE, "android", "app", "src", "main", "livres", "livres.json")
RAW = os.path.join(RACINE, "android", "app", "src", "main", "res", "raw")


def ecrire(chemin, donnees):
    with open(chemin, "w", encoding="utf-8") as f:
        json.dump(donnees, f, ensure_ascii=False, separators=(",", ":"))


def decoupe(livres=None):
    if livres is None:
        livres = json.load(open(SOURCE, encoding="utf-8"))
    tete = {k: v for k, v in livres.items() if k != "textes"}
    index = dict(tete, textes=[])
    for t in livres["textes"]:
        if not t["id"].replace("_", "").isalnum() or t["id"] != t["id"].lower():
            raise ValueError(f"Identifiant de livre inattendu : {t['id']}")
        entete = {k: v for k, v in t.items() if k != "sections"}
        index["textes"].append(dict(entete, sections=[], chapitres=len(t["sections"])))
        ecrire(os.path.join(RAW, f"livre_{t['id']}.json"), dict(tete, textes=[t]))
    ecrire(os.path.join(RAW, "livres_titres.json"), index)
    # Un livre retiré de la source ne laisse pas son fichier derrière lui.
    ids = {t["id"] for t in livres["textes"]}
    for nom in os.listdir(RAW):
        if nom.startswith("livre_") and nom.endswith(".json") and nom[6:-5] not in ids:
            os.remove(os.path.join(RAW, nom))


if __name__ == "__main__":
    decoupe()
    print("livres découpés dans", os.path.normpath(RAW))
