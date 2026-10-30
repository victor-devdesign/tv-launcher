package dev.victor.tvlauncher

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Screen { Home, Apps, Settings }

class MainActivity : ComponentActivity() {
    // Bumped on resume so the system info is re-read when returning from an app.
    private var refresh by mutableIntStateOf(0)
    private var screen by mutableStateOf(Screen.Home)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        route(intent)
        setContent { LauncherScreen(screen, refresh) }
    }

    // singleTask: pressing Home (or Apps) while we are alive lands here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        route(intent)
    }

    override fun onResume() {
        super.onResume()
        refresh++
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ALL_APPS) {
            screen = Screen.Apps
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    @Deprecated("Back leaves the Apps grid, and does nothing on Home")
    override fun onBackPressed() {
        screen = Screen.Home
    }

    // KeyService names the screen; a plain HOME or ALL_APPS intent is the fallback.
    private fun route(intent: Intent?) {
        screen = intent?.getStringExtra(EXTRA_SCREEN)?.let { name -> Screen.values().firstOrNull { it.name == name } }
            ?: if (intent?.action == Intent.ACTION_ALL_APPS) Screen.Apps else Screen.Home
    }

    companion object {
        const val EXTRA_SCREEN = "screen"
    }
}
