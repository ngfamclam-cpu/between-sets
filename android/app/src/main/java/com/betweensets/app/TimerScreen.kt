package com.betweensets.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.ceil

private val Bg = Color(0xFF111214)
private val Surface = Color(0xFF1B1D21)
private val Surface2 = Color(0xFF26292F)
private val Line = Color(0xFF34383F)
private val Fg = Color(0xFFEEF0F2)
private val Muted = Color(0xFF9097A1)
private val Accent = Color(0xFFF2C230)
private val AccentInk = Color(0xFF1A1505)
private val Custom = Color(0xFFA48BD6)

private data class Plate(val seconds: Int, val color: Color, val ink: Color, val kg: String)

private val PLATES = listOf(
    Plate(30, Color(0xFF2F9E5B), Color.White, "10 kg"),
    Plate(60, Color(0xFFF2C230), AccentInk, "15 kg"),
    Plate(90, Color(0xFF3A7BD5), Color.White, "20 kg"),
    Plate(120, Color(0xFFD8423A), Color.White, "25 kg"),
    Plate(180, Color(0xFFE9ECEF), Color(0xFF1A1A1A), "5 kg"),
)

private fun colorFor(ms: Long) = PLATES.firstOrNull { it.seconds * 1000L == ms }?.color ?: Custom

private val Display = FontFamily.SansSerif

