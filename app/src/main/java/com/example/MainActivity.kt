package com.example

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.airmind.AirMindApplication
import com.example.airmind.ui.navigation.AirMindNavGraph
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AirMindApplication
        val container = app.appContainer

        // Handle intent if launched via media button
        handleMediaButtonIntent(intent, container)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    AirMindNavGraph(appContainer = container)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val app = application as? AirMindApplication
        app?.appContainer?.let { handleMediaButtonIntent(intent, it) }
    }

    private fun handleMediaButtonIntent(intent: Intent?, container: com.example.airmind.core.di.AppContainer) {
        if (intent?.action == Intent.ACTION_MEDIA_BUTTON) {
            val keyEvent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
            }
            if (keyEvent != null) {
                container.earbudActionHandler.handleMediaKeyEvent(keyEvent)
            }
        }
    }
}
