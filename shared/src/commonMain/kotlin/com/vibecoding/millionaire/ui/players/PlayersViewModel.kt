package com.vibecoding.millionaire.ui.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoding.millionaire.domain.model.Player
import com.vibecoding.millionaire.domain.repository.PlayerRepository
import com.vibecoding.millionaire.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayersViewModel(
    private val playerRepository: PlayerRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val players: StateFlow<List<Player>> = playerRepository.observePlayers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun open(player: Player) {
        viewModelScope.launch { settingsRepository.setLastPlayerId(player.id) }
    }

    fun delete(player: Player) {
        viewModelScope.launch {
            playerRepository.deletePlayer(player.id)
            if (settingsRepository.lastPlayerId() == player.id) settingsRepository.setLastPlayerId(null)
        }
    }
}