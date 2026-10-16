package com.myhitbodedut

import java.time.LocalDate

/**
 * Les rappels de tefila (16/10, sur le modèle de CalJ) : quelques jours avant, la carte
 * de l'accueil annonce un changement dans la prière. Seulement des changements établis,
 * chacun avec sa source, et tirés des conditions du jour déjà vérifiées (CalendrierJuif,
 * KosherJava) :
 *
 *  - PLUIE_DEBUT : on commence à demander la pluie (וְתֵן טַל וּמָטָר, ou בָּרֵךְ עָלֵינוּ chez
 *    les Edot) à Arvit du soir qui ouvre le premier jour — 7 'Hechvan en Israël, la date
 *    de la tekoufa en diaspora (Choul'han Aroukh, O.H. 117, 1) ;
 *  - PLUIE_FIN : on cesse de la demander à partir de Pessa'h (même source) ;
 *  - GUECHEM_DEBUT, GUECHEM_FIN : מַשִּׁיב הָרוּחַ, de Moussaf de Chemini Atseret (22 Tichri)
 *    à Moussaf du premier jour de Pessa'h (15 Nissan) (O.H. 114, 1) ;
 *  - DIX_JOURS : les dix jours de techouva, dès le soir de Roch Hachana (O.H. 582, 1) ;
 *  - OMER : le compte commence le second soir de Pessa'h (O.H. 489, 1).
 *
 * Rien d'Android ici.
 */
object RappelsTefila {

    enum class Type(val source: String, val leSoir: Boolean) {
        PLUIE_DEBUT("117:1", true),
        PLUIE_FIN("117:1", false),
        GUECHEM_DEBUT("114:1", false),
        GUECHEM_FIN("114:1", false),
        DIX_JOURS("582:1", true),
        OMER("489:1", true),
    }

    /**
     * [date] : le jour civil du changement. Pour un changement « du soir » ([Type.leSoir]),
     * il commence au soir de la veille, à Arvit ; sinon dans la journée (Moussaf).
     */
    data class Rappel(val type: Type, val date: LocalDate)

    /** Les mois de JewishDate (NISSAN = 1, TICHREI = 7). */
    private const val NISSAN = 1
    private const val TICHREI = 7

    /**
     * Les changements des [avance] jours à venir (aujourd'hui compris), du plus proche au plus lointain.
     * [sansGuechem] : les Edot de l'hémisphère sud disent מוריד הטל toute l'année (SiddurJour.conditionsPour,
     * Yalkut Yosef O.H. 117) ; ni début ni fin de מַשִּׁיב הָרוּחַ à annoncer.
     */
    fun prochains(aujourdhui: LocalDate, avance: Int = 3, sansGuechem: Boolean = false, jour: (LocalDate) -> CalendrierJuif.Jour): List<Rappel> {
        val res = mutableListOf<Rappel>()
        var veille = jour(aujourdhui.minusDays(1))
        for (k in 0..avance) {
            val d = aujourdhui.plusDays(k.toLong())
            val j = jour(d)
            val pluie = CalendrierJuif.C.TAL_OUMATAR
            if (pluie in j.conditions && pluie !in veille.conditions) res += Rappel(Type.PLUIE_DEBUT, d)
            if (pluie !in j.conditions && pluie in veille.conditions) res += Rappel(Type.PLUIE_FIN, d)
            if (!sansGuechem && j.moisHebreu == TICHREI && j.jourHebreu == 22) res += Rappel(Type.GUECHEM_DEBUT, d)
            if (!sansGuechem && j.moisHebreu == NISSAN && j.jourHebreu == 15) res += Rappel(Type.GUECHEM_FIN, d)
            if (j.moisHebreu == TICHREI && j.jourHebreu == 1) res += Rappel(Type.DIX_JOURS, d)
            if (j.omer == 1) res += Rappel(Type.OMER, d)
            veille = j
        }
        // Un changement du soir dont le soir est déjà passé n'est plus annoncé (revue du 16/10).
        return res.filter { !(it.type.leSoir && !it.date.isAfter(aujourdhui)) }
    }
}

