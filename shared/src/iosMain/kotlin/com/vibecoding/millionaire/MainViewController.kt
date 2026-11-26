package com.vibecoding.millionaire

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import androidx.navigation.compose.rememberNavController
import com.vibecoding.millionaire.di.AppGraph
import com.vibecoding.millionaire.platform.IosPlatformContext
import com.vibecoding.millionaire.ui.navigation.AppNavHost
import com.vibecoding.millionaire.ui.theme.MillionaireTheme
import platform.UIKit.UIViewController

/**
 * The single entry point consumed by the SwiftUI shell in `iosApp`
 * (`MainViewControllerKt.MainViewController()`).
 *
 * NOTE: like every iOS target of `:shared`, this file only compiles on a macOS host.
 */
fun MainViewController(): UIViewController = ComposeUIViewController { MainScreen() }

@Composable
private fun MainScreen() {
    val graph = remember { AppGraph(IosPlatformContext()) }
    val navController = rememberNavController()
    MillionaireTheme {
        AppNavHost(
            graph = graph,
            navController = navController,
            // iOS forbids programmatic termination; "exit" screens simply stay where they are.
            onExitApp = { },
        )
    }
}
