package com.vibecoding.millionaire.ui.game

import org.jetbrains.compose.resources.stringResource

import org.jetbrains.compose.resources.StringResource

import com.vibecoding.millionaire.di.AppGraph
import com.vibecoding.millionaire.ui.common.appViewModel

import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecoding.millionaire.generated.resources.Res
import com.vibecoding.millionaire.generated.resources.*
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameScreen(
    playerId: Long,
    graph: AppGraph,
    onExit: () -> Unit,
    onOpenResult: (Long) -> Unit,
    viewModel: GameViewModel = appViewModel {
        GameViewModel(
            playerId = playerId,
            playerRepository = graph.playerRepository,
            startGameUseCase = graph.startGame,
            saveGameResult = graph.saveGameResult,
            gameEngine = graph.gameEngine,
            clock = graph.clock,
            feedback = graph.feedback,
        )
    },
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
                    GoldButton(textRes = Res.string.action_back_to_menu, onClick = onExit)
                }

                game != null -> GameContent(state, game, viewModel)
            }
        }
    }

    if (state.showExitDialog) {
        ConfirmDialog(
            titleRes = Res.string.dialog_exit_title,
            messageRes = Res.string.dialog_exit_message,
            confirmRes = Res.string.action_exit_to_menu,
            dismissRes = Res.string.action_stay,
            onConfirm = viewModel::onExitConfirmed,
            onDismiss = viewModel::onExitCancelled,
        )
    }

    if (state.showWalkAwayDialog && game != null) {
        AlertDialog(
            onDismissRequest = viewModel::onWalkAwayDismissed,
            title = { Text(stringResource(Res.string.dialog_walk_away_title)) },
            text = {
                Text(
                    stringResource(
                        Res.string.dialog_walk_away_message,
                        stringResource(Res.string.prize_value, formatGroups(PrizeLadder.walkAwayPrize(game.correctAnswers))),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::onWalkAwayConfirmed) {
                    Text(stringResource(Res.string.action_take_and_finish), color = Gold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onWalkAwayDismissed) { Text(stringResource(Res.string.action_keep_playing)) }
            },
            containerColor = Navy,
            titleContentColor = Gold,
            textContentColor = Color.White,
        )
    }

    if (game != null) {
        val percentages = game.audiencePercentages
        if (state.showAudience && percentages != null) {
            AudienceChartDialog(
                percentages = percentages,
                displayOrder = game.displayOrder,
                onDismiss = viewModel::onAudienceDismissed,
            )
        }

        val hint = game.friendHint
        if (state.showFriend && hint != null) {
            val answerText = game.questions[game.currentIndex].options[hint.answer].orEmpty()
            PhoneFriendDialog(
                hint = hint,
                letter = displayLetter(game.displayOrder, hint.answer),
                answerText = answerText,
                onDismiss = viewModel::onFriendDismissed,
            )
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
    // A lifeline is spent for the whole game, so the player confirms it before it is applied.
    var pendingLifeline by remember { mutableStateOf<Lifeline?>(null) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.nickname,
                    style = MaterialTheme.typography.titleMedium,
                    color = Gold,
                )
                Text(
                    text = stringResource(Res.string.game_question_progress, game.currentIndex + 1, PrizeLadder.QUESTIONS_COUNT),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            TextButton(onClick = viewModel::onExitRequest) {
                Text(stringResource(Res.string.action_exit), color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        QuestionCard(question = question, prizeAmount = PrizeLadder.prizeFor(game.currentIndex))

        Spacer(modifier = Modifier.height(10.dp))

        // Answers keep the free width; the prize ladder stands vertically to their right.
        Row(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.TopStart,
            ) {
                OptionsList(
                    game = game,
                    question = question,
                    viewModel = viewModel,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            PrizeLadderPanel(
                currentIndex = game.currentIndex,
                modifier = Modifier.fillMaxHeight(),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        LifelineRow(game = game) { lifeline ->
            // Already spent lifelines just reopen their result; a fresh one needs a confirmation.
            if (lifeline in game.usedLifelines || game.phase != Phase.AWAITING_ANSWER) {
                viewModel.onLifeline(lifeline)
            } else {
                pendingLifeline = lifeline
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        BottomBar(game = game, viewModel = viewModel)
    }

    val pending = pendingLifeline
    if (pending != null) {
        LifelineConfirmDialog(
            lifeline = pending,
            onConfirm = {
                pendingLifeline = null
                viewModel.onLifeline(pending)
            },
            onDismiss = { pendingLifeline = null },
        )
    }
}

/** Asks before a lifeline is spent, because every lifeline works only once per game. */
@Composable
private fun LifelineConfirmDialog(lifeline: Lifeline, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val labelRes = when (lifeline) {
        Lifeline.FIFTY_FIFTY -> Res.string.lifeline_fifty
        Lifeline.ASK_AUDIENCE -> Res.string.lifeline_audience
        Lifeline.PHONE_FRIEND -> Res.string.lifeline_friend
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dialog_lifeline_title)) },
        text = { Text(stringResource(Res.string.dialog_lifeline_message, stringResource(labelRes))) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(Res.string.action_use_lifeline), color = Gold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
        containerColor = Navy,
        titleContentColor = Gold,
        textContentColor = Color.White,
    )
}

@Composable
private fun QuestionCard(question: Question, prizeAmount: Int) {
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
                text = stringResource(Res.string.prize_value, formatGroups(prizeAmount)),
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

/** Answers listed one per line as A, B, C, D; options hidden by 50:50 keep an empty placeholder. */
@Composable
private fun OptionsList(
    game: GameState,
    question: Question,
    viewModel: GameViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        game.displayOrder.forEach { option ->
            if (option in game.hiddenOptions) {
                Spacer(modifier = Modifier.fillMaxWidth().height(58.dp))
            } else {
                AnswerOptionButton(
                    letter = displayLetter(game.displayOrder, option),
                    text = question.options[option].orEmpty(),
                    visual = visualState(game, option),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.onSelect(option) },
                )
            }
        }
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
private fun LifelineRow(game: GameState, onTap: (Lifeline) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LifelineButton(
            label = stringResource(Res.string.lifeline_fifty),
            used = Lifeline.FIFTY_FIFTY in game.usedLifelines,
        ) { onTap(Lifeline.FIFTY_FIFTY) }
        LifelineButton(
            label = stringResource(Res.string.lifeline_audience),
            used = Lifeline.ASK_AUDIENCE in game.usedLifelines,
        ) { onTap(Lifeline.ASK_AUDIENCE) }
        LifelineButton(
            label = stringResource(Res.string.lifeline_friend),
            used = Lifeline.PHONE_FRIEND in game.usedLifelines,
        ) { onTap(Lifeline.PHONE_FRIEND) }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(Res.string.questions_left, PrizeLadder.QUESTIONS_COUNT - game.currentIndex),
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
                    Res.string.action_walk_away,
                    stringResource(Res.string.prize_value, formatGroups(PrizeLadder.walkAwayPrize(game.correctAnswers))),
                ),
                onClick = viewModel::onWalkAwayRequest,
                modifier = Modifier.weight(1f),
            )
            GoldButton(
                textRes = Res.string.action_confirm,
                onClick = viewModel::onConfirm,
                enabled = game.phase == Phase.CONFIRMING,
                modifier = Modifier.weight(1f),
            )
        }

        Phase.REVEALING -> Box(modifier = Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Gold, strokeWidth = 3.dp, modifier = Modifier.height(28.dp))
        }

        Phase.ANSWER_CORRECT -> GoldButton(
            textRes = Res.string.action_next,
            onClick = viewModel::onContinue,
            modifier = Modifier.fillMaxWidth(),
        )

        Phase.ANSWER_WRONG, Phase.FINISHED -> GoldButton(
            textRes = Res.string.action_see_result,
            onClick = viewModel::onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
