package com.vibecoding.millionaire.ui.player

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoding.millionaire.R
import com.vibecoding.millionaire.domain.game.NicknameValidator
import com.vibecoding.millionaire.domain.game.NicknameError
import com.vibecoding.millionaire.domain.model.AgeGroup
import com.vibecoding.millionaire.domain.repository.PlayerRepository
import com.vibecoding.millionaire.ui.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerFormState(
    val nickname: String = "",
    val ageGroup: AgeGroup? = null,
    @StringRes val errorRes: Int? = null,
    val editMode: Boolean = false,
    val loading: Boolean = false,
    val saved: Boolean = false,
)

/** One ViewModel for "create player" and "edit player" (FR-1.1, FR-1.6). */
@HiltViewModel
class PlayerFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playerRepository: PlayerRepository,
) : ViewModel() {

    private val playerId: Long? = savedStateHandle[NavArgs.PLAYER_ID]

    private val _state = MutableStateFlow(
        PlayerFormState(editMode = playerId != null, loading = playerId != null),
    )
    val state: StateFlow<PlayerFormState> = _state.asStateFlow()

    init {
        playerId?.let(::loadPlayer)
    }

    private fun loadPlayer(id: Long) {
        viewModelScope.launch {
            val player = playerRepository.getPlayer(id)
            _state.update { current ->
                if (player == null) {
                    current.copy(loading = false)
                } else {
                    current.copy(
                        nickname = player.nickname,
                        ageGroup = player.ageGroup,
                        loading = false,
                    )
                }
            }
        }
    }

    fun onNicknameChange(value: String) {
        _state.update { it.copy(nickname = value, errorRes = null) }
    }

    fun onAgeGroupChange(ageGroup: AgeGroup) {
        _state.update { it.copy(ageGroup = ageGroup, errorRes = null) }
    }

    fun save() {
        val current = _state.value
        val ageGroup = current.ageGroup
        val validationError = NicknameValidator.validate(current.nickname)
        val error = when {
            validationError != null -> mapError(validationError)
            ageGroup == null -> R.string.error_select_age_group
            else -> null
        }
        if (error != null || ageGroup == null) {
            _state.update { it.copy(errorRes = error) }
            return
        }

        viewModelScope.launch {
            if (playerRepository.isNicknameTaken(current.nickname, playerId ?: 0L)) {
                _state.update { it.copy(errorRes = R.string.error_nickname_taken) }
                return@launch
            }
            val nickname = current.nickname.trim()
            if (playerId == null) {
                playerRepository.createPlayer(nickname, ageGroup)
            } else {
                playerRepository.updatePlayer(playerId, nickname, ageGroup)
            }
            _state.update { it.copy(saved = true) }
        }
    }

    @StringRes
    private fun mapError(error: NicknameError): Int = when (error) {
        NicknameError.Blank, NicknameError.TooShort -> R.string.error_nickname_too_short
        NicknameError.TooLong -> R.string.error_nickname_too_long
        NicknameError.InvalidCharacters -> R.string.error_nickname_invalid
        NicknameError.AlreadyTaken -> R.string.error_nickname_taken
    }
}