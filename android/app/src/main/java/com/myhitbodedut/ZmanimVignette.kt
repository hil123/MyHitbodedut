package com.myhitbodedut

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * Les trois vignettes des zmanim (demande du 28/10 : « trois différentes, trois
 * tailles, très classe ») sur un même fond de nuit verte :
 * - petite (2 × 2) : la prière du moment, l'heure où elle se ferme, une jauge ;
 * - moyenne (4 × 2) : la même, le temps qui reste, la série, les trois zmanim à venir ;
 * - grande (4 × 4) : la même et la journée entière, le prochain zman en évidence.
 * La date hébraïque et la fête du jour, ou la paracha de la semaine, en tête ; elles changent à la
 * sortie des étoiles du lieu, comme le jour hébreu (02/11).
 */
object ZmanimVignette {

    enum class Taille(val layout: Int) { PETITE(R.layout.widget_zmanim_petit), MOYENNE(R.layout.widget_zmanim), GRANDE(R.layout.widget_zmanim_grand) }

    /** La journée telle que la grande vignette la montre, dans l'ordre. */
    val JOURNEE = listOf("alot", "netz", "szks_gra", "szt_gra", "chatsot", "minha_gedola", "plag", "chkia", "tset")

    private val NOMS = mapOf(
        "alot" to R.string.zman_alot, "netz" to R.string.zman_netz, "szks_gra" to R.string.zman_szks_gra,
        "szt_gra" to R.string.zman_szt_gra, "chatsot" to R.string.zman_chatsot, "minha_gedola" to R.string.zman_minha_gedola,
        "plag" to R.string.zman_plag, "chkia" to R.string.zman_chkia, "tset" to R.string.zman_tset,
    )
    private val LIGNES = listOf(R.id.wv_r0, R.id.wv_r1, R.id.wv_r2, R.id.wv_r3, R.id.wv_r4, R.id.wv_r5, R.id.wv_r6, R.id.wv_r7, R.id.wv_r8)
    private val LIBELLES = listOf(R.id.wv_l0, R.id.wv_l1, R.id.wv_l2, R.id.wv_l3, R.id.wv_l4, R.id.wv_l5, R.id.wv_l6, R.id.wv_l7, R.id.wv_l8)
    private val HEURES = listOf(R.id.wv_t0, R.id.wv_t1, R.id.wv_t2, R.id.wv_t3, R.id.wv_t4, R.id.wv_t5, R.id.wv_t6, R.id.wv_t7, R.id.wv_t8)
    private val PROCHAINS = listOf(R.id.wv_pl0 to R.id.wv_pt0, R.id.wv_pl1 to R.id.wv_pt1, R.id.wv_pl2 to R.id.wv_pt2)

    private val FORT = 0xF2FFFFFF.toInt()
    private const val PASSE = 0x59FFFFFF
    private val MENTHE = 0xFF9FE3CC.toInt()
    private val HEURE = DateTimeFormatter.ofPattern("HH:mm")

    /** Où commence la fenêtre de la prière du moment, pour la jauge : l'aube, la fin de Cha'harit, la chki'a. */
    fun debut(priere: String, horaires: Map<String, ZonedDateTime?>): ZonedDateTime? = when (priere) {
        "chaharit" -> horaires["alot"]
        "minha" -> horaires["szt_gra"]
        else -> horaires["chkia"]
    }

    /**
     * La date civile dont la vignette montre le jour hébreu : celle du lieu, ou le lendemain une fois
     * la sortie des étoiles passée — le jour hébreu commence le soir (02/11).
     */
    fun jourHebreuAffiche(maintenant: ZonedDateTime, tset: ZonedDateTime?): LocalDate =
        if (tset != null && !maintenant.isBefore(tset)) maintenant.toLocalDate().plusDays(1) else maintenant.toLocalDate()

    /** La part écoulée de la fenêtre, de 0 à 1000 ; 0 sans bornes. */
    fun ecoule(debut: ZonedDateTime?, fin: ZonedDateTime?, maintenant: ZonedDateTime): Int {
        if (debut == null || fin == null || !fin.isAfter(debut)) return 0
        val total = Duration.between(debut, fin).seconds.toDouble()
        return (Duration.between(debut, maintenant).seconds / total * 1000).toInt().coerceIn(0, 1000)
    }

