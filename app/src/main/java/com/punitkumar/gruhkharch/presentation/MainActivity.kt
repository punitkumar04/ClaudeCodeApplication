package com.punitkumar.gruhkharch.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.punitkumar.gruhkharch.presentation.auth.AuthViewModel
import com.punitkumar.gruhkharch.presentation.navigation.AppNavGraph
import com.punitkumar.gruhkharch.presentation.theme.GruhKharchTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GruhKharchTheme {
                val authViewModel: AuthViewModel = hiltViewModel()
                val isSignedIn by authViewModel.isSignedIn.collectAsState()

                AppNavGraph(isSignedIn = isSignedIn)
            }
        }
    }
}
