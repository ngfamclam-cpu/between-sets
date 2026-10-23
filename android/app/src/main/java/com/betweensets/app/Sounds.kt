package com.betweensets.app

import android.net.Uri

data class BuiltInSound(val id: String, val label: String, val res: Int)

val BUILT_IN_SOUNDS = listOf(
    BuiltInSound("chime", "Bell chime", R.raw.chime),
    BuiltInSound("boxing", "Boxing bell", R.raw.boxing_bell),
    BuiltInSound("beeps", "Triple beep", R.raw.beeps),
    BuiltInSound("rising", "Rising tones", R.raw.rising),
    BuiltInSound("whistle", "Whistle", R.raw.whistle),
    BuiltInSound("gong", "Gong", R.raw.gong),
)

/** The rest-over sound: a built-in ("res:<id>") or a phone sound or file you picked ("uri:<content uri>"). */
data class SoundChoice(val key: String, val label: String) {
    val builtIn: BuiltInSound? get() = BUILT_IN_SOUNDS.firstOrNull { key == "res:${it.id}" }
    val uri: Uri? get() = if (key.startsWith("uri:")) Uri.parse(key.removePrefix("uri:")) else null

    companion object {
        val DEFAULT = of(BUILT_IN_SOUNDS.first())
        fun of(s: BuiltInSound) = SoundChoice("res:${s.id}", s.label)
        fun of(uri: Uri, label: String) = SoundChoice("uri:$uri", label)
    }
}
