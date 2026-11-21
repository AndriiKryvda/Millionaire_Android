package com.vibecoding.millionaire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.vibecoding.millionaire.ui.navigation.AppNavHost
import com.vibecoding.millionaire.ui.theme.MillionaireTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MillionaireTheme {
                AppNavHost(
                    navController = rememberNavController(),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}