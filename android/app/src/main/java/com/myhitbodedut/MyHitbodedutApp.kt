package com.myhitbodedut

import android.app.Application

/** Le seul travail fait au lancement : poser les couleurs choisies avant le premier écran. */
class MyHitbodedutApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Apparence.installer(this)
    }
}