    fun vues(context: Context, taille: Taille): RemoteViews {
        val v = RemoteViews(context.packageName, taille.layout)
        v.setOnClickPendingIntent(R.id.wv_corps, PendingIntent.getActivity(context, 160 + taille.ordinal,
            Intent(context, ZmanimActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        val israel = Store.inIsrael(context)
        // Le jour hébreu commence à la sortie des étoiles du lieu (02/11), pas à minuit : le vendredi
        // soir, la vignette dit déjà Chabbat. Sans lieu, pas de tsét connu : minuit.
        val lieuDate = Store.lieuZmanim(context)
        val maintenantLieu = ZonedDateTime.now(lieuDate?.fuseau?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault())
        val tset = lieuDate?.let { l -> Zmanim.jour(l, maintenantLieu.toLocalDate()).firstOrNull { it.cle == "tset" }?.local }
        val aujourdhui = jourHebreuAffiche(maintenantLieu, tset)
        val jour = CalendrierJuif.jour(aujourdhui, israel)
        v.setTextViewText(R.id.wv_date, jour.dateHebraique)
        // La fête du jour, sinon la paracha de la semaine.
        val fete = jour.fetes.firstNotNullOfOrNull { Libelles.res(it) }?.let { context.getString(it) }
        val paracha = CalendrierJuif.jour(aujourdhui.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)), israel).paracha
            ?.let { context.getString(R.string.widget_paracha, it) }
        val pastille = fete ?: paracha
        v.setViewVisibility(R.id.wv_pastille, if (pastille != null) View.VISIBLE else View.GONE)
        v.setTextViewText(R.id.wv_pastille, pastille ?: "")
        if (taille != Taille.PETITE) {
            val serie = Store.streak(context)
            v.setTextViewText(R.id.wv_serie, context.resources.getQuantityString(R.plurals.widget_zmanim_serie, serie, serie))
        }

        val lieu = Store.lieuZmanim(context)
        if (lieu == null) {
            v.setTextViewText(R.id.wv_priere, context.getString(R.string.zmanim_choisir_lieu))
            v.setTextViewText(R.id.wv_limite, "")
            v.setTextViewText(R.id.wv_reste, "")
            v.setViewVisibility(R.id.wv_jauge, View.GONE)
            return v
        }
        val zone = runCatching { ZoneId.of(lieu.fuseau) }.getOrDefault(ZoneId.systemDefault())
        val maintenant = ZonedDateTime.now(zone)
        val horaires = Zmanim.jour(lieu, maintenant.toLocalDate()).associate { it.cle to it.local }
        val (priere, limite) = ZmanimWidget.prochaine(horaires, maintenant)
        v.setTextViewText(R.id.wv_priere, context.getString(when (priere) {
            "chaharit" -> R.string.widget_zmanim_chaharit; "minha" -> R.string.widget_zmanim_minha; else -> R.string.widget_zmanim_arvit
        }))
        v.setTextViewText(R.id.wv_limite, limite?.let { context.getString(R.string.widget_jusqua, it.format(HEURE)) } ?: "")
        v.setViewVisibility(R.id.wv_jauge, if (limite != null) View.VISIBLE else View.GONE)
        v.setProgressBar(R.id.wv_jauge, 1000, ecoule(debut(priere, horaires), limite, maintenant), false)
        v.setTextViewText(R.id.wv_reste, limite?.let {
            val m = Duration.between(maintenant, it).toMinutes().coerceAtLeast(0)
            context.getString(R.string.widget_reste, String.format(java.util.Locale.ROOT, "%d:%02d", m / 60, m % 60))
        } ?: "")

        // La journée : aujourd'hui, ou demain une fois la nuit tombée.
        var journee = JOURNEE.mapNotNull { k -> horaires[k]?.let { k to it } }
        if (journee.none { it.second.isAfter(maintenant) }) {
            val demain = Zmanim.jour(lieu, maintenant.toLocalDate().plusDays(1)).associate { it.cle to it.local }
            journee = JOURNEE.mapNotNull { k -> demain[k]?.let { k to it } }
        }
        val prochain = journee.indexOfFirst { it.second.isAfter(maintenant) }
        when (taille) {
            Taille.MOYENNE -> PROCHAINS.forEachIndexed { k, (l, t) ->
                val z = journee.getOrNull(prochain + k)
                v.setTextViewText(l, z?.let { context.getString(NOMS.getValue(it.first)) } ?: "")
                v.setTextViewText(t, z?.second?.format(HEURE) ?: "")
                v.setTextColor(t, if (k == 0) MENTHE else FORT)
            }
            Taille.GRANDE -> LIGNES.indices.forEach { k ->
                val z = journee.getOrNull(k)
                v.setViewVisibility(LIGNES[k], if (z != null) View.VISIBLE else View.INVISIBLE)
                if (z == null) return@forEach
                v.setTextViewText(LIBELLES[k], context.getString(NOMS.getValue(z.first)))
                v.setTextViewText(HEURES[k], z.second.format(HEURE))
                val passe = !z.second.isAfter(maintenant)
                v.setTextColor(LIBELLES[k], if (k == prochain) MENTHE else if (passe) PASSE else 0xA6FFFFFF.toInt())
                v.setTextColor(HEURES[k], if (k == prochain) MENTHE else if (passe) PASSE else FORT)
                v.setInt(LIGNES[k], "setBackgroundResource", if (k == prochain) R.drawable.bg_widget_ligne_active else 0)
            }
            Taille.PETITE -> Unit
        }
        return v
    }

    /** Toutes les vignettes posées, des trois tailles ; puis le prochain changement programmé. */
    fun rafraichir(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val c = LangueApp.envelopper(context)
        var posees = 0
        for ((classe, taille) in listOf(ZmanimPetitWidget::class.java to Taille.PETITE, ZmanimWidget::class.java to Taille.MOYENNE,
            ZmanimGrandWidget::class.java to Taille.GRANDE)) {
            val ids = manager.getAppWidgetIds(ComponentName(context, classe))
            posees += ids.size
            ids.forEach { manager.updateAppWidget(it, vues(c, taille)) }
        }
        programmer(context, posees > 0)
    }

    /** Au plus tard, la vignette se redessine toutes les dix minutes : le temps qui reste et la jauge. */
    private const val PAS_MAX_MINUTES = 10L

    /**
     * Le prochain instant où la vignette change (02/11) : un zman d'aujourd'hui ou de demain — la
     * fin de Cha'harit, la chki'a, minuit de la nuit… —, minuit (la date en tête), et au plus tard
     * dans dix minutes. Avant, seule la mise à jour d'Android toutes les demi-heures la redessinait :
     * « Cha'harit jusqu'à 9:12 » restait affiché à 9:40.
     */
    fun prochainChangement(
        horaires: Collection<ZonedDateTime?>,
        maintenant: ZonedDateTime,
        minuitTelephone: ZonedDateTime? = null,
    ): ZonedDateTime {
        val minuitLieu = maintenant.toLocalDate().plusDays(1).atStartOfDay(maintenant.zone)
        return (horaires.filterNotNull() + listOfNotNull(minuitLieu, minuitTelephone, maintenant.plusMinutes(PAS_MAX_MINUTES)))
            .filter { it.isAfter(maintenant) }
            .minByOrNull { it.toInstant() }!!
    }

    private fun tic(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 165, Intent(context, ZmanimTic::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /**
     * Une alarme non réveillante : écran éteint, rien ne se passe ; au rallumage, la vignette est
     * redessinée aussitôt. Sans l'autorisation des alarmes exactes, qu'on ne demande pas pour une
     * vignette, Android 14 ne promet l'alarme qu'à dix minutes près : c'est le retard maximal.
     */
    private fun programmer(context: Context, posees: Boolean) {
        val alarmes = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
        val pending = tic(context)
        alarmes.cancel(pending)
        if (!posees) return
        val lieu = Store.lieuZmanim(context)
        val zone = lieu?.fuseau?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
        val maintenant = ZonedDateTime.now(zone)
        val horaires = lieu?.let { l ->
            (Zmanim.jour(l, maintenant.toLocalDate()) + Zmanim.jour(l, maintenant.toLocalDate().plusDays(1))).map { it.local }
        }.orEmpty()
        val minuitTelephone = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault())
        val quand = prochainChangement(horaires, maintenant, minuitTelephone)
        runCatching { alarmes.setWindow(android.app.AlarmManager.RTC, quand.toInstant().toEpochMilli(), 60_000L, pending) }
    }
}

/** Le prochain changement des vignettes des zmanim est arrivé (ZmanimVignette.programmer). */
class ZmanimTic : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = ZmanimWidget.refresh(context)
}

class ZmanimPetitWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // Toutes les tailles, et le prochain changement reprogrammé (02/11).
        ZmanimVignette.rafraichir(context)
    }
}

class ZmanimGrandWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // Toutes les tailles, et le prochain changement reprogrammé (02/11).
        ZmanimVignette.rafraichir(context)
    }
}
