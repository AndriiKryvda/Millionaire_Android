package com.vibecoding.millionaire.ui.splash

import org.jetbrains.compose.resources.StringResource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoding.millionaire.generated.resources.Res
import com.vibecoding.millionaire.generated.resources.*
import com.vibecoding.millionaire.domain.repository.PlayerRepository
import com.vibecoding.millionaire.domain.repository.QuestionBankRepository
import com.vibecoding.millionaire.domain.repository.SettingsRepository
import com.vibecoding.millionaire.util.Clock
import com.vibecoding.millionaire.util.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val questionBankRepository: QuestionBankRepository,
    private val playerRepository: PlayerRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock = SystemClock(),
) : ViewModel() {

    sealed interface Destination {
        data object Loading : Destination
        data class ToPlayers(val errorRes: StringResource?) : Destination
        data class ToMenu(val playerId: Long) : Destination
        data class Failure(val messageRes: StringResource) : Destination
    }

    private val _destination = MutableStateFlow<Destination>(Destination.Loading)
    val destination: StateFlow<Destination> = _destination.asStateFlow()

    init {
        load()
    }

    fun load() {
        _destination.value = Destination.Loading
        viewModelScope.launch {
            val startedAt = clock.now()
            val bank = runCatching { questionBankRepository.getBank() }
            if (bank.isFailure) {
                _destination.value = Destination.Failure(Res.string.bank_load_error)
                return@launch
            }
            // Keep the splash visible long enough to avoid a white flash.
            val elapsed = clock.now() - startedAt
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
