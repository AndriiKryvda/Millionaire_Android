package com.vibecoding.millionaire.ui.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoding.millionaire.domain.model.AgeGroup
import com.vibecoding.millionaire.domain.model.GameResult
import com.vibecoding.millionaire.domain.usecase.ObserveTopResultsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class RecordsViewModel(
    observeTopResults: ObserveTopResultsUseCase,
) : ViewModel() {

    private val filter = MutableStateFlow<AgeGroup?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val results: StateFlow<List<GameResult>> = filter
        .flatMapLatest { ageGroup -> observeTopResults(ageGroup) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ageGroupFilter: StateFlow<AgeGroup?> = filter.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setFilter(ageGroup: AgeGroup?) {
        filter.value = ageGroup
    }
}