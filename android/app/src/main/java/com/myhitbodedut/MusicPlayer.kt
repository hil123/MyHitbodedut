package com.myhitbodedut

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper

/**
 * Lecture d'un morceau pendant la séance. Demande le focus audio, pour que
 * les autres applications se taisent, et s'arrête en fondu.
 */
class MusicPlayer(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var volume = 1f

    val isPlaying: Boolean get() = player?.isPlaying == true

    fun start(track: Track, volumePercent: Int, loop: Boolean = true): Boolean {
        stop(fade = false)
        // Les attributs audio se posent avant prepare(), sans quoi certains
        // appareils lèvent une exception d'état.
        val created = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(attributes())
                if (track.isBundled) {
                    context.resources.openRawResourceFd(track.rawRes).use { fd ->
                        setDataSource(fd.fileDescriptor, fd.startOffset, fd.declaredLength)
                    }
                } else {
                    setDataSource(context, track.uri!!)
                }
                prepare()
            }
        }.getOrNull() ?: return false

        created.isLooping = loop
        player = created
        setVolume(volumePercent)

        if (!requestFocus()) {
            stop(fade = false)
            return false
        }
        return runCatching { created.start(); true }.getOrDefault(false)
    }

    fun setVolume(percent: Int) {
        volume = (percent.coerceIn(0, 100) / 100f)
        runCatching { player?.setVolume(volume, volume) }
    }

    fun stop(fade: Boolean = true) {
        val current = player ?: run { abandonFocus(); return }
        player = null
        if (!fade) {
            release(current)
            return
        }
        // Fondu court : couper net une musique de méditation est désagréable.
        var step = 10
        val level = volume
        val tick = object : Runnable {
            override fun run() {
                if (step <= 0) {
                    release(current)
                    return
                }
                val v = level * step / 10f
                runCatching { current.setVolume(v, v) }
                step--
                handler.postDelayed(this, 60L)
            }
        }
        handler.post(tick)
    }

    private fun release(target: MediaPlayer) {
        runCatching { if (target.isPlaying) target.stop() }
        runCatching { target.release() }
        abandonFocus()
    }

    private fun audioManager(): AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private fun attributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private fun requestFocus(): Boolean {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes())
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS -> stop(fade = false)
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> runCatching { player?.pause() }
                    AudioManager.AUDIOFOCUS_GAIN -> runCatching { player?.start() }
                }
            }
            .build()
        focusRequest = request
        return audioManager().requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        focusRequest?.let { runCatching { audioManager().abandonAudioFocusRequest(it) } }
        focusRequest = null
    }
}
