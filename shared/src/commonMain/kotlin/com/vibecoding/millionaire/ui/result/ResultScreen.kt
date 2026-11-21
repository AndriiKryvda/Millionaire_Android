package com.vibecoding.millionaire.ui.result

import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource

import org.jetbrains.compose.resources.StringResource

import com.vibecoding.millionaire.di.AppGraph
import com.vibecoding.millionaire.ui.common.appViewModel

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
import org.jetbrains.compose.resources.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecoding.millionaire.generated.resources.Res
import com.vibecoding.millionaire.generated.resources.*
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
    resultId: Long,
    graph: AppGraph,
    onPlayAgain: (Long) -> Unit,
    onMenu: () -> Unit,
    viewModel: ResultViewModel = appViewModel {
        ResultViewModel(resultId = resultId, gameResultRepository = graph.gameResultRepository)
    },
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
                Text(stringResource(Res.string.loading), color = TextSecondary)
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
                    text = stringResource(Res.string.prize_value, formatGroups(current.prize)),
                    style = MaterialTheme.typography.displaySmall,
                    color = Gold,
                )
                Spacer(modifier = Modifier.height(24.dp))

                CardPanel {
                    LabeledValue(labelRes = Res.string.stat_correct_answers, value = current.correctAnswers.toString())
                    LabeledValue(labelRes = Res.string.stat_lifelines_used, value = current.lifelinesUsed.size.toString())
                    LabeledValue(
                        labelRes = Res.string.stat_duration,
                        value = pluralStringResource(
                            Res.plurals.minutes,
                            minutesPlayed,
                            minutesPlayed,
                        ),
                    )
                }

                if (current.outcome == GameOutcome.WRONG_ANSWER && current.lastQuestionText != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CardPanel {
                        Text(
                            text = stringResource(Res.string.result_correct_answer_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = GoldSoft,
                        )
                        Text(text = current.lastQuestionText, style = MaterialTheme.typography.bodyMedium)
                        current.lastCorrectAnswerText?.let {
                            Text(
                                text = stringResource(Res.string.result_correct_answer, it),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Gold,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
                GoldButton(
                    textRes = Res.string.action_play_again,
                    onClick = { onPlayAgain(current.playerId) },
                    modifier = Modifier.fillMaxWidth(),
                )
                GhostButton(textRes = Res.string.action_back_to_menu, onClick = onMenu, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun outcomeTitleRes(result: GameResult): StringResource = when (result.outcome) {
    GameOutcome.WON -> Res.string.result_won
    GameOutcome.WALKED_AWAY -> Res.string.result_walked_away
    GameOutcome.WRONG_ANSWER -> Res.string.result_wrong_answer
}
