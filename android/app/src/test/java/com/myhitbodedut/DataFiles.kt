package com.myhitbodedut

import java.io.File

/**
 * Les tests lisent les fichiers livrés, pas une copie : une donnée cassée
 * dans res/raw fait rougir la compilation, et non l'appareil de l'utilisateur.
 */
object DataFiles {

    val chizukJson: String get() = read("chizuk.json")

    val bibliothequeJson: String get() = read("bibliotheque.json")

    val chizuk: Chizuk get() = ChizukLoader.parse(chizukJson)

    val library: Library get() = LibraryLoader.parse(bibliothequeJson)

    val tikkunJson: String get() = read("tikkun_haklali.json")

    val tikkun: Tikkun get() = TikkunLoader.parse(tikkunJson)

    val tehilimJson: String get() = read("tehilim.json")

    val tehilim: Tehilim get() = TehilimLoader.parse(tehilimJson)

    val siddurJson: String get() = read("siddur.json")

    val siddur: Siddur get() = SiddurLoader.parse(siddurJson)

    private fun read(name: String): String {
        val file = File("src/main/res/raw/$name")
        require(file.exists()) { "ressource absente : ${file.absolutePath}" }
        return file.readText()
    }
}
