package com.vibecoding.millionaire.ui.players

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecoding.millionaire.R
import com.vibecoding.millionaire.domain.model.Player
import com.vibecoding.millionaire.ui.common.CardPanel
import com.vibecoding.millionaire.ui.common.ConfirmDialog
import com.vibecoding.millionaire.ui.common.EmptyState
import com.vibecoding.millionaire.ui.common.ScreenScaffold
import com.vibecoding.millionaire.ui.theme.Gold
import com.vibecoding.millionaire.ui.theme.TextSecondary
import java.text.DateFormat
import java.util.Date

@Composable
fun PlayersScreen(
    onOpenPlayer: (Long) -> Unit,
    onCreatePlayer: () -> Unit,
    onEditPlayer: (Long) -> Unit,
    viewModel: PlayersViewModel = hiltViewModel(),
) {
    val players by viewModel.players.collectAsStateWithLifecycle()
    var playerToDelete by remember { mutableStateOf<Player?>(null) }

    ScreenScaffold(titleRes = R.string.title_players, scroll = false) {
        if (players.isEmpty()) {
            EmptyState(textRes = R.string.players_empty)
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
            Text(stringResource(R.string.action_new_player), style = MaterialTheme.typography.titleMedium)
        }
    }

    playerToDelete?.let { player ->
        ConfirmDialog(
            titleRes = R.string.dialog_delete_player_title,
            messageRes = R.string.dialog_delete_player_message,
            confirmRes = R.string.action_delete,
            dismissRes = R.string.action_cancel,
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
                    text = stringResource(R.string.player_age_group, stringResource(player.ageGroup.titleRes)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Text(
                    text = player.lastPlayedAt?.let {
                        stringResource(R.string.player_last_played, DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)))
                    } ?: stringResource(R.string.player_never_played),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            Box {
                TextButton(onClick = { menuOpen = true }) {
                    Text(text = stringResource(R.string.action_menu), color = Gold)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_edit)) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
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