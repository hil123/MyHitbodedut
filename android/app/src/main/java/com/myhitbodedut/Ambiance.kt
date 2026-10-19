package com.myhitbodedut

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.annotation.StringRes
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin

/**
 * Les ambiances (proposition 9 de la feuille de route, 19/10) : pluie, mer,
 * forêt, feu, nuit, ou le silence.
 *
 * Aucune n'est un fichier : l'audio embarqué est au plafond (130 Mo, voir
 * musique/README.md), et chaque ambiance enregistrée se serait payée en
 * mégaoctets. Elles sont donc calculées, une boucle de 24 secondes générée à
 * la demande (bruit filtré, gouttes, vagues, oiseaux, crépitements, grillons),
 * et jouées en boucle sous la musique, pendant la hitbodedout. Le web calcule
 * les mêmes (web/src/js/ambiance.js).
 */
enum class Ambiance(val cle: String, @StringRes val titre: Int) {
    SILENCE("silence", R.string.ambiance_silence),
    PLUIE("pluie", R.string.ambiance_pluie),
    MER("mer", R.string.ambiance_mer),
    FORET("foret", R.string.ambiance_foret),
    FEU("feu", R.string.ambiance_feu),
    NUIT("nuit", R.string.ambiance_nuit);

    companion object {
        fun of(cle: String?): Ambiance = entries.firstOrNull { it.cle == cle } ?: SILENCE
    }
}

/** Le calcul, sans appareil : vérifiable par les tests. */
object AmbianceSynth {

    const val TAUX = 22_050
    const val SECONDES = 24

    /** Un générateur pseudo-aléatoire simple et reproductible (mulberry32), le même que sur le web. */
    private class Hasard(graine: Int) {
        private var a = graine
        fun suivant(): Double {
            a += 0x6D2B79F5
            var t = a
            t = (t xor (t ushr 15)) * (t or 1)
            t = t xor (t + ((t xor (t ushr 7)) * (t or 61)))
            return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL) / 4294967296.0
        }
        fun bruit() = suivant() * 2 - 1
    }

    fun generer(type: Ambiance, taux: Int = TAUX, secondes: Int = SECONDES, graine: Int = 7): FloatArray {
        val n = taux * secondes
        val s = FloatArray(n)
        if (type == Ambiance.SILENCE) return s
        val h = Hasard(graine)
        var bas = 0.0      // passe-bas
        var brun = 0.0     // bruit brun
        var goutte = 0.0   // enveloppe d'une goutte, d'un craquement
        var ton = 0.0      // phase d'un chant
        var chant = 0      // échantillons restants d'un chant
        var freq = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / taux
            val w = h.bruit()
            bas += 0.35 * (w - bas)
            brun = (brun + 0.02 * w) * 0.995
            val v = when (type) {
                Ambiance.PLUIE -> {
                    if (h.suivant() < 0.0008) goutte = 0.25 + 0.35 * h.suivant()
                    goutte *= 0.985
                    0.22 * (w - bas) + 0.18 * bas + goutte * h.bruit()
                }
                Ambiance.MER -> {
                    // Une vague toutes les 8 secondes : 3 par boucle, sans couture.
                    val vague = 0.5 - 0.5 * kotlin.math.cos(2 * PI * t / 8.0)
                    (0.12 + 0.7 * vague * vague) * (1.6 * brun + 0.25 * bas)
                }
                Ambiance.FORET -> {
                    val vent = 0.6 + 0.4 * sin(2 * PI * t / 12.0)
                    if (chant <= 0 && h.suivant() < 1.0 / (taux * 2.5)) {
                        chant = (0.1 * taux).toInt(); freq = 2_600 + 1_200 * h.suivant()
                    }
                    var oiseau = 0.0
                    if (chant > 0) {
                        val p = 1 - chant / (0.1 * taux)
                        ton += 2 * PI * (freq + 900 * p) / taux
                        oiseau = 0.1 * sin(ton) * sin(PI * p)
                        chant--
                    }
                    0.9 * vent * brun + 0.05 * bas + oiseau
                }
                Ambiance.FEU -> {
                    if (h.suivant() < 0.0025) goutte = 0.2 + 0.5 * h.suivant()
                    goutte *= 0.9
                    1.4 * brun + goutte * h.bruit()
                }
                Ambiance.NUIT -> {
                    // Des grillons : trois impulsions de 4,5 kHz, à peu près chaque seconde.
                    val cycle = t % 1.0
                    val impulsion = cycle < 0.15 && (cycle * 20).toInt() % 2 == 0
                    val grillon = if (impulsion) 0.05 * sin(2 * PI * 4_500 * t) else 0.0
                    0.5 * brun + grillon
                }
                Ambiance.SILENCE -> 0.0
            }
            s[i] = v.toFloat()
        }
        // Normalisé à un niveau doux, jamais saturé.
        val crete = s.maxOf { abs(it) }.coerceAtLeast(1e-6f)
        val gain = (0.6f / crete).coerceAtMost(4f)
        for (i in s.indices) s[i] *= gain
        // Fondu de 50 ms aux deux bouts : la boucle ne claque pas à la reprise.
        val f = taux / 20
        for (i in 0 until f) {
            val g = i.toFloat() / f
            s[i] *= g; s[n - 1 - i] *= g
        }
        return s
    }

    fun rms(s: FloatArray): Double = kotlin.math.sqrt(s.sumOf { (it * it).toDouble() } / max(1, s.size))
}

/** La lecture en boucle d'une ambiance, sous la musique. */
class AmbiancePlayer {

    private var piste: AudioTrack? = null
    var courante: Ambiance = Ambiance.SILENCE
        private set

    /** Chaque demande a son numéro : une boucle calculée trop tard pour une ambiance déjà changée est jetée. */
    @Volatile private var demande = 0

    @Synchronized
    fun start(type: Ambiance, volume: Int) {
        if (type == courante) {
            piste?.setVolume(volume / 100f * 0.8f)
            return
        }
        stop()
        courante = type
        if (type == Ambiance.SILENCE) return
        val numero = ++demande
        // Le calcul prend une fraction de seconde : hors du fil principal.
        Thread {
            val f = AmbianceSynth.generer(type)
            val pcm = ShortArray(f.size) { (f[it] * Short.MAX_VALUE).toInt().toShort() }
            synchronized(this) { if (numero == demande && courante == type) jouer(pcm, volume) }
        }.apply { isDaemon = true }.start()
    }

    private fun jouer(pcm: ShortArray, volume: Int) {
        val t = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(AmbianceSynth.TAUX).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        runCatching {
            t.write(pcm, 0, pcm.size)
            t.setLoopPoints(0, pcm.size, -1)
            t.setVolume(volume / 100f * 0.8f)
            t.play()
            piste = t
        }.onFailure { t.release() }
    }

    @Synchronized
    fun stop() {
        demande++
        piste?.let { runCatching { it.stop() }; it.release() }
        piste = null
        courante = Ambiance.SILENCE
    }

    companion object {
        /** L'ambiance choisie, pour la séance en cours ou la prochaine. */
        fun choisie(context: Context): Ambiance = Ambiance.of(Store.ambiance(context))
    }
}
