package com.myhitbodedut

import com.myhitbodedut.Paliers.Piste
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Les paliers d'objectifs (11/10) : après chaque palier, le suivant. */
class PaliersTest {

    @Test
    fun `apres sept jours d affilee le palier suivant est quatorze`() {
        val e = Paliers.etat(Piste.SERIE, 7)
        assertEquals(7, e.atteint)
        assertEquals(14, e.prochain)
        assertEquals(7, e.reste)
        assertEquals(2, e.rang) // 3 et 7
        assertEquals(Paliers.etat(Piste.SERIE, 0).prochain, 3)
    }

    @Test
    fun `au dela de l echelle le pas continue`() {
        assertEquals(730, Paliers.etat(Piste.SERIE, 400).prochain)
        assertEquals(72000, Paliers.etat(Piste.CUMUL, 61000).prochain)
    }

    @Test
    fun `l objectif quotidien est un palier du jour`() {
        assertTrue(25 in Paliers.echelle(Piste.JOUR, 0, objectifJour = 25))
        assertEquals(25, Paliers.etat(Piste.JOUR, 21, objectifJour = 25).prochain)
    }

    @Test
    fun `les periodes repartent et le mois est le mois hebreu`() {
        val aujourdhui = LocalDate.of(2026, 10, 14) // mercredi, 3 'Hechvan 5787
        val minutes = mapOf(
            LocalDate.of(2026, 10, 10) to 20, // samedi précédent : hors semaine, en Tichri
            LocalDate.of(2026, 10, 12) to 30, // 1 'Hechvan
            LocalDate.of(2026, 10, 14) to 15,
        )
        val b = Paliers.Bilan(aujourdhui, minutes, serie = 2, debutMoisHebreu = LocalDate.of(2026, 10, 12), objectifJour = 20)
        val e = Paliers.etats(b).associateBy { it.piste }
        assertEquals(15, e.getValue(Piste.JOUR).valeur)
        assertEquals(45, e.getValue(Piste.SEMAINE).valeur)
        assertEquals(45, e.getValue(Piste.MOIS).valeur)
        assertEquals(65, e.getValue(Piste.CUMUL).valeur)
        assertEquals(3, e.getValue(Piste.JOURS).valeur)
        // Le jour se fête par date, la série une fois pour toutes.
        val m = Paliers.marques(Paliers.etats(b), b)
        assertTrue("jour:2026-10-14:15" in m)
        assertTrue(m.none { it.startsWith("serie") }) // 2 jours : pas encore 3
        assertEquals(listOf(Piste.JOUR, Piste.SEMAINE, Piste.CUMUL, Piste.JOURS).toSet(),
            Paliers.nouveaux(Paliers.etats(b), b, emptySet()).map { it.piste }.toSet())
        assertEquals(emptyList<Paliers.Etat>(), Paliers.nouveaux(Paliers.etats(b), b, m))
    }

    @Test
    fun `records et palier a la une`() {
        val minutes = mapOf(LocalDate.of(2026, 10, 4) to 30, LocalDate.of(2026, 10, 5) to 40, LocalDate.of(2026, 10, 12) to 20)
        val r = Paliers.records(minutes) { if (it.isBefore(LocalDate.of(2026, 10, 12))) "tichri" else "hechvan" }
        assertEquals(Paliers.Records(jour = 40, semaine = 70, mois = 70), r)
        val une = Paliers.aLaUne(listOf(Paliers.etat(Piste.SERIE, 6), Paliers.etat(Piste.JOUR, 5)))
        assertEquals(Piste.SERIE, une.piste) // 6 sur 3→7 : 75 %, contre 5/10 : 50 %
    }
}
