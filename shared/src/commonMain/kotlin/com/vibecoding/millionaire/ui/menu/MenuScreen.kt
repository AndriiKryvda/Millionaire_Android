package com.vibecoding.millionaire.ui.menu

import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource

import org.jetbrains.compose.resources.StringResource

import com.vibecoding.millionaire.di.AppGraph
import com.vibecoding.millionaire.ui.common.appViewModel

import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecoding.millionaire.generated.resources.Res
import com.vibecoding.millionaire.generated.resources.*
import com.vibecoding.millionaire.ui.common.CardPanel
import com.vibecoding.millionaire.ui.common.ConfirmDialog
import com.vibecoding.millionaire.ui.common.GhostButton
import com.vibecoding.millionaire.ui.common.GoldButton
import com.vibecoding.millionaire.ui.common.LabeledValue
import com.vibecoding.millionaire.ui.common.ScreenScaffold
import com.vibecoding.millionaire.ui.common.formatGroups
import com.vibecoding.millionaire.ui.theme.Gold
import com.vibecoding.millionaire.ui.theme.TextSecondary

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MenuScreen(
    playerId: Long,
    graph: AppGraph,
    onPlay: () -> Unit,
    onRecords: () -> Unit,
    onRules: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Boolean,
    onExitApp: () -> Unit,
    viewModel: MenuViewModel = appViewModel {
        MenuViewModel(
            playerId = playerId,
            playerRepository = graph.playerRepository,
            gameResultRepository = graph.gameResultRepository,
            questionBankRepository = graph.questionBankRepository,
            checkGameAvailability = graph.checkGameAvailability,
            resetGameHistory = graph.resetGameHistory,
        )
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val player = state.player
    var showExitDialog by remember { mutableStateOf(false) }

    // System back: return to the player list when there is one, otherwise ask about closing the app.
    BackHandler(enabled = !showExitDialog) { if (!onBack()) showExitDialog = true }

    LaunchedEffect(state.navigateToGame) {
        if (state.navigateToGame) {
            onPlay()
            // The flag is one-shot: without clearing it, coming back from the game opens a new game.
            viewModel.onGameNavigated()
        }
    }

    ScreenScaffold(
        titleRes = Res.string.title_menu,
        // Nothing to pop back to means the menu is the root screen, so offer to close the app.
        onBack = { if (!onBack()) showExitDialog = true },
    ) {
        CardPanel {
            Text(
                text = player?.nickname?.let { stringResource(Res.string.menu_greeting, it) }
                    ?: stringResource(Res.string.loading),
                style = MaterialTheme.typography.headlineMedium,
                color = Gold,
            )
            player?.let {
                Text(
                    text = stringResource(Res.string.player_age_group, stringResource(it.ageGroup.titleRes)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
            Text(
                text = if (state.bankError) {
                    stringResource(Res.string.bank_load_error)
                } else {
                    stringResource(Res.string.menu_available_questions, state.availableQuestions, state.totalQuestions)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        CardPanel {
            LabeledValue(
                labelRes = Res.string.stat_games,
                value = pluralStringResource(Res.plurals.games_count, state.stats.games, state.stats.games),
            )
            LabeledValue(
                labelRes = Res.string.stat_best_prize,
                value = stringResource(Res.string.prize_value, formatGroups(state.stats.bestPrize)),
            )
            LabeledValue(
                labelRes = Res.string.stat_total_prize,
                value = stringResource(Res.string.prize_value, formatGroups(state.stats.totalPrize)),
            )
            LabeledValue(
                labelRes = Res.string.stat_wins,
                value = state.stats.wins.toString(),
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            GoldButton(textRes = Res.string.action_new_game, onClick = viewModel::startGame, modifier = Modifier.fillMaxWidth())
            GhostButton(textRes = Res.string.action_records, onClick = onRecords, modifier = Modifier.fillMaxWidth())
            GhostButton(textRes = Res.string.action_rules, onClick = onRules, modifier = Modifier.fillMaxWidth())
            GhostButton(textRes = Res.string.action_settings, onClick = onSettings, modifier = Modifier.fillMaxWidth())
        }

        Text(
            text = stringResource(Res.string.menu_version, graph.appVersion),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (state.showNotEnoughDialog) {
        ConfirmDialog(
            titleRes = Res.string.dialog_not_enough_title,
            messageRes = Res.string.dialog_not_enough_message,
            confirmRes = Res.string.dialog_reset_and_play,
            dismissRes = Res.string.action_cancel,
            onConfirm = viewModel::resetHistoryAndPlay,
            onDismiss = viewModel::dismissNotEnoughDialog,
        )
    }

    if (showExitDialog) {
        ConfirmDialog(
            titleRes = Res.string.app_name,
            messageRes = Res.string.confirm_exit_app,
            confirmRes = Res.string.action_exit,
            dismissRes = Res.string.action_cancel,
            onConfirm = onExitApp,
            onDismiss = { showExitDialog = false },
        )
    }
}
