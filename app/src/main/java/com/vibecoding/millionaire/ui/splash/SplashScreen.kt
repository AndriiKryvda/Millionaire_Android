package com.vibecoding.millionaire.ui.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecoding.millionaire.R
import com.vibecoding.millionaire.ui.common.GoldButton
import com.vibecoding.millionaire.ui.theme.Gold
import com.vibecoding.millionaire.ui.theme.GoldSoft
import com.vibecoding.millionaire.ui.theme.GradientBackground
import com.vibecoding.millionaire.ui.theme.TextSecondary

@Composable
fun SplashScreen(
    onToPlayers: () -> Unit,
    onToMenu: (Long) -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = Gold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.splash_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(32.dp))
            when (val current = destination) {
                SplashViewModel.Destination.Loading -> {
                    CircularProgressIndicator(color = GoldSoft)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.splash_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }

                is SplashViewModel.Destination.ToPlayers -> LaunchedEffect(current) { onToPlayers() }

                is SplashViewModel.Destination.ToMenu -> LaunchedEffect(current) { onToMenu(current.playerId) }

                is SplashViewModel.Destination.Failure -> {
                    Text(
                        text = stringResource(current.messageRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    GoldButton(textRes = R.string.action_retry, onClick = viewModel::load)
                }
            }
        }
    }
}