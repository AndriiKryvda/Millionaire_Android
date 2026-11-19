package com.vibecoding.millionaire.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecoding.millionaire.R
import com.vibecoding.millionaire.domain.model.GameOutcome
import com.vibecoding.millionaire.domain.model.GameResult
import com.vibecoding.millionaire.ui.common.CardPanel
import com.vibecoding.millionaire.ui.common.GhostButton
import com.vibecoding.millionaire.ui.common.GoldButton
import com.vibecoding.millionaire.ui.common.LabeledValue
import com.vibecoding.millionaire.ui.common.formatGroups
import com.vibecoding.millionaire.ui.theme.Gold
import com.vibecoding.millionaire.ui.theme.GoldSoft
import com.vibecoding.millionaire.ui.theme.GradientBackground
import com.vibecoding.millionaire.ui.theme.TextSecondary

@Composable
fun ResultScreen(
    onPlayAgain: (Long) -> Unit,
    onMenu: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel(),
) {
    val result by viewModel.result.collectAsStateWithLifecycle()

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val current = result
            if (current == null) {
                Text(stringResource(R.string.loading), color = TextSecondary)
            } else {
                val minutesPlayed = ((current.finishedAt - current.startedAt) / 60_000L).toInt()
                Text(
                    text = stringResource(outcomeTitleRes(current)),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Gold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.prize_value, formatGroups(current.prize)),
                    style = MaterialTheme.typography.displaySmall,
                    color = Gold,
                )
                Spacer(modifier = Modifier.height(24.dp))

                CardPanel {
                    LabeledValue(labelRes = R.string.stat_correct_answers, value = current.correctAnswers.toString())
                    LabeledValue(labelRes = R.string.stat_lifelines_used, value = current.lifelinesUsed.size.toString())
                    LabeledValue(
                        labelRes = R.string.stat_duration,
                        value = pluralStringResource(
                            R.plurals.minutes,
                            minutesPlayed,
                            minutesPlayed,
                        ),
                    )
                }

                if (current.outcome == GameOutcome.WRONG_ANSWER && current.lastQuestionText != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CardPanel {
                        Text(
                            text = stringResource(R.string.result_correct_answer_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = GoldSoft,
                        )
                        Text(text = current.lastQuestionText, style = MaterialTheme.typography.bodyMedium)
                        current.lastCorrectAnswerText?.let {
                            Text(
                                text = stringResource(R.string.result_correct_answer, it),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Gold,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
                GoldButton(
                    textRes = R.string.action_play_again,
                    onClick = { onPlayAgain(current.playerId) },
                    modifier = Modifier.fillMaxWidth(),
                )
                GhostButton(textRes = R.string.action_back_to_menu, onClick = onMenu, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun outcomeTitleRes(result: GameResult): Int = when (result.outcome) {
    GameOutcome.WON -> R.string.result_won
    GameOutcome.WALKED_AWAY -> R.string.result_walked_away
    GameOutcome.WRONG_ANSWER -> R.string.result_wrong_answer
}