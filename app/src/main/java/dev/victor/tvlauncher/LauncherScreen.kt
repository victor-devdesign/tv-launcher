package dev.victor.tvlauncher

import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/** [customArt] draws our own tile instead of borrowing the app's banner or icon. */
private data class Shortcut(val label: String, val pkg: String, val customArt: Boolean = false)

private const val COLUMNS = 4

private const val SPACEDESK_PKG = "ph.spacedesk.beta"

/** Extra entries on the Apps screen, listed only when the app is installed. */
private val EXTRAS = listOf(
    Shortcut("Play Store", "com.android.vending"),
    Shortcut("Tela do PC", SPACEDESK_PKG, customArt = true),
)

private fun isInstalled(context: Context, pkg: String): Boolean =
    try {
        context.packageManager.getApplicationInfo(pkg, 0)
        true
    } catch (_: Exception) {
        false
    }

private val SHORTCUTS = listOf(
    Shortcut("Netflix", "com.netflix.ninja"),
    Shortcut("Crunchyroll", "com.crunchyroll.crunchyroid"),
    Shortcut("Prime Video", "com.amazon.amazonvideo.livingroom"),
    Shortcut("Disney+", "com.disney.disneyplus"),
    Shortcut("HBO Max", "com.hbo.hbonow"),
    Shortcut("YouTube", "com.google.android.youtube.tv"),
)

private val Background = Color(0xFF0B0D12)
private val Surface = Color(0xFF1A1E27)
private val Accent = Color.White
private val TextPrimary = Color(0xFFEDEFF4)
private val TextSecondary = Color(0xFF8D93A3)

@Composable
fun LauncherScreen(screen: Screen, refresh: Int) {
    when (screen) {
        Screen.Home -> HomeScreen(refresh)
        Screen.Apps -> AppsScreen(refresh)
        Screen.Settings -> SettingsScreen()
    }
}

/** Hardware and network readout, refreshed every two seconds. */
@Composable
private fun SettingsScreen() {
    val context = LocalContext.current
    var columns by remember { mutableStateOf(HardwareInfo.columns(context)) }
    val firstButton = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        while (true) {
            columns = withContext(Dispatchers.Default) { HardwareInfo.columns(context) }
            delay(2_000)
        }
    }
    LaunchedEffect(Unit) { firstButton.requestFocus() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 40.dp, vertical = 28.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText("Configurações", style = TextStyle(TextPrimary, 32.sp, FontWeight.Light))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton("Alterar rede", Modifier.focusRequester(firstButton)) {
                    openSystemSettings(context, Settings.ACTION_WIFI_SETTINGS)
                }
                ActionButton("Fechar apps") { closeApps(context) }
                ActionButton("Sistema") { openSystemSettings(context, Settings.ACTION_SETTINGS) }
            }
        }

        Spacer(Modifier.height(28.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            columns.forEach { sections ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    sections.forEach { SectionView(it) }
                }
            }
        }
    }
}

@Composable
private fun SectionView(section: HardwareInfo.Section) {
    Column {
        BasicText(section.title.uppercase(), style = TextStyle(TextSecondary, 12.sp, FontWeight.Medium))
        Spacer(Modifier.height(6.dp))
        section.rows.forEach { (label, value) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BasicText(label, style = TextStyle(TextSecondary, 13.sp))
                BasicText(
                    value,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(TextPrimary, 13.sp, textAlign = TextAlign.End),
                )
            }
        }
    }
}

@Composable
private fun ActionButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) Accent else Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        BasicText(label, style = TextStyle(if (focused) Background else TextPrimary, 16.sp))
    }
}

/**
 * Kills the background processes of every other app that has a launcher entry. Our launcher is in
 * front, so the app the user just left counts as background and goes too.
 */
private fun closeApps(context: Context) {
    val pm = context.packageManager
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val apps = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
        .flatMap { pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(it), 0) }
        .map { it.activityInfo.packageName }
        .toSet() - setOf(context.packageName, "com.android.tv.settings")
    apps.forEach { am.killBackgroundProcesses(it) }
    Toast.makeText(context, "Apps fechados", Toast.LENGTH_SHORT).show()
}

private fun openSystemSettings(context: Context, action: String) {
    try {
        context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Configurações indisponíveis", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun rememberNow(): Date {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(30_000)
        }
    }
    return now
}

/** Minimal home: a big clock and one quiet row of shortcuts. */
@Composable
private fun HomeScreen(refresh: Int) {
    val context = LocalContext.current
    val info = remember(refresh) { SystemInfo.read(context) }
    val now = rememberNow()
    val firstTile = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstTile.requestFocus() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 48.dp, vertical = 32.dp),
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(
                DateFormat.getTimeInstance(DateFormat.SHORT).format(now),
                style = TextStyle(TextPrimary, 96.sp, FontWeight.Light),
            )
            BasicText(
                DateFormat.getDateInstance(DateFormat.FULL).format(now),
                style = TextStyle(TextSecondary, 18.sp),
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SHORTCUTS.forEachIndexed { index, shortcut ->
                val modifier = Modifier
                    .weight(1f)
                    .aspectRatio(16f / 9f)
                Tile(
                    shortcut,
                    if (index == 0) modifier.focusRequester(firstTile) else modifier,
                ) { launch(context, shortcut) }
            }
        }

        Spacer(Modifier.height(24.dp))
        BasicText(
            "${info.storageFree} livres",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = TextStyle(TextSecondary.copy(alpha = 0.6f), 13.sp),
        )
    }
}

