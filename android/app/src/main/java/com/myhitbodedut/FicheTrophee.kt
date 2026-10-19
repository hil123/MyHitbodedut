package com.myhitbodedut

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.core.graphics.ColorUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * La fiche d'un trophée (16/10, point 1) : l'objet, ce qu'il demande, le jour gagné ou la
 * progression. Ouverte d'un toucher dans la vitrine, et d'elle-même dès qu'un trophée est
 * gagné (demande du 19/10) : l'objet se retourne, des confettis tombent sur tout l'écran et
 * des étincelles du métal jaillissent autour de lui. Une fenêtre plein écran, transparente,
 * pour que les confettis passent par-dessus la carte et le voile.
 */
object FicheTrophee {

    fun montrer(
        activite: Activity,
        t: Trophees.Niveau,
        p: Trophees.Pratique,
        dates: Map<String, LocalDate>,
        rangGlobal: Int,
        feter: Boolean = false,
        apres: (() -> Unit)? = null,
    ) {
        if (activite.isFinishing || activite.isDestroyed) return
        val d = activite.resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val gagne = Trophees.gagne(p, t)
        val m = if (gagne) Trophees.grade(rangGlobal) else null
        val v = Trophees.valeur(p, t.famille).coerceAtMost(t.seuil)

        val dialogue = Dialog(activite)
        dialogue.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val racine = FrameLayout(activite).apply { setOnClickListener { dialogue.dismiss() } }
        val carte = LinearLayout(activite).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(24), dp(24), dp(8))
            isClickable = true
            background = GradientDrawable().apply {
                cornerRadius = dp(28).toFloat()
                setColor(MaterialColors.getColor(activite, com.google.android.material.R.attr.colorSurfaceContainerHigh, Color.WHITE))
            }
            elevation = dp(8).toFloat()
        }
        racine.addView(carte, FrameLayout.LayoutParams(
            minOf(activite.resources.displayMetrics.widthPixels - dp(48), dp(360)), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER,
        ))

        val objet = View(activite).apply {
            background = TropheeDessin(activite, t.famille.forme, m, t.famille.icone)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        carte.addView(objet, LinearLayout.LayoutParams(dp(120), dp(120)))
        carte.addView(VueJour.texte(activite, TropheesVue.description(activite, t.famille, t.seuil), 20f, R.color.ink, gras = true, haut = 12)
            .apply { gravity = Gravity.CENTER })
        carte.addView(VueJour.texte(activite, TropheesVue.nom(activite, t.famille) + " · " +
            activite.getString(R.string.trophee_niveau_court, t.rang, t.famille.seuils.size), 14f, R.color.muted, haut = 2)
            .apply { gravity = Gravity.CENTER })
        if (gagne) {
            val le = dates[t.cle]?.let {
                activite.getString(R.string.trophee_gagne_le, it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                    .withLocale(activite.resources.configuration.locales[0])))
            } ?: activite.getString(R.string.trophee_gagne)
            carte.addView(VueJour.texte(activite, le, 15f, R.color.brand, gras = true, haut = 10).apply { gravity = Gravity.CENTER })
            carte.addView(VueJour.texte(activite, activite.getString(R.string.trophee_detail_grade, TropheesVue.metal(activite, m!!), rangGlobal),
                13f, R.color.muted, haut = 4).apply { gravity = Gravity.CENTER })
        } else {
            val couleur = Apparence.couleur(activite, R.color.brand)
            carte.addView(ProgressBar(activite, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 1000
                progress = (Trophees.fraction(p, t).coerceIn(0f, 1f) * 1000).toInt()
                progressTintList = android.content.res.ColorStateList.valueOf(couleur)
                progressBackgroundTintList = android.content.res.ColorStateList.valueOf(ColorUtils.setAlphaComponent(couleur, 0x30))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(10)).apply { topMargin = dp(12) })
            carte.addView(VueJour.texte(activite, activite.getString(R.string.trophee_encore, v, t.seuil, t.seuil - v), 14f, R.color.brand, gras = true, haut = 4)
                .apply { gravity = Gravity.CENTER })
        }
        carte.addView(MaterialButton(activite, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
            text = activite.getString(R.string.celebration_continue)
            setOnClickListener { dialogue.dismiss() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.END
            topMargin = dp(16)
        })

        dialogue.setContentView(racine)
        dialogue.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.6f)
        }
        dialogue.setOnDismissListener { apres?.invoke() }
        dialogue.show()

        if (gagne) AnimationsTrophees.retourner(objet, 150L)
        if (feter && AnimationsTrophees.actives) {
            // Les confettis dans la fenêtre de la fiche : par-dessus la carte et le voile.
            val pluie = Confettis.Vue(activite)
            racine.addView(pluie, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            pluie.lancer()
            objet.postDelayed({ AnimationsTrophees.etincelles(activite, objet, m, racine) }, 700L)
            objet.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        }
    }

    /**
     * Les trophées gagnés depuis la dernière visite, un par famille, le plus précieux d'abord :
     * leurs fiches s'ouvrent l'une après l'autre, chacune avec sa fête.
     */
    fun feter(activite: Activity, nouveaux: List<Trophees.Niveau>) {
        if (nouveaux.isEmpty()) return
        val p = Store.pratique(activite)
        val dates = Store.datesTrophees(activite)
        val faits = Trophees.chemin.filter { Trophees.gagne(p, it) }
        fun suivant(i: Int) {
            if (i >= nouveaux.size) return
            val t = nouveaux[i]
            val rang = faits.indexOfFirst { it.cle == t.cle }.let { if (it < 0) faits.size else it + 1 }
            montrer(activite, t, p, dates, rang, feter = true) { suivant(i + 1) }
        }
        suivant(0)
    }
}
