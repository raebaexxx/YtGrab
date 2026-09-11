package com.raebae.ytdl

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raebae.ytdl.data.SettingsRepository
import com.raebae.ytdl.data.SettingsState
import com.raebae.ytdl.ui.AppRoot
import com.raebae.ytdl.ui.theme.YtdlTheme
import com.raebae.ytdl.util.UrlBridge
import com.raebae.ytdl.util.UrlUtils

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            val repo = remember { SettingsRepository(this) }
            val settings by repo.state.collectAsStateWithLifecycle(initialValue = SettingsState())
            YtdlTheme(settings) {
                AppRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val text = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        } ?: return
        UrlUtils.extractUrl(text)?.let { UrlBridge.post(it) }
    }
}
