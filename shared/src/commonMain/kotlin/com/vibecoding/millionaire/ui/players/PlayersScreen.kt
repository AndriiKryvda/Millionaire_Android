package com.vibecoding.millionaire.ui.players

import org.jetbrains.compose.resources.stringResource

import org.jetbrains.compose.resources.StringResource

import com.vibecoding.millionaire.ui.common.formatDate

import com.vibecoding.millionaire.di.AppGraph
import com.vibecoding.millionaire.ui.common.appViewModel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecoding.millionaire.generated.resources.Res
import com.vibecoding.millionaire.generated.resources.*
import com.vibecoding.millionaire.domain.model.Player
import com.vibecoding.millionaire.ui.common.CardPanel
import com.vibecoding.millionaire.ui.common.ConfirmDialog
import com.vibecoding.millionaire.ui.common.EmptyState
import com.vibecoding.millionaire.ui.common.ScreenScaffold
import com.vibecoding.millionaire.ui.theme.Gold
import com.vibecoding.millionaire.ui.theme.TextSecondary

@Composable
fun PlayersScreen(
    graph: AppGraph,
    onOpenPlayer: (Long) -> Unit,
    onCreatePlayer: () -> Unit,
    onEditPlayer: (Long) -> Unit,
    viewModel: PlayersViewModel = appViewModel {
        PlayersViewModel(graph.playerRepository, graph.settingsRepository)
    },
) {
    val players by viewModel.players.collectAsStateWithLifecycle()
    var playerToDelete by remember { mutableStateOf<Player?>(null) }

    ScreenScaffold(titleRes = Res.string.title_players, scroll = false) {
        if (players.isEmpty()) {
            EmptyState(textRes = Res.string.players_empty)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                players.forEach { player ->
                    PlayerCard(
                        player = player,
                        onClick = {
                            viewModel.open(player)
                            onOpenPlayer(player.id)
                        },
                        onEdit = { onEditPlayer(player.id) },
                        onDelete = { playerToDelete = player },
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onCreatePlayer,
            containerColor = Gold,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Text(stringResource(Res.string.action_new_player), style = MaterialTheme.typography.titleMedium)
        }
    }

    playerToDelete?.let { player ->
        ConfirmDialog(
            titleRes = Res.string.dialog_delete_player_title,
            messageRes = Res.string.dialog_delete_player_message,
            confirmRes = Res.string.action_delete,
            dismissRes = Res.string.action_cancel,
            onConfirm = {
                viewModel.delete(player)
                playerToDelete = null
            },
            onDismiss = { playerToDelete = null },
        )
    }
}

@Composable
private fun PlayerCard(
    player: Player,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    CardPanel(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = player.nickname, style = MaterialTheme.typography.titleLarge, color = Gold)
                Text(
                    text = stringResource(Res.string.player_age_group, stringResource(player.ageGroup.titleRes)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Text(
                    text = player.lastPlayedAt?.let {
                        stringResource(Res.string.player_last_played, formatDate(it))
                    } ?: stringResource(Res.string.player_never_played),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            Box {
                TextButton(onClick = { menuOpen = true }) {
                    Text(text = stringResource(Res.string.action_menu), color = Gold)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.action_edit)) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.action_delete), color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}
