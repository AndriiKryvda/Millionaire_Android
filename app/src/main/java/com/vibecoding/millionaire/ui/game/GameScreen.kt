package com.vibecoding.millionaire.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecoding.millionaire.R
import com.vibecoding.millionaire.domain.game.GameState
import com.vibecoding.millionaire.domain.game.Phase
import com.vibecoding.millionaire.domain.game.PrizeLadder
import com.vibecoding.millionaire.domain.model.Lifeline
import com.vibecoding.millionaire.domain.model.Question
import com.vibecoding.millionaire.ui.common.ConfirmDialog
import com.vibecoding.millionaire.ui.common.GhostButton
import com.vibecoding.millionaire.ui.common.GoldButton
import com.vibecoding.millionaire.ui.common.formatGroups
import com.vibecoding.millionaire.ui.theme.Gold
import com.vibecoding.millionaire.ui.theme.GoldSoft
import com.vibecoding.millionaire.ui.theme.GradientBackground
import com.vibecoding.millionaire.ui.theme.Navy
import com.vibecoding.millionaire.ui.theme.Purple
import com.vibecoding.millionaire.ui.theme.TextSecondary

@Composable
fun GameScreen(
    onExit: () -> Unit,
    onOpenResult: (Long) -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val game = state.game

    BackHandler(enabled = game == null || game.phase != Phase.FINISHED) { viewModel.onExitRequest() }
    LaunchedEffect(state.resultId) { state.resultId?.let(onOpenResult) }
    LaunchedEffect(state.exitRequested) { if (state.exitRequested) onExit() }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            when {
                state.loading -> Centered { CircularProgressIndicator(color = GoldSoft) }
                state.errorRes != null -> Centered {
                    Text(
                        text = stringResource(state.errorRes!!),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    GoldButton(textRes = R.string.action_back_to_menu, onClick = onExit)
                }

                game != null -> GameContent(state, game, viewModel)
            }
        }
    }

    if (state.showExitDialog) {
        ConfirmDialog(
            titleRes = R.string.dialog_exit_title,
            messageRes = R.string.dialog_exit_message,
            confirmRes = R.string.action_exit,
            dismissRes = R.string.action_stay,
            onConfirm = viewModel::onExitConfirmed,
            onDismiss = viewModel::onExitCancelled,
        )
    }

    if (state.showWalkAwayDialog && game != null) {
        AlertDialog(
            onDismissRequest = viewModel::onWalkAwayDismissed,
            title = { Text(stringResource(R.string.dialog_walk_away_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.dialog_walk_away_message,
                        stringResource(R.string.prize_value, formatGroups(PrizeLadder.walkAwayPrize(game.correctAnswers))),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::onWalkAwayConfirmed) {
                    Text(stringResource(R.string.action_take_and_finish), color = Gold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onWalkAwayDismissed) { Text(stringResource(R.string.action_keep_playing)) }
            },
            containerColor = Navy,
            titleContentColor = Gold,
            textContentColor = Color.White,
        )
    }

    if (state.showAudience) {
        game?.audiencePercentages?.let { percentages ->
            AudienceChartDialog(percentages = percentages, onDismiss = viewModel::onAudienceDismissed)
        }
    }

    if (state.showFriend) {
        val hint = game?.friendHint
        if (hint != null && game != null) {
            val answerText = game.questions[game.currentIndex].options[hint.answer].orEmpty()
            PhoneFriendDialog(hint = hint, answerText = answerText, onDismiss = viewModel::onFriendDismissed)
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun GameContent(state: GameUiState, game: GameState, viewModel: GameViewModel) {
    val question = viewModel.currentQuestion(game)
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.nickname,
                    style = MaterialTheme.typography.titleMedium,
                    color = Gold,
                )
                Text(
                    text = stringResource(R.string.game_question_progress, game.currentIndex + 1, PrizeLadder.QUESTIONS_COUNT),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            TextButton(onClick = viewModel::onExitRequest) {
                Text(stringResource(R.string.action_exit), color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.weight(1f)) {
                QuestionCard(question = question, prizeRes = PrizeLadder.prizeFor(game.currentIndex))
                Spacer(modifier = Modifier.height(10.dp))
                OptionsGrid(game = game, question = question, viewModel = viewModel)
            }
            Spacer(modifier = Modifier.width(8.dp))
            PrizeLadderPanel(currentIndex = game.currentIndex, modifier = Modifier.width(94.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))
        LifelineRow(game = game, viewModel = viewModel)
        Spacer(modifier = Modifier.height(10.dp))
        BottomBar(game = game, viewModel = viewModel)
    }
}

@Composable
private fun QuestionCard(question: Question, prizeRes: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 116.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Navy.copy(alpha = 0.8f))
            .border(1.dp, Purple, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.prize_value, formatGroups(prizeRes)),
                style = MaterialTheme.typography.labelMedium,
                color = GoldSoft,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = question.text,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun OptionsGrid(game: GameState, question: Question, viewModel: GameViewModel) {
    game.displayOrder.chunked(2).forEach { rowOptions ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rowOptions.forEach { option ->
                val hidden = option in game.hiddenOptions
                if (hidden) {
                    Spacer(modifier = Modifier.weight(1f).height(58.dp))
                } else {
                    AnswerOptionButton(
                        letter = option.name,
                        text = question.options[option].orEmpty(),
                        visual = visualState(game, option),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onSelect(option) },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun visualState(game: GameState, option: com.vibecoding.millionaire.domain.model.AnswerOption): OptionVisualState =
    when (game.phase) {
        Phase.REVEALING -> if (game.selected == option) OptionVisualState.PULSING else OptionVisualState.IDLE
        Phase.ANSWER_CORRECT, Phase.ANSWER_WRONG, Phase.FINISHED -> when {
            option == game.questions[game.currentIndex].correct -> OptionVisualState.CORRECT
            option == game.selected -> OptionVisualState.WRONG
            else -> OptionVisualState.DIMMED
        }

        else -> if (game.selected == option) OptionVisualState.SELECTED else OptionVisualState.IDLE
    }

@Composable
private fun LifelineRow(game: GameState, viewModel: GameViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LifelineButton(
            label = stringResource(R.string.lifeline_fifty),
            used = Lifeline.FIFTY_FIFTY in game.usedLifelines,
        ) { viewModel.onLifeline(Lifeline.FIFTY_FIFTY) }
        LifelineButton(
            label = stringResource(R.string.lifeline_audience),
            used = Lifeline.ASK_AUDIENCE in game.usedLifelines,
        ) { viewModel.onLifeline(Lifeline.ASK_AUDIENCE) }
        LifelineButton(
            label = stringResource(R.string.lifeline_friend),
            used = Lifeline.PHONE_FRIEND in game.usedLifelines,
        ) { viewModel.onLifeline(Lifeline.PHONE_FRIEND) }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.questions_left, PrizeLadder.QUESTIONS_COUNT - game.currentIndex),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )
    }
}

@Composable
private fun BottomBar(game: GameState, viewModel: GameViewModel) {
    when (game.phase) {
        Phase.AWAITING_ANSWER, Phase.CONFIRMING -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton(
                text = stringResource(
                    R.string.action_walk_away,
                    stringResource(R.string.prize_value, formatGroups(PrizeLadder.walkAwayPrize(game.correctAnswers))),
                ),
                onClick = viewModel::onWalkAwayRequest,
                modifier = Modifier.weight(1f),
            )
            GoldButton(
                textRes = R.string.action_confirm,
                onClick = viewModel::onConfirm,
                enabled = game.phase == Phase.CONFIRMING,
                modifier = Modifier.weight(1f),
            )
        }

        Phase.REVEALING -> Box(modifier = Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Gold, strokeWidth = 3.dp, modifier = Modifier.height(28.dp))
        }

        Phase.ANSWER_CORRECT -> GoldButton(
            textRes = R.string.action_next,
            onClick = viewModel::onContinue,
            modifier = Modifier.fillMaxWidth(),
        )

        Phase.ANSWER_WRONG, Phase.FINISHED -> GoldButton(
            textRes = R.string.action_see_result,
            onClick = viewModel::onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}