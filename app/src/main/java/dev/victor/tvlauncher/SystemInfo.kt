package dev.victor.tvlauncher

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter

data class SystemInfo(
    val storageFree: String,
    val storageTotal: String,
    val storageUsedFraction: Float,
    val ramFree: String,
    val ramTotal: String,
) {
    companion object {
        fun read(context: Context): SystemInfo {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.totalBytes
            val free = stat.availableBytes

            val mem = ActivityManager.MemoryInfo()
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mem)

            return SystemInfo(
                storageFree = Formatter.formatFileSize(context, free),
                storageTotal = Formatter.formatFileSize(context, total),
                storageUsedFraction = if (total > 0) 1f - free.toFloat() / total else 0f,
                ramFree = Formatter.formatFileSize(context, mem.availMem),
                ramTotal = Formatter.formatFileSize(context, mem.totalMem),
            )
        }
    }
}
