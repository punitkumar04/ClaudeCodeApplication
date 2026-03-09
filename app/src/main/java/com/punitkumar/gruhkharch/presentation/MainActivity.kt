package com.punitkumar.gruhkharch.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.punitkumar.gruhkharch.domain.CurrentProjectHolder
import com.punitkumar.gruhkharch.presentation.auth.AuthViewModel
import com.punitkumar.gruhkharch.presentation.navigation.AppNavGraph
import com.punitkumar.gruhkharch.presentation.theme.GruhKharchTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var currentProjectHolder: CurrentProjectHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GruhKharchTheme {
                val authViewModel: AuthViewModel = hiltViewModel()
                val isSignedIn by authViewModel.isSignedIn.collectAsState()

                AppNavGraph(isSignedIn = isSignedIn, currentProjectHolder = currentProjectHolder)
            }
        }
    }
}
