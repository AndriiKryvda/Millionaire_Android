package com.vibecoding.millionaire.ui.player

import org.jetbrains.compose.resources.StringResource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoding.millionaire.generated.resources.Res
import com.vibecoding.millionaire.generated.resources.*
import com.vibecoding.millionaire.domain.game.NicknameValidator
import com.vibecoding.millionaire.domain.game.NicknameError
import com.vibecoding.millionaire.domain.model.AgeGroup
import com.vibecoding.millionaire.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerFormState(
    val nickname: String = "",
    val ageGroup: AgeGroup? = null,
    val errorRes: StringResource? = null,
    val editMode: Boolean = false,
    val loading: Boolean = false,
    val saved: Boolean = false,
)

/** One ViewModel for "create player" and "edit player" (FR-1.1, FR-1.6). */
class PlayerFormViewModel(
    private val playerId: Long?,
    private val playerRepository: PlayerRepository,
) : ViewModel() {

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
            ageGroup == null -> Res.string.error_select_age_group
            else -> null
        }
        if (error != null || ageGroup == null) {
            _state.update { it.copy(errorRes = error) }
            return
        }

        viewModelScope.launch {
            if (playerRepository.isNicknameTaken(current.nickname, playerId ?: 0L)) {
                _state.update { it.copy(errorRes = Res.string.error_nickname_taken) }
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

    private fun mapError(error: NicknameError): StringResource = when (error) {
        NicknameError.Blank, NicknameError.TooShort -> Res.string.error_nickname_too_short
        NicknameError.TooLong -> Res.string.error_nickname_too_long
        NicknameError.InvalidCharacters -> Res.string.error_nickname_invalid
        NicknameError.AlreadyTaken -> Res.string.error_nickname_taken
    }
}
