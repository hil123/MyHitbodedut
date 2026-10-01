package com.myhitbodedut

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.ColorUtils

/**
 * Le dessin des trois anneaux, partage par l'accueil (AnneauxView), la
 * vignette et les notifications, qui n'ont qu'une image a montrer.
 *
 * Le jour est au centre, contre le hassid ; la semaine et la serie
 * l'entourent. Un anneau qui depasse son objectif repasse sur lui-meme
 * dans une teinte plus sombre : le second tour se voit sans rien cacher
 * du premier.
 */
object AnneauxDessin {

    /** Les couleurs, resolues par l'appelant : l'ecran suit sa palette, la vignette non. */
    data class Couleurs(val jour: Int, val semaine: Int, val serie: Int, val piste: Int)

    /** La place des anneaux dans un carre : rayon exterieur de chaque anneau, du centre vers l'exterieur. */
    data class Geometrie(val epaisseur: Float, val ecart: Float) {
        /** L'epaisseur totale des trois anneaux et de leurs deux ecarts. */
        val largeur: Float get() = 3 * epaisseur + 2 * ecart
    }

    /**
     * @param avance de 0 a 1 : la part du remplissage deja dessinee, pour
     *   l'animation d'ouverture ; 1 pour un dessin fixe.
     * @param misEnAvant l'anneau qu'on regarde de pres : il s'epaissit, les
     *   autres s'effacent. null : les trois a egalite.
     */
    fun dessiner(
        toile: Canvas,
        carre: RectF,
        etat: Anneaux.Etat,
        couleurs: Couleurs,
        geometrie: Geometrie,
        rtl: Boolean,
        avance: Float = 1f,
        misEnAvant: Anneaux.Quel? = null
    ) {
        val piste = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        val sens = if (rtl) -1f else 1f
        // De l'exterieur vers le centre : la serie dehors, le jour contre le hassid.
        etat.duCentre.reversed().forEachIndexed { rang, anneau ->
            val depuisBord = rang * (geometrie.epaisseur + geometrie.ecart)
            val enAvant = misEnAvant == null || misEnAvant == anneau.quel
            val trait = if (misEnAvant == anneau.quel) geometrie.epaisseur * 1.3f else geometrie.epaisseur
            val retrait = depuisBord + geometrie.epaisseur / 2f
            val cadre = RectF(
                carre.left + retrait, carre.top + retrait, carre.right - retrait, carre.bottom - retrait
            )
            val couleur = when (anneau.quel) {
                Anneaux.Quel.JOUR -> couleurs.jour
                Anneaux.Quel.SEMAINE -> couleurs.semaine
                Anneaux.Quel.SERIE -> couleurs.serie
            }
            val alpha = if (enAvant) 255 else 70

            piste.strokeWidth = trait
            piste.color = ColorUtils.setAlphaComponent(couleurs.piste, alpha)
            toile.drawOval(cadre, piste)

            val fraction = anneau.fraction * avance.coerceIn(0f, 1f)
            if (fraction <= 0f) return@forEachIndexed
            arc.strokeWidth = trait
            val tours = fraction.toInt()
            val reste = fraction - tours
            if (tours >= 1) {
                // Le premier tour, complet.
                arc.color = ColorUtils.setAlphaComponent(couleur, alpha)
                toile.drawOval(cadre, arc)
            }
            if (reste > 0f) {
                // Le tour en cours : la teinte de base au premier tour, plus
                // sombre ensuite pour qu'on voie qu'il repasse.
                val teinte = if (tours == 0) couleur else ColorUtils.blendARGB(couleur, 0xFF000000.toInt(), 0.28f)
                arc.color = ColorUtils.setAlphaComponent(teinte, alpha)
                toile.drawArc(cadre, -90f, sens * 360f * reste, false, arc)
            }
        }
    }

    /**
     * L'anneau sous un point, pour le toucher : null entre les anneaux, au
     * centre ou dehors.
     */
    fun sousLePoint(
        carre: RectF,
        geometrie: Geometrie,
        x: Float,
        y: Float
    ): Anneaux.Quel? {
        val rayon = carre.width() / 2f
        val distance = Math.hypot((x - carre.centerX()).toDouble(), (y - carre.centerY()).toDouble()).toFloat()
        val depuisBord = rayon - distance
        if (depuisBord < 0f) return null
        // Un peu de marge autour de chaque trait : le doigt n'est pas une pointe.
        val pas = geometrie.epaisseur + geometrie.ecart
        val marge = geometrie.ecart / 2f
        return listOf(Anneaux.Quel.SERIE, Anneaux.Quel.SEMAINE, Anneaux.Quel.JOUR).withIndex()
            .firstOrNull { (rang, _) ->
                depuisBord >= rang * pas - marge && depuisBord <= rang * pas + geometrie.epaisseur + marge
            }?.value
    }
}
