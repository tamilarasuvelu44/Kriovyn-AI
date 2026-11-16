package com.orbita.personalagent

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.orbita.personalagent.ui.navigation.KriovynNavHost
import com.orbita.personalagent.ui.screens.KriovynViewModelFactory
import com.orbita.personalagent.ui.screens.MainViewModel
import com.orbita.personalagent.ui.theme.KriovynTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        val appContainer = (application as KriovynApplication).container
        val vmFactory = KriovynViewModelFactory(appContainer).factory

        setContent {
            val mainViewModel: MainViewModel = viewModel(factory = vmFactory)
            val prefs by mainViewModel.preferences.collectAsStateWithLifecycle()

            KriovynTheme(themeMode = prefs.themeMode) {
                KriovynNavHost(
                    viewModelFactory = vmFactory,
                    mainViewModel = mainViewModel
                )
            }
        }
    }
}
