package com.vibecoding.millionaire.ui.navigation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vibecoding.millionaire.ui.game.GameScreen
import com.vibecoding.millionaire.ui.menu.MenuScreen
import com.vibecoding.millionaire.ui.player.PlayerFormScreen
import com.vibecoding.millionaire.ui.players.PlayersScreen
import com.vibecoding.millionaire.ui.records.RecordsScreen
import com.vibecoding.millionaire.ui.result.ResultScreen
import com.vibecoding.millionaire.ui.rules.RulesScreen
import com.vibecoding.millionaire.ui.settings.SettingsScreen
import com.vibecoding.millionaire.ui.splash.SplashScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current as? Activity
    NavHost(
        navController = navController,
        startDestination = SplashRoute,
        modifier = modifier,
    ) {
        composable<SplashRoute> {
            SplashScreen(
                onToPlayers = {
                    navController.navigate(PlayersRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                },
                onToMenu = { playerId ->
                    navController.navigate(MenuRoute(playerId)) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                },
            )
        }

        composable<PlayersRoute> {
            PlayersScreen(
                onOpenPlayer = { playerId -> navController.navigate(MenuRoute(playerId)) },
                onCreatePlayer = { navController.navigate(CreatePlayerRoute) },
                onEditPlayer = { playerId -> navController.navigate(EditPlayerRoute(playerId)) },
            )
        }

        composable<CreatePlayerRoute> {
            PlayerFormScreen(onSaved = { navController.popBackStack() }, onBack = { navController.popBackStack() })
        }

        composable<EditPlayerRoute> {
            PlayerFormScreen(onSaved = { navController.popBackStack() }, onBack = { navController.popBackStack() })
        }

        composable<MenuRoute> { entry ->
            val route = entry.toRoute<MenuRoute>()
            MenuScreen(
                onPlay = { navController.navigate(GameRoute(route.playerId)) },
                onRecords = { navController.navigate(RecordsRoute) },
                onRules = { navController.navigate(RulesRoute) },
                onSettings = { navController.navigate(SettingsRoute(route.playerId)) },
                onBack = { navController.popBackStack() },
                onExitApp = { activity?.finish() },
            )
        }

        composable<GameRoute> {
            GameScreen(
                // Leaving the game always returns to the menu; if the menu is not in the stack, close the app.
                onExit = { if (!navController.popBackStack()) activity?.finish() },
                onOpenResult = { resultId ->
                    navController.navigate(ResultRoute(resultId)) {
                        popUpTo<GameRoute> { inclusive = true }
                    }
                },
            )
        }

        composable<ResultRoute> {
            ResultScreen(
                onPlayAgain = { playerId ->
                    navController.navigate(GameRoute(playerId)) {
                        popUpTo<ResultRoute> { inclusive = true }
                    }
                },
                onMenu = { navController.popBackStack<MenuRoute>(inclusive = false) },
            )
        }

        composable<RecordsRoute> { RecordsScreen(onBack = { navController.popBackStack() }) }

        composable<RulesRoute> { RulesScreen(onBack = { navController.popBackStack() }) }

        composable<SettingsRoute> { SettingsScreen(onBack = { navController.popBackStack() }) }
    }
}