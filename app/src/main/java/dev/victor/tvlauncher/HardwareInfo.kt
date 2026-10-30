package dev.victor.tvlauncher

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.text.format.Formatter
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File
import java.net.Inet4Address

/** Everything the Settings screen shows, grouped by section. */
object HardwareInfo {
    data class Section(val title: String, val rows: List<Pair<String, String>>)

    private const val NONE = "—"

    /** Three columns of sections, laid out left to right. */
    fun columns(context: Context): List<List<Section>> = listOf(
        listOf(system(), display(context)),
        listOf(processor(), memory(context)),
        listOf(network(context), storage(context)),
    )

    private fun system() = Section(
        "Sistema",
        listOf(
            "Modelo" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "Build" to Build.DISPLAY,
            "Kernel" to (System.getProperty("os.version") ?: NONE),
            "Ligado há" to uptime(),
        ),
    )

    private fun display(context: Context): Section {
        val metrics = DisplayMetrics()
        val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        display.getRealMetrics(metrics)
        return Section(
            "Tela",
            listOf(
                "Resolução" to "${metrics.widthPixels}×${metrics.heightPixels}",
                "Densidade" to "${metrics.densityDpi} dpi",
                "Taxa" to "%.0f Hz".format(display.refreshRate),
            ),
        )
    }

    private fun processor(): Section {
        val cur = readLong("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")
        val max = readLong("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
        val rows = mutableListOf(
            "Arquitetura" to Build.SUPPORTED_ABIS.firstOrNull().orEmpty().ifEmpty { NONE },
            "Núcleos" to Runtime.getRuntime().availableProcessors().toString(),
            "Frequência" to if (cur != null && max != null) "${cur / 1000} / ${max / 1000} MHz" else NONE,
        )
        return Section("Processador", rows)
    }

    private fun memory(context: Context): Section {
        val info = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(info)
        val used = info.totalMem - info.availMem
        val swapTotal = meminfoKb("SwapTotal")
        val swapFree = meminfoKb("SwapFree")
        return Section(
            "Memória",
            listOf(
                "Total" to size(context, info.totalMem),
                "Em uso" to "${size(context, used)} (${percent(used, info.totalMem)})",
                "Livre" to size(context, info.availMem),
                "Swap em uso" to if (swapTotal != null && swapFree != null) {
                    size(context, (swapTotal - swapFree) * 1024)
                } else {
                    NONE
                },
            ),
        )
    }

    private fun storage(context: Context): Section {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes
        val free = stat.availableBytes
        return Section(
            "Armazenamento",
            listOf(
                "Total" to size(context, total),
                "Em uso" to "${size(context, total - free)} (${percent(total - free, total)})",
                "Livre" to size(context, free),
            ),
        )
    }

    private fun network(context: Context): Section {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(net)
        val link = cm.getLinkProperties(net)

        val type = when {
            caps == null -> "Desconectado"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Outra"
        }
        val rows = mutableListOf(
            "Conexão" to type,
            "IP" to (link?.linkAddresses?.map { it.address }?.firstOrNull { it is Inet4Address }?.hostAddress ?: NONE),
            "Gateway" to (link?.routes?.firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }
                ?.gateway?.hostAddress ?: NONE),
            "DNS" to (link?.dnsServers?.firstOrNull()?.hostAddress ?: NONE),
        )
        if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
            val wifi = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo
            val ssid = wifi.ssid?.trim('"')?.takeIf { it.isNotEmpty() && it != WifiManager.UNKNOWN_SSID }
            rows += "Rede" to (ssid ?: NONE)
            rows += "Sinal" to "${wifi.rssi} dBm"
            rows += "Velocidade" to "${wifi.linkSpeed} Mbps"
            rows += "Banda" to "%.1f GHz".format(wifi.frequency / 1000.0)
        }
        return Section("Rede", rows)
    }

    private fun uptime(): String {
        val minutes = SystemClock.elapsedRealtime() / 60_000
        return if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"
    }

    private fun size(context: Context, bytes: Long): String = Formatter.formatFileSize(context, bytes)

    private fun percent(part: Long, total: Long): String =
        if (total > 0) "${(part * 100 / total)}%" else NONE

    private fun meminfoKb(key: String): Long? =
        readText("/proc/meminfo")?.lineSequence()
            ?.firstOrNull { it.startsWith("$key:") }
            ?.filter { it.isDigit() }?.toLongOrNull()

    private fun readText(path: String): String? = try {
        File(path).readText()
    } catch (_: Exception) {
        null
    }

    private fun readLong(path: String): Long? = readText(path)?.trim()?.toLongOrNull()
}
