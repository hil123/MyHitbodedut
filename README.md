# MyHitbodedut — Suivi d’habitudes intelligent

Application web autonome (un seul fichier `index.html` à la racine) : ajout d’habitudes,
validation quotidienne, séries, taux de réussite, heatmap sur 7 jours, analyses
comportementales et recommandations du jour.

## Lancer

Ouvrir `index.html` dans un navigateur, ou servir le dépôt :

```bash
python3 -m http.server 8080
# → http://127.0.0.1:8080
```

Tailwind et Lucide sont chargés depuis un CDN : une connexion internet est nécessaire
au chargement de la page.

## Données

Les habitudes sont stockées dans le `localStorage` du navigateur (clé `habit-tracker:v1`).
Rien n’est envoyé sur un serveur.

La couche `window.dataSdk` (`init` / `create` / `update` / `delete`, chacune renvoyant
`{ isOk }`) est isolée en tête du script : pour brancher un vrai backend, il suffit de
remplacer ce bloc en gardant le même contrat.

## Structure

| Fichier | Rôle |
| --- | --- |
| `index.html` | L’application complète : markup, styles, couche de données, logique |
| `README.md` | Ce document |
