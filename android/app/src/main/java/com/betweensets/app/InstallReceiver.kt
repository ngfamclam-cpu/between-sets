package com.betweensets.app

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build

/** Gets the installer's answer; shows Android's confirm screen when one is needed. */
class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm: Intent = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }) ?: return
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // Opens straight away when the app is on screen; otherwise the notification does it.
                runCatching { ctx.startActivity(confirm) }
                Updater.notify(
                    ctx,
                    ctx.getString(R.string.update_confirm),
                    PendingIntent.getActivity(ctx, 7, confirm, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
                )
            }
            PackageInstaller.STATUS_SUCCESS -> Updater.set(UpdateState.UpToDate)
            PackageInstaller.STATUS_FAILURE_ABORTED -> Updater.set(UpdateState.Failed("Update cancelled. Tap Check for updates to try again."))
            else -> Updater.set(
                UpdateState.Failed("Update failed: " + (intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "unknown error"))
            )
        }
    }

    companion object {
        const val ACTION = "com.betweensets.app.INSTALL_RESULT"
    }
}
