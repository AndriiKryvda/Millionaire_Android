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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val graph = (application as MillionaireApp).graph
        setContent {
            MillionaireTheme {
                AppNavHost(
                    graph = graph,
                    navController = rememberNavController(),
                    onExitApp = { finish() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}