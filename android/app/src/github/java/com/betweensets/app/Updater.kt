package com.betweensets.app

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Checks GitHub Releases every hour (and when the app opens), downloads a newer APK and installs it
 * with PackageInstaller. Android asks to confirm the first time. Once Between Sets has installed
 * itself, Android 12+ lets later updates install with no prompt. Never installs during a rest.
 */
object Updater {
    const val ENABLED = true
    private const val REPO = "ngfamclam-cpu/between-sets"
    private const val CH_UPDATES = "updates"
    private const val ID_UPDATE = 3

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private var lastCheck = 0L

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state

    internal fun set(s: UpdateState) { _state.value = s }

    fun start(ctx: Context) {
        val request = PeriodicWorkRequestBuilder<UpdateWorker>(1, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("update-check", ExistingPeriodicWorkPolicy.KEEP, request)
        checkNow(ctx)
    }

    /** Checks at most every 10 minutes unless [force]. */
    fun checkNow(ctx: Context, force: Boolean = false) {
        if (!force && System.currentTimeMillis() - lastCheck < 10 * 60 * 1000L) return
        val app = ctx.applicationContext
        scope.launch { run(app, fromBackground = false) }
    }

    /** The Install button: asks for the install permission first if it's off. */
    fun installNow(ctx: Context) {
        val app = ctx.applicationContext
        val ready = _state.value as? UpdateState.Ready ?: return
        if (!app.packageManager.canRequestPackageInstalls()) {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        scope.launch { install(app, ready) }
    }

    /** When the app is closed: install a waiting update if no prompt is needed and no rest is running. */
    fun installIfSilent(ctx: Context) {
        val app = ctx.applicationContext
        val ready = _state.value as? UpdateState.Ready ?: return
        if (ready.file.exists() && canInstallSilently(app) && TimerStore.load(app).status != Status.RUNNING) {
            scope.launch { install(app, ready) }
        }
    }

    suspend fun run(ctx: Context, fromBackground: Boolean) = lock.withLock {
        lastCheck = System.currentTimeMillis()
        if (_state.value is UpdateState.Installing) return@withLock
        val result = try {
            check(ctx)
        } catch (e: Exception) {
            UpdateState.Failed("Couldn't check for updates (${e.javaClass.simpleName})")
        }
        _state.value = result
        if (result is UpdateState.Ready && fromBackground) {
            if (!result.needsPermission && canInstallSilently(ctx) && TimerStore.load(ctx).status != Status.RUNNING) {
                install(ctx, result)
            } else {
                notifyReady(ctx, result.version)
            }
        }
    }

    private fun currentVersion(ctx: Context): Long =
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).longVersionCode

    private fun check(ctx: Context): UpdateState {
        if (_state.value !is UpdateState.Ready) _state.value = UpdateState.Checking
        val release = JSONObject(httpGet("https://api.github.com/repos/$REPO/releases/latest"))
        val version = release.getString("tag_name").removePrefix("v").toIntOrNull()
            ?: return UpdateState.Failed("Unexpected release name ${release.getString("tag_name")}")
        if (version <= currentVersion(ctx)) return UpdateState.UpToDate

        val dir = File(ctx.cacheDir, "updates").apply { mkdirs() }
        val file = File(dir, "BetweenSets-v$version.apk")
        if (!isNewerBuild(ctx, file)) {
            dir.listFiles()?.forEach { it.delete() }
            val assets = release.getJSONArray("assets")
            val url = (0 until assets.length()).map { assets.getJSONObject(it) }
                .firstOrNull { it.getString("name") == "BetweenSets.apk" }
                ?.getString("browser_download_url")
                ?: return UpdateState.Failed("Release v$version has no APK")
            download(url, file, version)
            if (!isNewerBuild(ctx, file)) {
                file.delete()
                return UpdateState.Failed("The download wasn't a newer Between Sets")
            }
        }
        return UpdateState.Ready(version, file, needsPermission = !ctx.packageManager.canRequestPackageInstalls())
    }

    private fun isNewerBuild(ctx: Context, file: File): Boolean {
        if (!file.exists()) return false
        val info = ctx.packageManager.getPackageArchiveInfo(file.path, 0) ?: return false
        return info.packageName == ctx.packageName && info.longVersionCode > currentVersion(ctx)
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            setRequestProperty("User-Agent", "BetweenSets-updater")
            setRequestProperty("Accept", "application/vnd.github+json")
        }

    private fun httpGet(url: String): String {
        val conn = open(url)
        try {
            if (conn.responseCode != 200) throw IllegalStateException("GitHub error ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun download(url: String, file: File, version: Int) {
        val conn = open(url)
        try {
            if (conn.responseCode != 200) throw IllegalStateException("Download error ${conn.responseCode}")
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                file.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    var lastPct = -1
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        val pct = if (total > 0) ((done * 100) / total).toInt() else 0
                        if (pct != lastPct) { lastPct = pct; _state.value = UpdateState.Downloading(version, pct) }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    /** Android lets an app install itself without a prompt only when it was the last installer (Android 12+). */
    private fun canInstallSilently(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !ctx.packageManager.canRequestPackageInstalls()) return false
        val installer = runCatching {
            ctx.packageManager.getInstallSourceInfo(ctx.packageName).installingPackageName
        }.getOrNull()
        return installer == ctx.packageName
    }

    private fun install(ctx: Context, ready: UpdateState.Ready) {
        _state.value = UpdateState.Installing(ready.version)
        try {
            val installer = ctx.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(ctx.packageName)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }
            val sessionId = installer.createSession(params)
            installer.openSession(sessionId).use { session ->
                session.openWrite("base.apk", 0, ready.file.length()).use { out ->
                    ready.file.inputStream().use { it.copyTo(out) }
                    session.fsync(out)
                }
                val intent = Intent(ctx, InstallReceiver::class.java).setAction(InstallReceiver.ACTION)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
                session.commit(PendingIntent.getBroadcast(ctx, sessionId, intent, flags).intentSender)
            }
        } catch (e: Exception) {
            _state.value = UpdateState.Failed("Install failed: ${e.message}")
        }
    }

    private fun channel(ctx: Context): NotificationManager {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_UPDATES, ctx.getString(R.string.channel_updates), NotificationManager.IMPORTANCE_DEFAULT)
        )
        return nm
    }

    /** Tells you once per version that an update is waiting for a tap. */
    @SuppressLint("MissingPermission")
    private fun notifyReady(ctx: Context, version: Int) {
        val prefs = ctx.getSharedPreferences("updates", Context.MODE_PRIVATE)
        if (prefs.getInt("notified", 0) == version) return
        prefs.edit().putInt("notified", version).apply()
        notify(ctx, ctx.getString(R.string.update_ready, version), TimerController.openAppIntent(ctx))
    }

    @SuppressLint("MissingPermission")
    internal fun notify(ctx: Context, text: String, tap: PendingIntent) {
        channel(ctx).notify(
            ID_UPDATE,
            Notification.Builder(ctx, CH_UPDATES)
                .setSmallIcon(R.drawable.ic_stat_timer)
                .setColor(ctx.getColor(R.color.accent))
                .setContentTitle(ctx.getString(R.string.app_name))
                .setContentText(text)
                .setContentIntent(tap)
                .setAutoCancel(true)
                .build(),
        )
    }
}

class UpdateWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        Updater.run(applicationContext, fromBackground = true)
        return Result.success()
    }
}
