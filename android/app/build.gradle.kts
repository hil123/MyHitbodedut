plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.myhitbodedut"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.myhitbodedut"
        minSdk = 26
        targetSdk = 35
        // Sans numéro croissant, ni Android ni un gestionnaire de mises à jour
        // ne peuvent voir qu'une version est plus récente que l'installée.
        // L'intégration continue passe le numéro d'exécution.
        val build = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 0
        versionCode = 1 + build
        versionName = "1.$build"
        // Valeur par defaut : seule l'edition d'essai la change, en debug.
        // Sans ce defaut, une variante qui ne passe pas par ce buildType
        // (release, light) referencerait un marqueur jamais rempli, et la
        // fusion du manifeste echouerait.
        manifestPlaceholders["appLabel"] = "@string/app_name"
    }

    /*
     * Clé de signature stable. Sans elle, chaque compilation utilisait une clé
     * de débogage régénérée à neuf, donc une identité différente à chaque fois,
     * et Android refusait d'installer la nouvelle version par-dessus l'ancienne.
     *
     * Le trousseau versionné n'est qu'une clé de développement, son mot de passe
     * est public. Pour une publication sur le Play Store, générer une autre clé
     * gardée hors du dépôt et la fournir par les variables ci-dessous.
     */
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "../keystore/myhitbodedut-dev.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "myhitbodedut"
            keyAlias = System.getenv("KEY_ALIAS") ?: "myhitbodedut"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "myhitbodedut"
        }
    }

    buildTypes {
        /*
         * L'édition de test sert à relire les textes sur le téléphone
         * (écran « Relecture »), à côté de l'édition installée.
         *
         * Deux détails l'y autorisent. Le suffixe d'identifiant en fait une
         * application distincte, donc installable sans désinstaller l'autre.
         * Et elle est signée avec la même clé stable que la release : sans
         * cela chaque compilation repartirait d'un trousseau de débogage
         * régénéré, et aucune version de test ne pourrait remplacer la
         * précédente.
         */
        debug {
            // essai_leger sert a l'edition de test allegee (musique en
            // silence, publiee hors des releases numerotees), lancee a la
            // demande depuis un second flux d'integration continue. Elle
            // portait le meme identifiant que Relecture, donc le meme
            // versionCode a comparer ; comme Relecture est reconstruite a
            // chaque envoi sur main tandis que l'edition d'essai ne l'est
            // qu'a la demande, son propre compteur restait presque toujours
            // en retard, et Android refuse d'installer un versionCode plus
            // bas que celui deja en place (INSTALL_FAILED_VERSION_DOWNGRADE)
            // — en silence ou avec une erreur qui passe facilement
            // inapercue. Aucune des editions d'essai envoyees jusqu'ici n'a
            // donc pu s'installer par-dessus une Relecture plus recente.
            // Un identifiant distinct l'exempte de cette comparaison, quel
            // que soit l'ecart entre les deux compteurs.
            val essaiLeger = System.getenv("ESSAI_LEGER") == "true"
            applicationIdSuffix = if (essaiLeger) ".debug.essai" else ".debug"
            versionNameSuffix = if (essaiLeger) "-essai" else "-debug"
            signingConfig = signingConfigs.getByName("release")
            // Les deux portaient le meme nom affiche, « MyHitbodedut » : deux
            // icones identiques sur l'ecran d'accueil, aucun moyen de les
            // distinguer au premier coup d'œil. Un retour a persiste apres
            // le correctif d'identifiant precedent, alors que le paquet
            // etait bien distinct — la confusion se faisait probablement au
            // moment de choisir laquelle des deux icones rouvrir.
            //
            // Un marqueur de manifeste (« placeholder »), pas resValue : un
            // resValue de meme nom qu'une chaine deja declaree dans
            // values/strings.xml (app_name l'est) casse la fusion des
            // ressources pour tout ce qui partage ce type de compilation —
            // Relecture aussi bien que l'essai. Le marqueur, lui, ne touche
            // que le texte du manifeste, jamais les ressources.
            manifestPlaceholders["appLabel"] =
                if (essaiLeger) "MyHitbodedut (essai)" else "@string/app_name"
        }

        /*
         * R8 retire le code et les ressources que rien n'atteint. Le gain
         * est la taille : le jar de calendrier hebraique est embarque en
         * entier alors que l'application n'en appelle qu'une classe.
         *
         * L'edition de test, elle, reste non minifiee : c'est celle qu'on
         * installe pour comprendre un probleme, et une trace renommee y
         * serait un obstacle de plus.
         */
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }

    /*
     * Deux éditions.
     *
     * « full » contient la barrière du matin, donc l'affichage par-dessus les
     * autres applications et l'accès aux données d'utilisation. Cette paire de
     * permissions est exactement celle des logiciels de surveillance, et Play
     * Protect bloque volontiers une application hors boutique qui la demande.
     *
     * « light » retire la barrière et ces permissions. Il reste le minuteur, le
     * journal, la série, le rappel et la sauvegarde. Elle s'installe sans heurt,
     * et sert aussi à isoler la cause quand l'autre est refusée.
     */
    flavorDimensions += "edition"

    productFlavors {
        create("full") {
            dimension = "edition"
            buildConfigField("boolean", "HAS_GATE", "true")
        }
        create("light") {
            dimension = "edition"
            applicationIdSuffix = ".light"
            versionNameSuffix = "-light"
            buildConfigField("boolean", "HAS_GATE", "false")
        }
    }

    lint {
        textReport = true
        textOutput = file("build/reports/lint-results-debug.txt")
    }

}

dependencies {
    // Calendrier hébraïque hors ligne. Version vérifiée sur Maven Central le
    // 20/09/2026 : 2.5.0 est la dernière publiée sous cette coordonnée.
    implementation("com.kosherjava:zmanim:2.5.0")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    testImplementation("junit:junit:4.13.2")
    // org.json n'existe qu'en stub dans le jar Android des tests unitaires :
    // toute méthode y lève « not mocked ». La vraie implémentation, placée
    // devant ce stub, fait tourner les analyseurs de chizuk.json et de
    // bibliotheque.json sur la machine virtuelle, sans appareil.
    testImplementation("org.json:json:20240303")
}
