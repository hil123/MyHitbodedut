package com.myhitbodedut

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Israël ou diaspora d'après la position (16/10) : des villes de part et d'autre des frontières. */
class TerritoireIsraelTest {

    private val dedans = mapOf(
        "Jérusalem" to (31.7769 to 35.2224), "Tel Aviv" to (32.0853 to 34.7818), "Haïfa" to (32.7940 to 34.9896),
        "Eilat" to (29.5577 to 34.9519), "Beer Chéva" to (31.2518 to 34.7913), "Mitspé Ramon" to (30.6097 to 34.8019),
        "Kiryat Chmona" to (33.2073 to 35.5700), "Metoula" to (33.2770 to 35.5790), "Katsrin" to (32.9930 to 35.6900),
        "Ariel" to (32.1060 to 35.1710), "Hébron" to (31.5326 to 35.0998), "Tibériade" to (32.7922 to 35.5312),
        "Nahariya" to (33.0058 to 35.0950), "Ashkelon" to (31.6688 to 34.5743), "Ein Gedi" to (31.4500 to 35.3830),
        "Netanya" to (32.3215 to 34.8532), "Akko" to (32.9281 to 35.0818), "Bet Chéan" to (32.4973 to 35.4968),
        "Sderot" to (31.5250 to 34.5966), "Yotvata" to (29.8950 to 35.0600),
    )
    private val dehors = mapOf(
        "Amman" to (31.9539 to 35.9106), "Aqaba" to (29.5321 to 35.0063), "Beyrouth" to (33.8938 to 35.5018),
        "Tyr" to (33.2705 to 35.2038), "Damas" to (33.5138 to 36.2765), "Le Caire" to (30.0444 to 31.2357),
        "El Arich" to (31.1316 to 33.7984), "Taba" to (29.4900 to 34.8760), "Paris" to (48.8566 to 2.3522),
        "Irbid" to (32.5556 to 35.8500), "Nicosie" to (35.1856 to 33.3823), "Kerak" to (31.1800 to 35.7000),
    )

    @Test
    fun `les villes d Israel sont dedans`() = dedans.forEach { (nom, p) -> assertTrue(nom, TerritoireIsrael.contient(p.first, p.second)) }

    @Test
    fun `les villes voisines sont dehors`() = dehors.forEach { (nom, p) -> assertFalse(nom, TerritoireIsrael.contient(p.first, p.second)) }
}
