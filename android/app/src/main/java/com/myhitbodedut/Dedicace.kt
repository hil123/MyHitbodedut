package com.myhitbodedut

import android.app.Activity
import android.content.Intent
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import java.net.URLEncoder

/**
 * La dédicace (04/11) : un utilisateur ne dédie pas lui-même sa hitbodedout ou son étude dans
 * l'application ; la demande passe par l'équipe. Elle s'écrit dans la messagerie de
 * l'utilisateur, adressée à l'équipe, prête à partir : rien n'est envoyé sans lui, rien n'est
 * gardé sur le téléphone. Le site fait de même (app.js, dedicacer).
 */
object Dedicace {

    const val ADRESSE = "myhitbodedut26@gmail.com"

    enum class Objet(val libelle: Int) { HITBODEDUT(R.string.dedicace_hitbodedut), ETUDE(R.string.dedicace_etude) }

    /** La formule hébraïque reste en hébreu dans l'e-mail, quelle que soit la langue : c'est elle qui s'affichera. */
    enum class Intention(val libelle: Int, val formule: String?) {
        LEILOUY(R.string.dedicace_leilouy, "לעילוי נשמת"),
        REFOUA(R.string.dedicace_refoua, "לרפואה שלמה"),
        HATSLAHA(R.string.dedicace_hatslaha, "להצלחה"),
        ZIVOUG(R.string.dedicace_zivoug, "לזיווג הגון"),
        AUTRE(R.string.dedicace_autre, null),
    }

    /** Le corps de l'e-mail : la salutation, la demande, la ligne de la dédicace, le mot éventuel, la signature. */
    fun corps(intro: String, demande: String, formule: String?, nom: String, mot: String, fin: String): String =
        buildList {
            add(intro)
            add("")
            add(demande)
            add(listOfNotNull(formule, nom.trim()).joinToString(" "))
            if (mot.isNotBlank()) { add(""); add(mot.trim()) }
            add("")
            add(fin)
        }.joinToString("\n")

    /** mailto: avec le sujet et le corps encodés (espaces en %20, pas en +, que certaines messageries gardent). */
    fun lien(sujet: String, corps: String): String {
        fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
        return "mailto:$ADRESSE?subject=${enc(sujet)}&body=${enc(corps)}"
    }

    fun ouvrir(activite: Activity) {
        val dp = activite.resources.displayMetrics.density
        fun px(v: Int) = (v * dp).toInt()
        fun legende(res: Int) = TextView(activite).apply {
            setText(res)
            setPadding(0, px(12), 0, px(4))
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall)
        }
        val objets = RadioGroup(activite).apply {
            Objet.values().forEachIndexed { i, o ->
                addView(RadioButton(activite).apply { id = 100 + i; setText(o.libelle) })
            }
            check(100)
        }
        val intentions = RadioGroup(activite).apply {
            Intention.values().forEachIndexed { i, o ->
                addView(RadioButton(activite).apply { id = 200 + i; setText(o.libelle) })
            }
            check(200)
        }
        val nom = EditText(activite).apply {
            setHint(R.string.dedicace_nom)
            setSingleLine()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        val mot = EditText(activite).apply {
            setHint(R.string.dedicace_mot)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 2
        }
        val contenu = LinearLayout(activite).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(24), px(8), px(24), 0)
            addView(TextView(activite).apply { setText(R.string.section_dedicace_hint) })
            addView(legende(R.string.dedicace_quoi))
            addView(objets)
            addView(legende(R.string.dedicace_pour))
            addView(intentions)
            addView(nom, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(12) })
            addView(mot, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        val dialogue = AlertDialog.Builder(activite)
            .setTitle(R.string.section_dedicace)
            .setView(android.widget.ScrollView(activite).apply { addView(contenu) })
            .setPositiveButton(R.string.dedicace_ecrire, null)
            .setNegativeButton(R.string.action_back, null)
            .create()
        dialogue.setOnShowListener {
            // Le bouton ne ferme pas la boîte tant que le nom manque.
            dialogue.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (nom.text.isBlank()) {
                    nom.error = activite.getString(R.string.dedicace_nom_manque)
                    return@setOnClickListener
                }
                val objet = Objet.values()[objets.checkedRadioButtonId - 100]
                val intention = Intention.values()[intentions.checkedRadioButtonId - 200]
                val quoi = activite.getString(objet.libelle)
                val texte = corps(
                    activite.getString(R.string.dedicace_corps_intro),
                    activite.getString(R.string.dedicace_corps_demande, quoi.replaceFirstChar { it.lowercase() }),
                    intention.formule,
                    nom.text.toString(),
                    mot.text.toString(),
                    activite.getString(R.string.dedicace_corps_fin),
                )
                val sujet = activite.getString(R.string.dedicace_sujet, quoi)
                val ouvert = runCatching {
                    // Le sujet et le corps aussi en extras : certaines messageries ignorent ceux du lien.
                    activite.startActivity(Intent(Intent.ACTION_SENDTO, lien(sujet, texte).toUri())
                        .putExtra(Intent.EXTRA_EMAIL, arrayOf(ADRESSE))
                        .putExtra(Intent.EXTRA_SUBJECT, sujet)
                        .putExtra(Intent.EXTRA_TEXT, texte))
                }.isSuccess
                if (!ouvert) Toast.makeText(activite, R.string.dedicace_sans_messagerie, Toast.LENGTH_LONG).show()
                dialogue.dismiss()
            }
        }
        dialogue.show()
    }
}
