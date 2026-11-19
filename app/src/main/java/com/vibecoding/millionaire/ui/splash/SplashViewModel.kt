package com.vibecoding.millionaire.ui.splash

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoding.millionaire.R
import com.vibecoding.millionaire.domain.repository.PlayerRepository
import com.vibecoding.millionaire.domain.repository.QuestionBankRepository
import com.vibecoding.millionaire.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val questionBankRepository: QuestionBankRepository,
    private val playerRepository: PlayerRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    sealed interface Destination {
        data object Loading : Destination
        data class ToPlayers(val errorRes: Int?) : Destination
        data class ToMenu(val playerId: Long) : Destination
        data class Failure(@StringRes val messageRes: Int) : Destination
    }

    private val _destination = MutableStateFlow<Destination>(Destination.Loading)
    val destination: StateFlow<Destination> = _destination.asStateFlow()

    init {
        load()
    }

    fun load() {
        _destination.value = Destination.Loading
        viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            val bank = runCatching { questionBankRepository.getBank() }
            if (bank.isFailure) {
                _destination.value = Destination.Failure(R.string.bank_load_error)
                return@launch
            }
            // Keep the splash visible long enough to avoid a white flash.
            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed < MIN_SPLASH_MS) delay(MIN_SPLASH_MS - elapsed)

            val lastPlayerId = settingsRepository.lastPlayerId()
            val player = lastPlayerId?.let { playerRepository.getPlayer(it) }
            _destination.value = if (player != null) {
                Destination.ToMenu(player.id)
            } else {
                Destination.ToPlayers(errorRes = null)
            }
        }
    }

    private companion object {
        const val MIN_SPLASH_MS = 700L
    }
}