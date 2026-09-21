# R8 est actif en release : il retire le code inatteignable et raccourcit
# les noms. Le gain visé est la taille, pas la protection — l'obfuscation
# ralentit la lecture du code, elle ne l'empêche pas, et il n'y a rien à
# protéger dans une application qui affiche des Tehilim.
#
# Rien n'est gardé de force ici, et c'est délibéré. L'application n'appelle
# aucune classe ni aucune ressource par son nom : pas de Class.forName, pas
# de getIdentifier, pas de newInstance. Les composants du manifeste et les
# vues gonflées depuis le XML sont déjà gardés par les règles d'Android.
# Toute règle ajoutée « au cas où » ne ferait qu'annuler le gain sans qu'on
# sache jamais si elle servait.

# Une trace d'exécution doit rester lisible malgré le renommage : les
# numéros de ligne sont conservés, et mapping.txt est publié à côté de
# l'APK pour pouvoir la retraduire.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
