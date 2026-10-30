package dev.victor.tvlauncher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent

/**
 * Reads the remote's Home and Apps keys before the system does (Android 9 sends Apps to the system
 * settings package, so the launcher never sees it) and sends each to a screen of this launcher:
 * Home opens the home screen, holding Home opens Settings, Apps opens the apps grid.
 *
 * The Search key cannot be used: the Xiaomi firmware consumes it before accessibility services see it.
 */
class KeyService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var homeDown = false
    private var homeHeld = false
    private val openSettings = Runnable {
        homeHeld = true
        show(Screen.Settings)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_HOME -> onHome(event)
            KeyEvent.KEYCODE_ALL_APPS ->
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) show(Screen.Apps)
            else -> return false
        }
        return true
    }

    // Consumed events are not repeated by the system, so the long press is timed here.
    private fun onHome(event: KeyEvent) {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> if (!homeDown) {
                homeDown = true
                homeHeld = false
                handler.postDelayed(openSettings, ViewConfiguration.getLongPressTimeout().toLong())
            }
            KeyEvent.ACTION_UP -> {
                handler.removeCallbacks(openSettings)
                if (homeDown && !homeHeld) show(Screen.Home)
                homeDown = false
            }
        }
    }

    private fun show(screen: Screen) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(MainActivity.EXTRA_SCREEN, screen.name),
        )
    }
}
