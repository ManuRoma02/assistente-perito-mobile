package com.example.assistenteperitomobile

import android.content.ContentResolver
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.assistenteperitomobile.domain.MainAppTab
import com.example.assistenteperitomobile.theme.AssistentePeritoMobileTheme
import com.example.assistenteperitomobile.ui.AssistentePeritoApp
import com.example.assistenteperitomobile.ui.PeritoViewModel
import kotlinx.coroutines.CancellationException

class MainActivity : ComponentActivity() {

    private val peritoViewModel: PeritoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            handleIntentNavigation(intent)
            handleIncomingShare(intent)
        }

        setContent {
            val uiState by peritoViewModel.uiState.collectAsStateWithLifecycle()
            PredictiveBackHandler(enabled = uiState.selectedTab != MainAppTab.CHAT) { progress ->
                try {
                    progress.collect { }
                    peritoViewModel.selectTab(MainAppTab.CHAT)
                } catch (_: CancellationException) {
                    // Back gesture cancelled by user
                }
            }
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
        handleIntentNavigation(intent)
        handleIncomingShare(intent)
    }

    private fun handleIncomingShare(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_SEND) return
        if (intent.type != "text/plain") {
            @Suppress("DEPRECATION")
            val streamUri = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            if (streamUri != null &&
                (streamUri.scheme != ContentResolver.SCHEME_CONTENT ||
                    streamUri.authority.isNullOrBlank() ||
                    streamUri.authority == "$packageName.fileprovider")
            ) {
                Log.w("MainActivity", "Ignored invalid or internal EXTRA_STREAM URI: $streamUri")
                return
            }
        }
        peritoViewModel.handleIncomingShareIntent(intent)
    }

    private fun handleIntentNavigation(intent: Intent?) {
        if (intent == null) return
        val rawTab = intent.getStringExtra("NAVIGATE_TO_TAB")?.trim()?.uppercase(Locale.ROOT)
        val allowedTabs = setOf("CHAT", "AGENDA", "FASCICOLI")
        val targetTab = rawTab?.takeIf { it in allowedTabs }
        if (rawTab != null && targetTab == null) {
            Log.w("MainActivity", "Ignored invalid NAVIGATE_TO_TAB extra: $rawTab")
        }
        val rawFascicoloId = intent.getStringExtra("FASCICOLO_ID")?.trim()
        val targetFascicoloId = rawFascicoloId?.takeIf {
            it.isNotEmpty() &&
                it.length <= 128 &&
                !it.contains("..") &&
                !it.contains('/') &&
                !it.contains('\\') &&
                it.none { ch -> ch.isISOControl() }
        }
        if (rawFascicoloId != null && targetFascicoloId == null) {
            Log.w("MainActivity", "Ignored invalid FASCICOLO_ID extra")
        }
        if (targetTab != null || targetFascicoloId != null) {
            peritoViewModel.handleNavigationIntent(targetTab, targetFascicoloId)
        }
    }

    companion object {
        @Volatile
        var isAppInForeground: Boolean = false
    }
}
