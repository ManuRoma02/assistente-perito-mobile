package com.example.assistenteperitomobile

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.assistenteperitomobile.theme.AssistentePeritoMobileTheme
import com.example.assistenteperitomobile.ui.AssistentePeritoApp
import com.example.assistenteperitomobile.ui.PeritoViewModel

class MainActivity : ComponentActivity() {

    private val peritoViewModel: PeritoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            peritoViewModel.handleIncomingShareIntent(intent)
        }

        setContent {
            AssistentePeritoMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AssistentePeritoApp(viewModel = peritoViewModel)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isAppInForeground = true
        peritoViewModel.refreshEasterEggForAppOpen()
    }

    override fun onResume() {
        super.onResume()
        peritoViewModel.refreshAllData()
        peritoViewModel.checkAndTriggerMorningBriefingOnFirstLaunch()
    }

    override fun onStop() {
        super.onStop()
        isAppInForeground = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        peritoViewModel.handleIncomingShareIntent(intent)
    }

    companion object {
        @Volatile
        var isAppInForeground: Boolean = false
    }
}
