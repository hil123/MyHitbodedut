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
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
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
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
