package com.myhitbodedut

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * La série sur l'écran d'accueil, et de quoi commencer d'un geste.
 *
 * Ouvrir l'application pour savoir où l'on en est est déjà un détour, et
 * un détour de plus suffit à ne pas s'y mettre. La vignette dit la série
 * et l'état du jour sans rien ouvrir, et son bouton lance la hitbodedout
 * directement — le même chemin que le bouton « Commencer » d'un rappel.
 *
 * Elle suit la place qu'on lui donne (demande du 28/09, WidgetTaille) :
 * une petite place, les anneaux et le hassid avec la série en pastille ;
 * la taille habituelle, les anneaux d'un côté, de l'autre la salutation,
 * les minutes du jour en grands chiffres, la semaine et le bouton ; une
 * grande place, la semaine en sept pastilles et un texte de ’hizouk du
 * jour en plus.
 *
 * Elle porte les couleurs choisies dans les réglages (demande du 04/10),
 * et n'est plus une simple carte (demande du 04/10, « trop banale ») : un
 * dégradé de la couleur des boutons choisie, deux grands arcs pâles qui
 * reprennent les anneaux, un halo derrière le hassid. Voir Teintes et
 * VignetteDecor.
 */
class StreakWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val traduit = LangueApp.envelopper(context)
        appWidgetIds.forEach { draw(traduit, manager, it) }
    }

    /**
     * On a agrandi ou réduit la vignette : avant Android 12, c'est à nous de
     * changer de version ; depuis, le fond se redessine aux proportions de
     * la nouvelle place.
     */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        draw(LangueApp.envelopper(context), manager, appWidgetId)
    }

    companion object {

        /**
         * Redessine toutes les vignettes posées. Appelé quand la journée
         * change d'état : une série qui vient d'être allongée doit se voir
         * tout de suite, pas à la prochaine mise à jour du système.
         */
        fun refresh(context: Context) {
            runCatching {
                val manager = AppWidgetManager.getInstance(context) ?: return
                val ids = manager.getAppWidgetIds(
                    ComponentName(context.applicationContext, StreakWidget::class.java)
                )
                val traduit = LangueApp.envelopper(context)
                ids.forEach { draw(traduit, manager, it) }
                if (ids.isNotEmpty()) programmerMinuit(context, ids)
            }
        }

        /**
         * La vignette de [taille] telle que l'écran d'accueil la recevrait, à
         * sa place ordinaire : pour la vitrine de l'édition de test
         * (VitrineWidgetActivity), qui la montre sans lanceur.
         */
        internal fun apercu(context: Context, taille: WidgetTaille): RemoteViews {
            val traduit = LangueApp.envelopper(context)
            return vues(
                traduit, taille, taille.largeurType, taille.hauteurType,
                Etat.lire(traduit), Teintes.lire(traduit)
            )
        }

        /**
         * Une mise à jour juste après minuit : sans elle, la vignette
         * montrait la coche de la veille jusqu'à une heure après (sa période
         * système est d'une heure, et différée en veille).
         */
        private fun programmerMinuit(context: Context, ids: IntArray) {
            val minuit = java.time.LocalDate.now().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli() + 60_000L
            val intent = Intent(context, StreakWidget::class.java)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            val pending = PendingIntent.getBroadcast(
                context, 9, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmes = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            runCatching { alarmes.set(android.app.AlarmManager.RTC, minuit, pending) }
        }

        private fun draw(context: Context, manager: AppWidgetManager, id: Int) {
            val etat = Etat.lire(context)
            val teintes = Teintes.lire(context)
            val options = manager.getAppWidgetOptions(id)
            val vues = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Le lanceur choisit lui-même la version qui tient dans la place.
                // S'il dit les places qu'il donne (portrait, paysage), chacune a
                // sa version, au fond dessiné à ses proportions ; sinon, les
                // trois tailles, à leur place ordinaire.
                val places = places(options)
                RemoteViews(
                    if (places.isNotEmpty()) places.associateWith { place ->
                        vues(
                            context, WidgetTaille.pour(place.width, place.height),
                            place.width, place.height, etat, teintes
                        )
                    } else WidgetTaille.entries.associate { taille ->
                        SizeF(taille.largeurMin, taille.hauteurMin) to
                            vues(context, taille, taille.largeurType, taille.hauteurType, etat, teintes)
                    }
                )
            } else {
                val largeur = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180).toFloat()
                val hauteur = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 110).toFloat()
                val taille = WidgetTaille.pour(largeur, hauteur)
                vues(
                    context, taille,
                    largeur.takeIf { it > 0f } ?: taille.largeurType,
                    hauteur.takeIf { it > 0f } ?: taille.hauteurType,
                    etat, teintes
                )
            }
            manager.updateAppWidget(id, vues)
        }

        /**
         * Les places que le lanceur dit donner à la vignette, en dp (Android
         * 12 et plus) : d'ordinaire deux, en portrait et en paysage. Deux au
         * plus : chaque version porte ses images, et RemoteViews refuse une
         * vignette dont les images dépassent une fois et demie l'écran (vite
         * atteint sur un écran 720p avec trois grandes versions).
         */
        private fun places(options: Bundle): List<SizeF> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return emptyList()
            @Suppress("DEPRECATION")
            val tailles = runCatching {
                options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
            }.getOrNull()
            return tailles.orEmpty().filter { it.width > 0f && it.height > 0f }.distinct().take(2)
        }

        /** Ce que la vignette montre, lu une fois pour toutes les versions. */
        private class Etat(
            val streak: Int,
            val running: Boolean,
            val done: Boolean,
            /** Shabbat, Yom Tov ou pause : rien n'est attendu aujourd'hui. */
            val repos: Boolean,
            val semaine: List<WeekDay>,
            val anneaux: Anneaux.Etat,
            val prenom: String
        ) {
            companion object {
                fun lire(context: Context): Etat = Etat(
                    streak = Store.streak(context),
                    running = Store.isSessionRunning(context),
                    done = Store.isDone(context),
                    repos = Store.isExempt(context, java.time.LocalDate.now()),
                    semaine = Store.currentWeek(context),
                    anneaux = Store.anneaux(context),
                    prenom = Store.firstName(context)
                )
            }
        }

        /**
         * Les couleurs de la vignette, prises dans la palette choisie. Le
         * lanceur la dessine sans le thème de l'application, et RemoteViews
         * ne lit pas « ?attr/mhBrand » : chaque couleur est donc résolue ici,
         * dans un contexte aux couleurs choisies (Apparence.contexte, le même
         * que l'écran de blocage), puis posée vue par vue.
         *
         * La vignette est une vitrine : elle porte la couleur des boutons
         * choisie, en dégradé, quel que soit le fond choisi, de jour comme de
         * nuit (la nuit, plus sombre). Elle ne prend que des jetons fixes de
         * la palette — day_done, brand_dark, mint, stage_soft, on_plate —
         * qui ne basculent pas la nuit : l'encre blanche et « mint » restent
         * lisibles sur tout le dégradé (WidgetTest le vérifie pour chaque
         * palette), et le bouton est la plaque claire et son encre, dont
         * PaletteTest vérifie le contraste.
         *
         * En mode automatique, la bascule du téléphone vers la nuit ne se
         * voit qu'au prochain dessin (ouverture de l'application, minuit, ou
         * l'heure suivante) : une vignette ne sait pas changer de couleurs
         * d'elle-même.
         */
        private class Teintes(
            /** Les deux bouts du dégradé du fond. */
            val clair: Int,
            val fonce: Int,
            /** Les chiffres, le temps, le texte du jour. */
            val encre: Int,
            /** Les libellés : la couleur claire de la palette. */
            val douce: Int,
            /** Le bouton : une pastille claire, et son encre. */
            val pastille: Int,
            val surPastille: Int,
            val jourFait: Int,
            val surJourFait: Int,
            /** La flamme de la série, allumée : la couleur de l'anneau de la série. */
            val flamme: Int,
            val anneaux: AnneauxDessin.Couleurs,
            /** La plaque claire sous le hassid, claire de nuit comme de jour. */
            val plaque: Int
        ) {
            companion object {
                fun lire(context: Context): Teintes {
                    val palette = Apparence.contexte(context)
                    fun c(id: Int) = Apparence.couleur(palette, id)
                    val nuit = (palette.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                        Configuration.UI_MODE_NIGHT_YES
                    val (clair, fonce) = VignetteDecor.degrade(c(R.color.day_done), c(R.color.brand_dark), nuit)
                    val mint = c(R.color.mint)
                    return Teintes(
                        clair = clair,
                        fonce = fonce,
                        encre = c(R.color.white),
                        douce = mint,
                        pastille = c(R.color.stage_soft),
                        surPastille = c(R.color.on_plate),
                        jourFait = mint,
                        surJourFait = c(R.color.brand_dark),
                        flamme = c(R.color.ring_streak_on_dark),
                        // Les anneaux de la vignette d'origine, faits pour un fond sombre.
                        anneaux = AnneauxDessin.Couleurs(
                            jour = mint,
                            semaine = c(R.color.ring_week_on_dark),
                            serie = c(R.color.ring_streak_on_dark),
                            piste = Color.argb(60, 255, 255, 255)
                        ),
                        plaque = c(R.color.stage_soft)
                    )
                }
            }
        }

        private fun vues(
            context: Context,
            taille: WidgetTaille,
            largeurDp: Float,
            hauteurDp: Float,
            etat: Etat,
            teintes: Teintes
        ): RemoteViews {
            val gabarit = when (taille) {
                WidgetTaille.PETITE -> R.layout.widget_small
                WidgetTaille.MOYENNE -> R.layout.widget_streak
                WidgetTaille.GRANDE -> R.layout.widget_large
            }
            val views = RemoteViews(context.packageName, gabarit)
            val rtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

            // Le fond : une image au dégradé de la palette, aux proportions de
            // la place, coins arrondis compris. Un fond de vue ne se teint pas
            // par RemoteViews avant Android 12, et un dégradé pas du tout.
            views.setImageViewBitmap(R.id.widget_fond, fond(context, largeurDp, hauteurDp, teintes, rtl))

            // Les anneaux et le hassid. Pendant la hitbodedout, il prie.
            val personnage = if (etat.running) Character.during() else Character.greeting(etat.done)
            views.setImageViewBitmap(
                R.id.widget_gauge,
                jauge(context, etat.anneaux, personnage, teintes, coteJauge(taille, largeurDp, hauteurDp), rtl)
            )
            views.setContentDescription(R.id.widget_gauge, descriptionAnneaux(context, etat.anneaux))

            // La série, dans sa pastille : la flamme s'allume quand le jour
            // est fait, ou qu'il n'attend rien (Shabbat ne l'éteint pas).
            val allumee = etat.done || etat.repos
            views.setInt(R.id.widget_badge_fond, "setColorFilter", VignetteDecor.NOIR)
            views.setInt(R.id.widget_badge_fond, "setImageAlpha", alpha(VignetteDecor.VOILE_BADGE))
            views.setInt(R.id.widget_flame, "setColorFilter", if (allumee) teintes.flamme else teintes.encre)
            views.setInt(R.id.widget_flame, "setImageAlpha", if (allumee) 255 else 150)
            views.setTextViewText(R.id.widget_streak, etat.streak.toString())
            views.setTextColor(R.id.widget_streak, teintes.encre)
            views.setTextColor(R.id.widget_chrono, teintes.encre)
            views.setContentDescription(
                R.id.widget_badge,
                if (etat.streak == 0) context.getString(R.string.status_streak_none)
                else context.resources.getQuantityString(R.plurals.status_streak, etat.streak, etat.streak)
            )
            compteur(context, views, etat, taille)

            // Le reste de la vignette ouvre l'application, sans rien décider ;
            // pendant une hitbodedout, son écran.
            views.setOnClickPendingIntent(
                R.id.widget_body,
                if (etat.running) activity(context, 3, Intent(context, SessionActivity::class.java))
                else activity(context, 2, Intent(context, MainActivity::class.java))
            )

            if (taille == WidgetTaille.PETITE) return views

            views.setTextViewText(R.id.widget_greeting, salutation(context, etat))
            views.setTextColor(R.id.widget_greeting, teintes.douce)
            views.setTextViewText(
                R.id.widget_minutes,
                minutes(context, etat.anneaux.jour.fait, etat.anneaux.jour.objectif, teintes)
            )
            views.setTextColor(R.id.widget_minutes, teintes.encre)
            views.setTextViewText(
                R.id.widget_week_label,
                context.resources.getQuantityString(
                    R.plurals.week_gauge, etat.anneaux.semaine.fait,
                    etat.anneaux.semaine.fait, etat.anneaux.semaine.objectif
                )
            )
            views.setTextColor(R.id.widget_week_label, teintes.douce)

            views.setInt(R.id.widget_start_fond, "setColorFilter", teintes.pastille)
            views.setTextColor(R.id.widget_start, teintes.surPastille)
            views.setTextViewText(
                R.id.widget_start,
                context.getString(if (etat.running) R.string.session_finish_now else R.string.widget_start)
            )
            // Hors hitbodedout, le bouton part droit sur son écran et la lance.
            // Pendant, il la termine, comme le bouton de la notification : le
            // même service, la même action.
            views.setOnClickPendingIntent(
                R.id.widget_start,
                if (etat.running) {
                    PendingIntent.getService(
                        context,
                        4,
                        Intent(context, SessionService::class.java).setAction(SessionService.ACTION_FINISH),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                } else {
                    activity(
                        context,
                        1,
                        Intent(context, SessionActivity::class.java)
                            .putExtra(SessionActivity.EXTRA_START_NOW, true)
                    )
                }
            )

            if (taille == WidgetTaille.GRANDE) {
                semaine(context, views, etat.semaine, teintes)
                views.setInt(R.id.widget_text_fond, "setColorFilter", VignetteDecor.NOIR)
                views.setInt(R.id.widget_text_fond, "setImageAlpha", alpha(VignetteDecor.VOILE_PANNEAU))
                views.setTextColor(R.id.widget_text, teintes.encre)
                views.setTextColor(R.id.widget_ref, teintes.douce)
                texteDuJour(context, views)
            }
            return views
        }

        private fun alpha(part: Float): Int = (part * 255).toInt().coerceIn(0, 255)

        /**
         * La première ligne : pendant la hitbodedout, qu'elle est en cours ;
         * sinon bonjour ou bonsoir selon l'heure (Salutation), avec le prénom ;
         * sans prénom, l'état du jour.
         */
        private fun salutation(context: Context, etat: Etat): String = when {
            etat.running -> context.getString(R.string.widget_running)
            etat.prenom.isNotBlank() -> context.getString(Salutation.chaine(LocalTime.now().hour), etat.prenom)
            else -> context.getString(
                when {
                    etat.done -> R.string.widget_done
                    // « Pas encore aujourd'hui » un jour de Shabbat sonnait
                    // comme un reproche (revue du 30/09).
                    etat.repos -> R.string.widget_rest
                    else -> R.string.widget_todo
                }
            )
        }

        /**
         * « 12 / 20 min » : les minutes faites en grands chiffres, l'objectif
         * et l'unité en plus petit, dans la couleur des libellés.
         */
        private fun minutes(context: Context, fait: Int, objectif: Int, teintes: Teintes): CharSequence {
            val texte = context.getString(R.string.widget_minutes, fait, objectif)
            val nombre = fait.toString()
            val debut = texte.indexOf(nombre)
            if (debut < 0) return texte
            val fin = debut + nombre.length
            return SpannableString(texte).apply {
                listOf(0 until debut, fin until texte.length).filterNot { it.isEmpty() }.forEach { morceau ->
                    val bout = morceau.last + 1
                    setSpan(RelativeSizeSpan(0.5f), morceau.first, bout, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(ForegroundColorSpan(teintes.douce), morceau.first, bout, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }

        private fun descriptionAnneaux(context: Context, etat: Anneaux.Etat): String =
            context.getString(
                R.string.rings_description,
                Store.formatDuration(context, etat.jour.fait), Store.formatDuration(context, etat.jour.objectif),
                etat.semaine.fait, etat.semaine.objectif, etat.serie.fait, etat.serie.objectif
            )

        /**
         * Le temps de la hitbodedout en direct. Le Chronometer est tenu par le
         * lanceur lui-même : la vignette n'a pas à se redessiner chaque
         * seconde, et la batterie n'en paie rien. Il dit ce que dit son écran
         * (Store.sessionCounter) : le temps restant, le temps écoulé sans
         * durée fixée, ou rien si on a choisi de masquer le compteur. Il
         * prend la place des minutes du jour ; sur la petite vignette, celle
         * du compte dans la pastille.
         */
        private fun compteur(context: Context, views: RemoteViews, etat: Etat, taille: WidgetTaille) {
            val compteur = if (etat.running) Store.sessionCounter(context) else null
            val montre = compteur != null && compteur != SessionRule.Compteur.MASQUE
            views.setViewVisibility(R.id.widget_chrono, if (montre) View.VISIBLE else View.GONE)
            views.setViewVisibility(
                if (taille == WidgetTaille.PETITE) R.id.widget_streak else R.id.widget_minutes,
                if (montre) View.GONE else View.VISIBLE
            )
            if (!montre) {
                views.setChronometer(R.id.widget_chrono, SystemClock.elapsedRealtime(), null, false)
                return
            }
            val maintenant = System.currentTimeMillis()
            val base = if (compteur == SessionRule.Compteur.RESTE) {
                SystemClock.elapsedRealtime() + (Store.sessionEndsAt(context) - maintenant)
            } else {
                SystemClock.elapsedRealtime() - (maintenant - Store.sessionStartedAt(context))
            }
            views.setChronometerCountDown(R.id.widget_chrono, compteur == SessionRule.Compteur.RESTE)
            views.setChronometer(R.id.widget_chrono, base, null, true)
        }

        private val JOURS = intArrayOf(
            R.id.widget_day_0, R.id.widget_day_1, R.id.widget_day_2, R.id.widget_day_3,
            R.id.widget_day_4, R.id.widget_day_5, R.id.widget_day_6
        )

        /** La lettre du jour, au-dessus de sa pastille. */
        private val NOMS_JOURS = intArrayOf(
            R.id.widget_day_label_0, R.id.widget_day_label_1, R.id.widget_day_label_2, R.id.widget_day_label_3,
            R.id.widget_day_label_4, R.id.widget_day_label_5, R.id.widget_day_label_6
        )

        /** La pastille de chaque jour, une image teinte derrière sa marque. */
        private val FONDS_JOURS = intArrayOf(
            R.id.widget_day_fond_0, R.id.widget_day_fond_1, R.id.widget_day_fond_2, R.id.widget_day_fond_3,
            R.id.widget_day_fond_4, R.id.widget_day_fond_5, R.id.widget_day_fond_6
        )

        /**
         * Les sept jours, comme sur l'accueil : fait (un disque plein et une
         * coche), Shabbat (ש sur un disque voilé), pause (un tiret), à faire
         * (un cercle, franc pour aujourd'hui, pâle pour la suite).
         */
        private fun semaine(context: Context, views: RemoteViews, jours: List<WeekDay>, teintes: Teintes) {
            val format = DateTimeFormatter.ofPattern("EEEEE", context.resources.configuration.locales[0])
            val aujourdhui = LocalDate.now()
            jours.take(7).forEachIndexed { i, jour ->
                val estAujourdhui = jour.date == aujourdhui
                views.setTextViewText(NOMS_JOURS[i], jour.date.format(format))
                views.setTextColor(NOMS_JOURS[i], if (estAujourdhui) teintes.encre else teintes.douce)
                val (forme, teinte, opacite) = when {
                    jour.done -> Triple(R.drawable.bg_widget_day_done, teintes.jourFait, 255)
                    jour.exempt -> Triple(R.drawable.bg_widget_day_done, teintes.encre, 0x2E)
                    estAujourdhui -> Triple(R.drawable.bg_widget_day_todo, teintes.encre, 255)
                    jour.future -> Triple(R.drawable.bg_widget_day_todo, teintes.encre, 0x55)
                    else -> Triple(R.drawable.bg_widget_day_todo, teintes.encre, 0x8C)
                }
                views.setImageViewResource(FONDS_JOURS[i], forme)
                views.setInt(FONDS_JOURS[i], "setColorFilter", teinte)
                views.setInt(FONDS_JOURS[i], "setImageAlpha", opacite)
                val marque = when {
                    jour.done -> "✓"
                    jour.shabbat && jour.exempt -> context.getString(R.string.week_shabbat_mark)
                    jour.exempt -> "–"
                    else -> ""
                }
                views.setTextViewText(JOURS[i], marque)
                views.setTextColor(JOURS[i], if (jour.done) teintes.surJourFait else teintes.douce)
            }
        }

        /**
         * Un texte de ’hizouk par jour, parmi ceux de l'ouverture de la
         * hitbodedout. Le même toute la journée : la vignette se redessine
         * souvent, et un texte qui changerait à chaque fois ne se lirait
         * jamais jusqu'au bout.
         */
        private fun texteDuJour(context: Context, views: RemoteViews) {
            val chizuk = runCatching { Data.chizuk(context) }.getOrNull() ?: return
            val textes = ChizukRules.forMoment(chizuk, Moment.OUVERTURE, BuildConfig.DEBUG)
            if (textes.isEmpty()) return
            val item = textes[(LocalDate.now().toEpochDay() % textes.size).toInt()]
            views.setTextViewText(R.id.widget_text, ChizukRules.hebrew(chizuk, item))
            views.setTextViewText(R.id.widget_ref, Reference.localise(context, item.ref))
        }

        /** Le rayon des coins, en dp : celui du système depuis Android 12, sinon celui des cartes. */
        private fun rayonDesCoinsDp(context: Context): Float {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val densite = context.resources.displayMetrics.density
                val systeme = runCatching {
                    context.resources.getDimension(android.R.dimen.system_app_widget_background_radius) / densite
                }.getOrNull()
                if (systeme != null && systeme > 0f) return systeme
            }
            return 22f
        }

        /**
         * Le fond de la vignette, en image : le dégradé de la palette, du
         * coin des anneaux vers le foncé, deux grands arcs pâles venus du
         * coin opposé, comme un écho des anneaux, et un liseré clair sur le
         * bord haut. Coins arrondis compris : avant Android 12, rien d'autre
         * ne les arrondirait.
         *
         * Aux proportions de la place, pour que les arcs restent des cercles,
         * et sous VignetteDecor.COTE_MAX pixels : un dégradé n'a pas besoin
         * de plus, et RemoteViews compte la mémoire de ses images.
         */
        private fun fond(context: Context, largeurDp: Float, hauteurDp: Float, teintes: Teintes, rtl: Boolean): Bitmap {
            val densite = context.resources.displayMetrics.density
            val (l, h) = VignetteDecor.pixels(largeurDp, hauteurDp, densite)
            val px = l / largeurDp.coerceAtLeast(1f)
            val image = Bitmap.createBitmap(l, h, Bitmap.Config.ARGB_8888)
            val toile = Canvas(image)
            val largeur = l.toFloat()
            val hauteur = h.toFloat()
            val cadre = RectF(0f, 0f, largeur, hauteur)

            // Le dégradé, en diagonale, du haut du côté des anneaux vers le bas de l'autre côté.
            val depart = if (rtl) largeur else 0f
            val arrivee = if (rtl) 0f else largeur
            toile.drawRect(cadre, Paint().apply {
                isDither = true
                shader = LinearGradient(depart, 0f, arrivee, hauteur, teintes.clair, teintes.fonce, Shader.TileMode.CLAMP)
            })

            // Deux arcs, comme deux anneaux immenses, centrés au-delà du coin bas opposé.
            val m = minOf(largeur, hauteur)
            val cx = if (rtl) -m * 0.1f else largeur + m * 0.1f
            val cy = hauteur + m * 0.15f
            val trait = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
            trait.strokeWidth = m * 0.10f
            trait.color = VignetteDecor.opacite(VignetteDecor.BLANC, VignetteDecor.ARC_EXTERIEUR)
            toile.drawCircle(cx, cy, m * 0.95f, trait)
            trait.strokeWidth = m * 0.07f
            trait.color = VignetteDecor.opacite(VignetteDecor.BLANC, VignetteDecor.ARC_INTERIEUR)
            toile.drawCircle(cx, cy, m * 0.62f, trait)

            // Le liseré : un filet clair en haut, qui s'éteint vers le bas.
            val rayon = rayonDesCoinsDp(context) * px
            val filet = px.coerceAtLeast(1f)
            val bord = RectF(filet / 2, filet / 2, largeur - filet / 2, hauteur - filet / 2)
            toile.drawRoundRect(bord, rayon, rayon, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = filet
                shader = LinearGradient(
                    0f, 0f, 0f, hauteur * 0.6f,
                    VignetteDecor.opacite(VignetteDecor.BLANC, VignetteDecor.LISERE), Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
            })

            // Les coins : tout ce qui dépasse l'arrondi est effacé, bord lissé.
            val dehors = Path().apply {
                addRoundRect(cadre, rayon, rayon, Path.Direction.CW)
                fillType = Path.FillType.INVERSE_WINDING
            }
            toile.drawPath(dehors, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            })
            return image
        }

        /** Le côté des anneaux, en dp, à la place qu'ils ont dans chaque gabarit. */
        private fun coteJauge(taille: WidgetTaille, largeurDp: Float, hauteurDp: Float): Float = when (taille) {
            WidgetTaille.PETITE -> minOf(largeurDp, hauteurDp - 12f) - 16f
            WidgetTaille.MOYENNE -> minOf((largeurDp - 40f) * 0.4f, hauteurDp - 24f)
            WidgetTaille.GRANDE -> minOf((largeurDp - 44f) * 0.4f, 116f)
        }.coerceAtLeast(40f)

        /**
         * Les trois anneaux de l'accueil, le hassid en leur creux, sur un
         * disque sombre qui les détache du dégradé, dans un halo clair.
         * Dessinés ici en image : une vignette n'accepte pas de vue sur
         * mesure. Les couleurs suivent la palette choisie (Teintes).
         */
        private fun jauge(
            context: Context,
            etat: Anneaux.Etat,
            personnage: Int,
            teintes: Teintes,
            coteDp: Float,
            rtl: Boolean
        ): Bitmap {
            val densite = context.resources.displayMetrics.density
            val cote = (coteDp * densite).toInt().coerceIn(96, 440)
            val image = Bitmap.createBitmap(cote, cote, Bitmap.Config.ARGB_8888)
            val toile = Canvas(image)
            val centre = cote / 2f

            // Le halo, qui s'éteint au bord de l'image.
            val voileHalo = VignetteDecor.opacite(VignetteDecor.BLANC, VignetteDecor.HALO)
            toile.drawCircle(centre, centre, centre, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    centre, centre, centre,
                    intArrayOf(voileHalo, voileHalo, Color.TRANSPARENT),
                    floatArrayOf(0f, 0.8f, 1f),
                    Shader.TileMode.CLAMP
                )
            })

            // Les anneaux, un peu en retrait du bord, sur leur disque sombre.
            val retrait = cote * 0.06f
            val carre = RectF(retrait, retrait, cote - retrait, cote - retrait)
            val s = carre.width()
            toile.drawOval(carre, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = VignetteDecor.opacite(VignetteDecor.NOIR, VignetteDecor.VOILE_ANNEAUX)
            })
            val geometrie = AnneauxDessin.Geometrie(epaisseur = s * 0.062f, ecart = s * 0.016f)
            AnneauxDessin.dessiner(toile, carre, etat, teintes.anneaux, geometrie, rtl = rtl)

            // La plaque claire, puis le hassid, découpé au bord du disque
            // dans un calque : un découpage par chemin laisserait des marches.
            val marge = geometrie.largeur + s * 0.02f
            val plaque = RectF(carre.left + marge, carre.top + marge, carre.right - marge, carre.bottom - marge)
            toile.saveLayer(plaque, null)
            toile.drawOval(plaque, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = teintes.plaque })
            dessin(context, personnage, (plaque.height() * 1.4f).toInt())?.let { perso ->
                toile.drawBitmap(perso, null, cadrage(perso, plaque), Paint(Paint.FILTER_BITMAP_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                })
            }
            toile.restore()
            return image
        }

        /**
         * Le hassid en plan rapproché : de la tête aux genoux (les trois
         * quarts de sa silhouette), le haut de la tête juste sous le bord de
         * la plaque, la coupe au point le plus bas du disque, où elle ne se
         * voit pas. En pied, il tenait dans un disque deux fois trop grand
         * pour lui. Les illustrations ont des marges transparentes inégales :
         * on cadre sur ce qui est dessiné.
         */
        private fun cadrage(perso: Bitmap, plaque: RectF): RectF {
            val corps = silhouette(perso)
            val echelle = plaque.height() * 0.94f / (corps.height() * 0.78f)
            val haut = plaque.top + plaque.height() * 0.06f - corps.top * echelle
            val gauche = plaque.centerX() - corps.centerX() * echelle
            return RectF(gauche, haut, gauche + perso.width * echelle, haut + perso.height * echelle)
        }

        /** Le rectangle de ce qui est dessiné dans l'illustration, hors de ses marges transparentes. */
        private fun silhouette(image: Bitmap): RectF {
            val l = image.width
            val h = image.height
            val points = IntArray(l * h)
            image.getPixels(points, 0, l, 0, 0, l, h)
            var gauche = l
            var droite = -1
            var haut = h
            var bas = -1
            for (y in 0 until h) {
                for (x in 0 until l) {
                    if ((points[y * l + x] ushr 24) > 16) {
                        if (x < gauche) gauche = x
                        if (x > droite) droite = x
                        if (y < haut) haut = y
                        if (y > bas) bas = y
                    }
                }
            }
            return if (bas < 0) RectF(0f, 0f, l.toFloat(), h.toFloat())
            else RectF(gauche.toFloat(), haut.toFloat(), droite + 1f, bas + 1f)
        }

        /** L'illustration, réduite à la lecture : les originaux font 900 px de haut. */
        private fun dessin(context: Context, resDemande: Int, hauteurVoulue: Int): Bitmap? = runCatching {
            val res = Personnage.pour(context, resDemande)
            val bornes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, res, bornes)
            var echelle = 1
            while (bornes.outHeight / (echelle * 2) >= hauteurVoulue) echelle *= 2
            BitmapFactory.decodeResource(
                context.resources, res, BitmapFactory.Options().apply { inSampleSize = echelle }
            )
        }.getOrNull()

        private fun activity(context: Context, code: Int, intent: Intent): PendingIntent =
            PendingIntent.getActivity(
                context,
                code,
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
    }
}