/** Full grid of shortcuts with system info, opened by the remote's Apps button. */
@Composable
private fun AppsScreen(refresh: Int) {
    val context = LocalContext.current
    val info = remember(refresh) { SystemInfo.read(context) }
    val now = rememberNow()
    val firstTile = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstTile.requestFocus() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 40.dp, vertical = 28.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            BasicText(
                DateFormat.getTimeInstance(DateFormat.SHORT).format(now),
                style = TextStyle(TextPrimary, 40.sp, FontWeight.Light),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    DateFormat.getDateInstance(DateFormat.FULL).format(now),
                    style = TextStyle(TextSecondary, 16.sp),
                )
                ActionButton("Fechar apps") { closeApps(context) }
            }
        }

        Spacer(Modifier.height(20.dp))

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            val apps = remember { SHORTCUTS + EXTRAS.filter { isInstalled(context, it.pkg) } }
            apps.chunked(COLUMNS).forEachIndexed { rowIndex, row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    row.forEachIndexed { colIndex, shortcut ->
                        val modifier = Modifier
                            .weight(1f)
                            .aspectRatio(16f / 9f)
                        Tile(
                            shortcut,
                            if (rowIndex == 0 && colIndex == 0) modifier.focusRequester(firstTile) else modifier,
                        ) { launch(context, shortcut) }
                    }
                    // Keep the last row's tiles the same size as the rows above.
                    repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Footer(info)
    }
}

@Composable
private fun Tile(shortcut: Shortcut, modifier: Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val art = remember(shortcut.pkg) { if (shortcut.customArt) null else loadArt(context, shortcut.pkg) }
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier
            .graphicsLayer {
                val s = if (focused) 1.04f else 1f
                scaleX = s
                scaleY = s
            }
            .onFocusChanged { focused = it.isFocused }
            .border(if (focused) 3.dp else 0.dp, Accent, shape)
            .clip(shape)
            .background(Surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (shortcut.customArt) MonitorArt()
        if (art != null) {
            Image(
                art.bitmap,
                contentDescription = shortcut.label,
                contentScale = if (art.isBanner) ContentScale.Crop else ContentScale.Fit,
                modifier = if (art.isBanner) Modifier.fillMaxSize() else Modifier.aspectRatio(1f).fillMaxHeight(0.6f),
            )
        }
        if (art == null || !art.isBanner) {
            BasicText(
                shortcut.label,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                style = TextStyle(TextPrimary, 14.sp),
            )
        }
    }
}

@Composable
private fun Footer(info: SystemInfo) {
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Surface),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(info.storageUsedFraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(SolidColor(Accent)),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BasicText(
                "Armazenamento: ${info.storageFree} livres de ${info.storageTotal}",
                style = TextStyle(TextSecondary, 14.sp),
            )
            BasicText(
                "RAM: ${info.ramFree} livres de ${info.ramTotal}",
                style = TextStyle(TextSecondary, 14.sp),
            )
        }
    }
}

/** A plain monitor with a stand, in the launcher's own style. */
@Composable
private fun MonitorArt() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF0E2630), Color(0xFF133C47))))
        val line = Color(0xFF7FDCE8)
        val stroke = 5f
        val width = size.width * 0.30f
        val height = width * 0.6f
        val left = (size.width - width) / 2
        val top = size.height * 0.16f
        drawRoundRect(line, Offset(left, top), Size(width, height), CornerRadius(10f), style = Stroke(stroke))
        val stemEnd = top + height + size.height * 0.07f
        drawLine(line, Offset(size.width / 2, top + height), Offset(size.width / 2, stemEnd), stroke)
        drawLine(
            line,
            Offset(size.width / 2 - width * 0.2f, stemEnd),
            Offset(size.width / 2 + width * 0.2f, stemEnd),
            stroke,
        )
    }
}

private class Art(val bitmap: ImageBitmap, val isBanner: Boolean)

/** Prefers the app's own TV banner, falls back to its icon, null if not installed. */
private fun loadArt(context: Context, pkg: String): Art? {
    val pm = context.packageManager
    return try {
        pm.getApplicationBanner(pkg)?.let { Art(it.toBitmap(320, 180).asImageBitmap(), true) }
            ?: Art(pm.getApplicationIcon(pkg).toBitmap(128, 128).asImageBitmap(), false)
    } catch (_: Exception) {
        null
    }
}

private fun launch(context: Context, shortcut: Shortcut) {
    val pm = context.packageManager
    val intent = pm.getLeanbackLaunchIntentForPackage(shortcut.pkg)
        ?: pm.getLaunchIntentForPackage(shortcut.pkg)
    if (intent == null) {
        Toast.makeText(context, "${shortcut.label} não está instalado", Toast.LENGTH_SHORT).show()
    } else {
        context.startActivity(intent)
    }
}
