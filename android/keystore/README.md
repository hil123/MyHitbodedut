# Clé de signature

`myhitbodedut-dev.jks` signe les APK produits par l'intégration continue.

**Ce n'est pas un secret.** Le mot de passe du trousseau et de la clé est
`myhitbodedut`, et il est écrit ici exprès. Cette clé sert uniquement aux
installations directes depuis ce dépôt privé.

## Pourquoi elle est versionnée

Sans clé fixe, Gradle signe chaque compilation avec une clé de débogage
régénérée à neuf sur chaque machine. L'identité de l'application change alors à
chaque build, et Android refuse d'installer la nouvelle version par-dessus
l'ancienne : l'installation échoue sans explication utile.

Une clé stable règle cela. Les mises à jour s'installent par-dessus la version
précédente, et l'historique de pratique est conservé.

## Pour publier sur le Play Store

Ne pas utiliser cette clé. En générer une autre, la garder hors du dépôt, et la
fournir par variables d'environnement, que la configuration Gradle lit en
priorité :

```bash
KEYSTORE_PATH=/chemin/vers/play.jks \
KEYSTORE_PASSWORD=... \
KEY_ALIAS=... \
KEY_PASSWORD=... \
./gradlew assembleRelease
```

En intégration continue, passer ces valeurs par des secrets GitHub.

Une clé de publication est définitive : la perdre interdit toute mise à jour de
l'application sur le Play Store.
