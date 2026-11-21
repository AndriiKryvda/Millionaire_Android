package com.vibecoding.millionaire.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoding.millionaire.domain.model.GameResult
import com.vibecoding.millionaire.domain.repository.GameResultRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ResultViewModel(
    resultId: Long,
    gameResultRepository: GameResultRepository,
) : ViewModel() {

    val result: StateFlow<GameResult?> = gameResultRepository.observeResult(resultId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}