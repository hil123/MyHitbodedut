# -*- coding: utf-8 -*-
"""
Régénère android/app/src/main/java/com/myhitbodedut/Libelles.kt à partir des
chaînes ajout_, fete_ et zman_ de res/values/strings.xml (10/10/2026). Une
table écrite plutôt que getIdentifier : la réduction des ressources de la
version publiée ne garde que les références directes. LibellesTest échoue si
une clé manque.

  python3 tools/libelles_construction.py
"""
import os
import re

RACINE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
MAIN = os.path.join(RACINE, "android", "app", "src", "main")
s = open(os.path.join(MAIN, "res", "values", "strings.xml"), encoding="utf-8").read()
cles = sorted(set(re.findall(r'name="((?:ajout|fete|zman)_[a-z0-9_]+)"', s)))
sortie = '''package com.myhitbodedut

/**
 * Les libellés que le calendrier, les zmanim et le siddour nomment par leur
 * clé (« fete_pessah », « ajout_yaale_veyavo », « zman_alot »). Une table
 * écrite plutôt que getIdentifier : la réduction des ressources de la
 * version publiée ne voit que les références directes, et retirerait le reste.
 *
 * Fichier produit par tools/libelles_construction.py ; LibellesTest vérifie
 * qu'aucune clé n'y manque.
 */
object Libelles {
    fun res(cle: String): Int? = when (cle) {
''' + "".join(f'        "{k}" -> R.string.{k}\n' for k in cles) + '''        else -> null
    }
}
'''
open(os.path.join(MAIN, "java", "com", "myhitbodedut", "Libelles.kt"), "w").write(sortie)
print(len(cles), "libellés")
