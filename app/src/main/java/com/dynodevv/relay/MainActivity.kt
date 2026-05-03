package com.dynodevv.relay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.dynodevv.relay.data.repository.SettingsRepository
import com.dynodevv.relay.ui.theme.RelayTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val (initialDarkTheme, initialDynamicColors) = runBlocking {
            val mode = settingsRepository.themeMode.first()
            val dynamic = settingsRepository.dynamicColors.first()
            val isDark = when (mode) {
                "dark" -> true
                "light" -> false
                else -> null
            }
            isDark to dynamic
        }

        setContent {
            val themeMode by settingsRepository.themeMode.collectAsState(initial = "system")
            val dynamicColors by settingsRepository.dynamicColors.collectAsState(initial = true)
            val systemDarkTheme = isSystemInDarkTheme()

            val isDarkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemDarkTheme
            }

            RelayTheme(
                darkTheme = isDarkTheme,
                dynamicColor = dynamicColors
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RelayAppContent()
                }
            }
        }
    }
}
