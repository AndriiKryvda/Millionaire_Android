package com.vibecoding.millionaire.ui.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoding.millionaire.domain.model.GameResult
import com.vibecoding.millionaire.domain.repository.GameResultRepository
import com.vibecoding.millionaire.ui.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    gameResultRepository: GameResultRepository,
) : ViewModel() {

    private val resultId: Long = savedStateHandle[NavArgs.RESULT_ID] ?: -1L

    val result: StateFlow<GameResult?> = gameResultRepository.observeResult(resultId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}