/** La carte d'un rappel de tefila, et la phrase du moment (« Motsaé Chabbat », « Demain soir »…). */
object RappelsTefilaVue {

    /** « 117:1 » ; en hébreu « קי״ז, א׳ », comme les autres références de l'app. */
    fun sourceLocale(c: android.content.Context, source: String): String {
        if (ChizukCard.language(c) != "he") return source
        val (s, a) = source.split(":").map { it.toInt() }
        return "${Reference.lettres(s)}, ${Reference.lettres(a)}"
    }

    fun cartes(c: android.content.Context): List<android.view.View> {
        val aujourdhui = LocalDate.now()
        val israel = Store.inIsrael(c)
        val muk = Store.mukafChoma(c)
        val edot = Store.siddurNusah(c) == Nusah.EDOT
        val sansGuechem = edot && Store.hemisphereSud(c)
        return RappelsTefila.prochains(aujourdhui, sansGuechem = sansGuechem) { CalendrierJuif.jour(it, israel, muk) }.map { r ->
            val (titre, corps) = when (r.type) {
                RappelsTefila.Type.PLUIE_DEBUT -> R.string.rappel_pluie_debut_titre to if (edot) R.string.rappel_pluie_debut_edot else R.string.rappel_pluie_debut
                RappelsTefila.Type.PLUIE_FIN -> R.string.rappel_pluie_fin_titre to if (edot) R.string.rappel_pluie_fin_edot else R.string.rappel_pluie_fin
                RappelsTefila.Type.GUECHEM_DEBUT -> R.string.rappel_guechem_titre to R.string.rappel_guechem_debut
                RappelsTefila.Type.GUECHEM_FIN -> R.string.rappel_guechem_titre to R.string.rappel_guechem_fin
                RappelsTefila.Type.DIX_JOURS -> R.string.rappel_dix_jours_titre to R.string.rappel_dix_jours
                RappelsTefila.Type.OMER -> R.string.rappel_omer_titre to R.string.rappel_omer
            }
            VueJour.carte(c).apply {
                (layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.topMargin = (12 * c.resources.displayMetrics.density).toInt()
                addView(VueJour.texte(c, c.getString(titre), 19f, R.color.ink, gras = true).apply { gravity = android.view.Gravity.CENTER })
                addView(VueJour.texte(c, c.getString(corps, moment(c, r, aujourdhui)), 16f, R.color.ink, haut = 8).apply {
                    gravity = android.view.Gravity.CENTER
                    setLineSpacing(0f, 1.25f)
                })
                addView(VueJour.texte(c, c.getString(R.string.rappel_source, sourceLocale(c, r.type.source)), 12f, R.color.muted, haut = 8)
                    .apply { gravity = android.view.Gravity.CENTER })
            }
        }
    }

    /** Le moment du changement, en tête de phrase. */
    fun moment(c: android.content.Context, r: RappelsTefila.Rappel, aujourdhui: LocalDate): String {
        val jourSemaine = { d: LocalDate ->
            d.format(java.time.format.DateTimeFormatter.ofPattern("EEEE", c.resources.configuration.locales[0]))
                .replaceFirstChar { it.titlecase(c.resources.configuration.locales[0]) }
        }
        if (!r.type.leSoir) return when (r.date) {
            aujourdhui -> c.getString(R.string.rappel_aujourdhui)
            aujourdhui.plusDays(1) -> c.getString(R.string.rappel_demain)
            else -> jourSemaine(r.date)
        }
        val soir = r.date.minusDays(1)
        return when {
            soir.isBefore(aujourdhui) -> c.getString(R.string.rappel_hier_soir)
            soir.dayOfWeek == java.time.DayOfWeek.SATURDAY -> c.getString(R.string.rappel_motsae)
            soir == aujourdhui -> c.getString(R.string.rappel_ce_soir)
            soir == aujourdhui.plusDays(1) -> c.getString(R.string.rappel_demain_soir)
            else -> c.getString(R.string.rappel_soir, jourSemaine(soir))
        }
    }
}
