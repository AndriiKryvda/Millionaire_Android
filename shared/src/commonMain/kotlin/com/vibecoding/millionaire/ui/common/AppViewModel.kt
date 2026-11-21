package com.vibecoding.millionaire.ui.common

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/**
 * Creates a [ViewModel] without a DI framework: the caller passes a lambda that builds the already
 * wired instance from the [com.vibecoding.millionaire.di.AppGraph].
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline creator: () -> VM): VM =
    viewModel(factory = viewModelFactory { initializer { creator() } })
