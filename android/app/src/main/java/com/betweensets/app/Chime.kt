package com.betweensets.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

object Chime {
    /** Long songs or ringtones get cut off here, so the alarm receiver finishes in time. */
    private const val MAX_MS = 7_000L

    private val alarmAudio: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val main = Handler(Looper.getMainLooper())
    private var stopPreview: (() -> Unit)? = null

    /** Plays the chosen rest-over sound at alarm volume, so it sounds with the phone locked or media muted. */
    fun play(ctx: Context, done: () -> Unit) {
        play(ctx, TimerStore.sound(ctx), done)
    }

    /** Settings: play a sound to try it, stopping any other preview. */
    fun preview(ctx: Context, choice: SoundChoice) {
        stopPreview()
        stopPreview = play(ctx.applicationContext, choice) { }
    }

    fun stopPreview() {
        stopPreview?.invoke()
        stopPreview = null
    }

    /** Returns a function that stops it early. Falls back to the bell if a picked sound can't be played. */
    private fun play(ctx: Context, choice: SoundChoice, done: () -> Unit): () -> Unit {
        val mp = MediaPlayer()
        var finished = false
        val finish = {
            if (!finished) {
                finished = true
                runCatching { mp.stop() }
                mp.release()
                done()
            }
        }
        try {
            mp.setAudioAttributes(alarmAudio)
            val builtIn = choice.builtIn
            val uri = choice.uri
            when {
                builtIn != null -> ctx.resources.openRawResourceFd(builtIn.res).use {
                    mp.setDataSource(it.fileDescriptor, it.startOffset, it.length)
                }
                uri != null -> mp.setDataSource(ctx, uri)
                else -> throw IllegalArgumentException("Unknown sound ${choice.key}")
            }
            mp.setOnCompletionListener { finish() }
            mp.setOnErrorListener { _, _, _ -> finish(); true }
            mp.prepare()
            mp.start()
            main.postDelayed({ finish() }, MAX_MS)
            return finish
        } catch (e: Exception) {
            finished = true
            mp.release()
            if (choice != SoundChoice.DEFAULT) return play(ctx, SoundChoice.DEFAULT, done)
            done()
            return { }
        }
    }

    private var tones: ToneGenerator? = null

    /** The 3-2-1 beep, only while the app is open. */
    fun tick() {
        try {
            val t = tones ?: ToneGenerator(AudioManager.STREAM_ALARM, 70).also { tones = it }
            t.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (e: RuntimeException) {
            tones = null
        }
    }
}