@Composable
fun TimerScreen() {
    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, onPrimary = AccentInk, background = Bg, surface = Surface)) {
        val ctx = LocalContext.current
        val s by TimerController.state.collectAsState()
        val update by Updater.state.collectAsState()
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        var customs by remember { mutableStateOf(TimerStore.customs(ctx)) }
        var adding by remember { mutableStateOf(false) }
        var notifMode by remember { mutableStateOf(TimerStore.notifMode(ctx)) }
        val notifControls = notifMode != NotifMode.OFF
        var lockControls by remember { mutableStateOf(TimerStore.lockScreenControls(ctx)) }

        LaunchedEffect(Unit) {
            while (true) {
                now = System.currentTimeMillis()
                delay(100)
            }
        }
        val left = s.leftMs(now)
        val secLeft = ceil(left / 1000.0).toInt()
        LaunchedEffect(secLeft, s.status) {
            if (s.status == Status.RUNNING && secLeft in 1..3) Chime.tick()
        }

        Box(Modifier.fillMaxSize().background(Bg)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "BETWEEN SETS", color = Fg, fontSize = 24.sp, fontWeight = FontWeight.Black,
                        fontFamily = Display, letterSpacing = 1.sp, modifier = Modifier.weight(1f),
                    )
                    Text("Sets ", color = Muted, fontSize = 15.sp)
                    Text("${s.sets}", color = Fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { TimerController.clearSets(ctx) }) {
                        Text("clear", color = Muted, fontSize = 13.sp)
                    }
                }

                (update as? UpdateState.Ready)?.let { UpdateBanner(it) { Updater.installNow(ctx) } }

                Ring(
                    fraction = if (s.durationMs > 0) left.toFloat() / s.durationMs else 0f,
                    color = colorFor(s.durationMs),
                    time = formatClock(left),
                    label = when (s.status) {
                        Status.IDLE -> "Ready · ${formatClock(s.durationMs)} rest"
                        Status.RUNNING -> if (s.auto) "Resting · auto-repeat" else "Resting"
                        Status.PAUSED -> "Paused"
                        Status.DONE -> "Done · set ${s.sets}"
                    },
                    done = s.status == Status.DONE,
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { TimerController.stop(ctx) },
                        modifier = Modifier.weight(1f).height(58.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Line),
                    ) { Text("Reset", color = Fg, fontSize = 16.sp) }
                    Button(
                        onClick = { TimerController.toggle(ctx) },
                        modifier = Modifier.weight(1.6f).height(58.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = AccentInk),
                    ) {
                        Text(
                            when (s.status) {
                                Status.IDLE -> "Start"
                                Status.RUNNING -> "Pause"
                                Status.PAUSED -> "Resume"
                                Status.DONE -> "Go again"
                            },
                            fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                    OutlinedButton(
                        onClick = { TimerController.plus(ctx) },
                        enabled = s.status != Status.DONE,
                        modifier = Modifier.weight(1f).height(58.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Line),
                    ) { Text("+15s", color = Fg, fontSize = 16.sp) }
                }

                SettingRow(
                    title = "Auto-start next rest",
                    detail = if (s.auto) "On: the next rest starts as soon as the chime plays"
                    else "Off: tap Go again after each chime",
                    checked = s.auto,
                    onChange = { TimerController.setAuto(ctx, it) },
                )

                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Label("Rest interval · tap to start")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PLATES.forEach { p ->
                            PlateButton(p, selected = s.durationMs == p.seconds * 1000L, Modifier.weight(1f)) {
                                TimerController.start(ctx, p.seconds)
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        customs.forEach { sec ->
                            Chip(
                                text = formatClock(sec * 1000L),
                                selected = s.durationMs == sec * 1000L,
                                onClick = { TimerController.start(ctx, sec) },
                                onRemove = {
                                    customs = customs - sec
                                    TimerStore.setCustoms(ctx, customs)
                                },
                            )
                        }
                        Chip(text = "+ New timer", selected = false, dashed = true, onClick = { adding = true })
                    }
                }

                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Label("Settings")
                    ChoiceRow(
                        title = "Buttons in notifications",
                        detail = when (notifMode) {
                            NotifMode.APP_OPEN -> "Show when you open the app; gone when you swipe it out of recent apps"
                            NotifMode.ALWAYS -> "Always in your notification shade, even after closing the app"
                            NotifMode.OFF -> "Off: only the rest-over alert shows"
                        },
                        options = listOf(
                            NotifMode.APP_OPEN to "While open",
                            NotifMode.ALWAYS to "Always",
                            NotifMode.OFF to "Off",
                        ),
                        selected = notifMode,
                        onSelect = {
                            notifMode = it
                            TimerController.setNotifMode(ctx, it)
                        },
                    )
                    SettingRow(
                        title = "Buttons on lock screen",
                        detail = when {
                            !notifControls -> "Turn on buttons in notifications first"
                            lockControls -> "Use them without unlocking your phone"
                            else -> "Off: unlock to see them in the notification shade"
                        },
                        checked = lockControls && notifControls,
                        enabled = notifControls,
                        onChange = {
                            lockControls = it
                            TimerController.setLockScreenControls(ctx, it)
                        },
                    )
                }

                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Label("Home screen")
                    Text(
                        "Widget: long-press your home screen, tap Widgets, and drag out Between Sets. " +
                            "Long-press the app icon for one-tap 30s to 3:00 shortcuts. " +
                            "The chime plays at alarm volume, even with the phone locked.",
                        color = Muted, fontSize = 14.sp, lineHeight = 20.sp,
                    )
                }

                UpdateFooter(update) { Updater.checkNow(ctx, force = true) }
            }
        }

        if (adding) {
            NewTimerDialog(
                onDismiss = { adding = false },
                onSave = { sec ->
                    adding = false
                    if (sec !in customs && sec !in PRESETS) {
                        customs = customs + sec
                        TimerStore.setCustoms(ctx, customs)
                    }
                    TimerController.start(ctx, sec)
                },
            )
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    detail: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(14.dp))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) Fg else Muted, fontSize = 16.sp)
            Text(detail, color = Muted, fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = Accent, checkedThumbColor = AccentInk),
        )
    }
}

