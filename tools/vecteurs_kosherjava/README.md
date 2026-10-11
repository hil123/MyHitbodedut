# Vecteurs KosherJava pour le web

`Vecteurs.kt` fait répondre le code Android (`CalendrierJuif.kt`, `Zmanim.kt`,
sur KosherJava 2.5.0) pour chaque jour du 01/09/2025 au 31/12/2030, en
Israël et en diaspora, et pour 35 couples lieu-date de zmanim. Le résultat,
`web/test/vecteurs_kosherjava.json`, sert de référence au portage web
(`tefila.js`, `zmanim.js`) : `web/test/calendrier.test.mjs` exige l'égalité
jour par jour, et une seconde près pour les zmanim.

Aucune valeur n'y est tapée à la main. Pour le régénérer, compiler sur la JVM
seule, sans SDK Android :

- `Vecteurs.kt`, `CalendrierJuif.kt` et `Zmanim.kt` ;
- avec `com.kosherjava:zmanim:2.5.0` et `kotlin-stdlib` 2.0.21 (Maven Central) ;
- lancer `VecteursKt` avec `-Dstdout.encoding=UTF-8` (l’hébreu) et rediriger la sortie dans le fichier.

Ce n'est pas une preuve d'exactitude : deux portages du même code qui
s'accordent montrent seulement que le portage est fidèle. L'exactitude
astronomique se vérifie contre PyEphem (`ZmanimTest`), le calendrier contre
Hebcal et ICU, les règles de prière auprès d'un Rav.
