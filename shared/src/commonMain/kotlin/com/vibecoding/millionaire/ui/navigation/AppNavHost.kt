package com.vibecoding.millionaire.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.vibecoding.millionaire.di.AppGraph
import com.vibecoding.millionaire.ui.game.GameScreen
import com.vibecoding.millionaire.ui.menu.MenuScreen
import com.vibecoding.millionaire.ui.player.PlayerFormScreen
import com.vibecoding.millionaire.ui.players.PlayersScreen
import com.vibecoding.millionaire.ui.records.RecordsScreen
import com.vibecoding.millionaire.ui.result.ResultScreen
import com.vibecoding.millionaire.ui.rules.RulesScreen
import com.vibecoding.millionaire.ui.settings.SettingsScreen
import com.vibecoding.millionaire.ui.splash.SplashScreen

/**
 * @param onExitApp closes the app (Android: `activity.finish()`, iOS: pop to the root). The shared
 * code must not know about the host activity, so leaving is injected from the platform shell.
 */
@Composable
fun AppNavHost(
    graph: AppGraph,
    navController: NavHostController,
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = SplashRoute,
        modifier = modifier,
    ) {
        composable<SplashRoute> {
            SplashScreen(
                graph = graph,
                onToPlayers = {
                    navController.navigate(PlayersRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                },
                onToMenu = { playerId ->
                    // Keep the player chooser in the back stack so the menu's back arrow returns
                    // to it (pick another player / create a new one) instead of exiting the app.
                    navController.navigate(PlayersRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                    navController.navigate(MenuRoute(playerId))
                },
            )
        }

        composable<PlayersRoute> {
            PlayersScreen(
                graph = graph,
                onOpenPlayer = { playerId -> navController.navigate(MenuRoute(playerId)) },
                onCreatePlayer = { navController.navigate(CreatePlayerRoute) },
                onEditPlayer = { playerId -> navController.navigate(EditPlayerRoute(playerId)) },
            )
        }

        composable<CreatePlayerRoute> {
            PlayerFormScreen(
                playerId = null,
                graph = graph,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        composable<EditPlayerRoute> { entry ->
            PlayerFormScreen(
                playerId = entry.toRoute<EditPlayerRoute>().playerId,
                graph = graph,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        composable<MenuRoute> { entry ->
            val route = entry.toRoute<MenuRoute>()
            MenuScreen(
                playerId = route.playerId,
                graph = graph,
                onPlay = { navController.navigate(GameRoute(route.playerId)) },
                onRecords = { navController.navigate(RecordsRoute) },
                onRules = { navController.navigate(RulesRoute) },
                onSettings = { navController.navigate(SettingsRoute(route.playerId)) },
                onBack = { navController.popBackStack() },
                onExitApp = onExitApp,
            )
        }

        composable<GameRoute> { entry ->
            val route = entry.toRoute<GameRoute>()
            GameScreen(
                playerId = route.playerId,
                graph = graph,
                // Leaving the game always returns to the menu; if the menu is not in the stack, close the app.
                onExit = { if (!navController.popBackStack()) onExitApp() },
                onOpenResult = { resultId ->
                    navController.navigate(ResultRoute(resultId)) {
                        popUpTo<GameRoute> { inclusive = true }
                    }
                },
            )
        }

        composable<ResultRoute> { entry ->
            val route = entry.toRoute<ResultRoute>()
            ResultScreen(
                resultId = route.resultId,
                graph = graph,
                onPlayAgain = { playerId ->
                    navController.navigate(GameRoute(playerId)) {
                        popUpTo<ResultRoute> { inclusive = true }
                    }
                },
                onMenu = { navController.popBackStack<MenuRoute>(inclusive = false) },
            )
        }

        composable<RecordsRoute> {
            RecordsScreen(graph = graph, onBack = { navController.popBackStack() })
        }

        composable<RulesRoute> { RulesScreen(onBack = { navController.popBackStack() }) }

        composable<SettingsRoute> { entry ->
            SettingsScreen(
                playerId = entry.toRoute<SettingsRoute>().playerId,
                graph = graph,
                // Always return to the player's main menu, never exit the app.
                onBack = { navController.popBackStack<MenuRoute>(inclusive = false) },
            )
        }
    }
}
