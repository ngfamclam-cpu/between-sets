package com.betweensets.app

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.AudioManager
import android.media.ToneGenerator

object Chime {
    private val alarmAudio: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    /** Plays the end-of-rest chime at alarm volume, so it sounds with the phone locked or on silent media. */
    fun play(ctx: Context, done: () -> Unit) {
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(alarmAudio)
            ctx.resources.openRawResourceFd(R.raw.chime).use {
                mp.setDataSource(it.fileDescriptor, it.startOffset, it.length)
            }
            mp.setOnCompletionListener { it.release(); done() }
            mp.setOnErrorListener { p, _, _ -> p.release(); done(); true }
            mp.prepare()
            mp.start()
        } catch (e: Exception) {
            mp.release()
            done()
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
