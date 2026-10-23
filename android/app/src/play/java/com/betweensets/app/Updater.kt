package com.betweensets.app

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The Play Store updates this build, so the updater does nothing here. */
object Updater {
    const val ENABLED = false
    val state: StateFlow<UpdateState> = MutableStateFlow(UpdateState.Idle)

    fun start(ctx: Context) {}
    fun checkNow(ctx: Context, force: Boolean = false) {}
    fun installNow(ctx: Context) {}
    fun installIfSilent(ctx: Context) {}
}
