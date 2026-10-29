package com.myhitbodedut

import android.app.Application

/** Au lancement, avant le premier écran : la langue de repli, les couleurs choisies, l'icône qui va avec. */
class MyHitbodedutApp : Application() {

    override fun onCreate() {
        super.onCreate()
        LangueApp.appliquerRepli(this)
        Apparence.installer(this)
        IconeLanceur.appliquer(this)
    }
}