@Composable
private fun <T> ChoiceRow(
    title: String,
    detail: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(14.dp))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column {
            Text(title, color = Fg, fontSize = 16.sp)
            Text(detail, color = Muted, fontSize = 13.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { (value, label) ->
                val on = value == selected
                Box(
                    Modifier
                        .weight(1f)
                        .height(38.dp)
                        .background(if (on) Accent else Surface2, RoundedCornerShape(10.dp))
                        .clickable { onSelect(value) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label, color = if (on) AccentInk else Fg, fontSize = 14.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateBanner(ready: UpdateState.Ready, onInstall: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(14.dp))
            .border(1.dp, Accent, RoundedCornerShape(14.dp))
            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Update ready · version ${ready.version}", color = Fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (ready.needsPermission) "Allow Between Sets to install apps, once, so it can update itself"
                else "Installs when you close the app, or tap Install now",
                color = Muted, fontSize = 13.sp,
            )
        }
        Button(
            onClick = onInstall,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = AccentInk),
        ) { Text(if (ready.needsPermission) "Allow" else "Install") }
    }
}

@Composable
private fun UpdateFooter(update: UpdateState, onCheck: () -> Unit) {
    val ctx = LocalContext.current
    val version = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "?"
    }
    val status = when (update) {
        UpdateState.Idle -> ""
        UpdateState.Checking -> "Checking for updates…"
        UpdateState.UpToDate -> "Up to date"
        is UpdateState.Downloading -> "Downloading version ${update.version} · ${update.percent}%"
        is UpdateState.Ready -> "Version ${update.version} ready to install"
        is UpdateState.Installing -> "Installing version ${update.version}…"
        is UpdateState.Failed -> update.reason
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Version $version", color = Muted, fontSize = 13.sp)
            if (status.isNotEmpty()) Text(status, color = Muted, fontSize = 12.sp)
        }
        TextButton(onClick = onCheck) { Text("Check for updates", color = Accent, fontSize = 13.sp) }
    }
}

@Composable
private fun Label(text: String) {
    Text(text.uppercase(), color = Muted, fontSize = 12.sp, letterSpacing = 2.sp)
}

@Composable
private fun Ring(fraction: Float, color: Color, time: String, label: String, done: Boolean) {
    Box(Modifier.widthIn(max = 340.dp).fillMaxWidth(0.82f).aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.07f
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(Surface2, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            drawArc(
                color, -90f, 360f * fraction.coerceIn(0f, 1f), false, topLeft, arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                time, color = if (done) color else Fg, fontSize = 84.sp, fontWeight = FontWeight.Black,
                fontFamily = Display, lineHeight = 84.sp,
            )
            Text(label.uppercase(), color = Muted, fontSize = 12.sp, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun PlateButton(p: Plate, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .aspectRatio(1f)
            .then(if (selected) Modifier.border(3.dp, Fg, CircleShape).padding(5.dp) else Modifier)
            .background(p.color, CircleShape)
            .border(4.dp, Color.Black.copy(alpha = 0.18f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatClock(p.seconds * 1000L), color = p.ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(p.kg, color = p.ink.copy(alpha = 0.8f), fontSize = 9.sp)
        }
    }
}

@Composable
private fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    dashed: Boolean = false,
    onRemove: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .height(42.dp)
            .background(Surface, RoundedCornerShape(50))
            .border(1.dp, if (selected) Fg else Line, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = if (onRemove != null) 4.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!dashed) {
            Box(Modifier.size(10.dp).background(Custom, CircleShape))
            Spacer(Modifier.size(6.dp))
        }
        Text(text, color = if (dashed) Muted else Fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        if (onRemove != null) {
            Text(
                "×", color = Muted, fontSize = 18.sp,
                modifier = Modifier.clickable(onClick = onRemove).padding(horizontal = 10.dp),
            )
        }
    }
}

@Composable
private fun NewTimerDialog(onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var min by remember { mutableStateOf("2") }
    var sec by remember { mutableStateOf("30") }
    val total = (min.toIntOrNull() ?: 0) * 60 + (sec.toIntOrNull() ?: 0)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New timer") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = min, onValueChange = { min = it.filter(Char::isDigit).take(2) },
                    label = { Text("Min") }, singleLine = true, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = sec, onValueChange = { sec = it.filter(Char::isDigit).take(2) },
                    label = { Text("Sec") }, singleLine = true, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(total) }, enabled = total >= 5) { Text("Save and start") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
