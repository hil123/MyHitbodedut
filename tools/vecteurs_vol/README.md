# Vecteurs des zmanim en vol pour le web

`VecteursVol.kt` fait répondre `Vol.kt` (sur `Zmanim.kt` et KosherJava 2.5.0)
pour quelques vols : vers l'est, vers l'ouest, de nuit, la ligne de date dans
les deux sens, une route polaire, un vol sans zman, un passage à l'heure d'été.
Le résultat, `web/test/vecteurs_vol.json`, sert de référence à `vol.js` :
`web/test/vol.test.mjs` exige les mêmes zmanim, dans le même ordre, à deux
secondes près, les mêmes réserves et les mêmes dates de part et d'autre de la
ligne. `VolTest` exige de son côté que le fichier reste celui que `Vol.kt`
produit : un changement de calcul oblige à le régénérer.

Aucune valeur n'y est tapée à la main. Pour le régénérer, depuis la racine du
dépôt, sur la JVM seule, sans SDK Android :

- compiler `Vol.kt`, `Zmanim.kt` et `VecteursVol.kt` avec
  `com.kosherjava:zmanim:2.5.0` et `kotlin-stdlib` 2.0.21 (Maven Central) ;
- lancer `VecteursVolKt` et rediriger la sortie dans `web/test/vecteurs_vol.json`.

Comme pour `tools/vecteurs_kosherjava`, ce n'est pas une preuve d'exactitude :
deux portages qui s'accordent montrent seulement que le portage est fidèle.
