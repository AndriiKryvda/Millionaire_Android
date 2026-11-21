package com.vibecoding.millionaire.ui.game

import org.jetbrains.compose.resources.stringResource

import org.jetbrains.compose.resources.StringResource

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vibecoding.millionaire.generated.resources.Res
import com.vibecoding.millionaire.generated.resources.*
import com.vibecoding.millionaire.domain.game.Confidence
import com.vibecoding.millionaire.domain.game.FriendHint
import com.vibecoding.millionaire.domain.game.PrizeLadder
import com.vibecoding.millionaire.domain.model.AnswerOption
import com.vibecoding.millionaire.ui.common.formatGroups
import com.vibecoding.millionaire.ui.theme.Gold
import com.vibecoding.millionaire.ui.theme.GoldSoft
import com.vibecoding.millionaire.ui.theme.HexagonShape
import com.vibecoding.millionaire.ui.theme.Navy
import com.vibecoding.millionaire.ui.theme.PlateBlue
import com.vibecoding.millionaire.ui.theme.PlateCorrect
import com.vibecoding.millionaire.ui.theme.PlateGradient
import com.vibecoding.millionaire.ui.theme.PlateSelected
import com.vibecoding.millionaire.ui.theme.PlateWrong
import com.vibecoding.millionaire.ui.theme.Purple
import com.vibecoding.millionaire.ui.theme.TextSecondary

enum class OptionVisualState { IDLE, SELECTED, PULSING, CORRECT, WRONG, DIMMED }

@Composable
fun AnswerOptionButton(
    letter: String,
    text: String,
    visual: OptionVisualState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    val alpha = if (visual == OptionVisualState.PULSING) pulseAlpha else 1f
    val container: Brush = when (visual) {
        OptionVisualState.IDLE -> PlateGradient
        OptionVisualState.SELECTED -> SolidColor(PlateSelected.copy(alpha = alpha))
        OptionVisualState.PULSING -> SolidColor(Gold.copy(alpha = alpha))
        OptionVisualState.CORRECT -> SolidColor(PlateCorrect)
        OptionVisualState.WRONG -> SolidColor(PlateWrong)
        OptionVisualState.DIMMED -> SolidColor(PlateBlue.copy(alpha = 0.30f))
    }
    val shape = HexagonShape()
    Box(
        modifier = modifier
            .heightIn(min = 58.dp)
            .clip(shape)
            .background(container)
            .border(1.5.dp, GoldSoft.copy(alpha = 0.7f), shape)
            .clickable(enabled = visual == OptionVisualState.IDLE || visual == OptionVisualState.SELECTED) { onClick() }
            .padding(horizontal = 22.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = "$letter.  $text",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )
    }
}

/** Prize ladder shown to the right of the answers: a vertical column, lowest step at the bottom. */
@Composable
fun PrizeLadderPanel(currentIndex: Int, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val stepHeightPx = with(density) { (LadderRowHeight + LadderRowGap).toPx() }
    Column(modifier = modifier.width(LadderPanelWidth)) {
        Text(
            text = stringResource(Res.string.ladder_title),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(LadderRowGap),
        ) {
            // Highest prize on top, so the ladder is drawn from the last prize backwards.
            PrizeLadder.prizes.indices.reversed().forEach { index ->
                LadderRow(
                    index = index,
                    prize = PrizeLadder.prizes[index],
                    currentIndex = currentIndex,
                )
            }
        }
    }
    LaunchedEffect(currentIndex, stepHeightPx) {
        // Keep the running step visible; the offset clamps to 0 when the whole ladder fits on screen.
        scrollState.animateScrollTo(((PrizeLadder.prizes.lastIndex - currentIndex) * stepHeightPx).toInt())
    }
}

private val LadderPanelWidth = 84.dp
private val LadderRowHeight = 20.dp
private val LadderRowGap = 3.dp

@Composable
private fun LadderRow(index: Int, prize: Int, currentIndex: Int) {
    val isCurrent = index == currentIndex
    val isPassed = index < currentIndex
    val isSafe = PrizeLadder.isSafeHaven(index)
    val background = when {
        isCurrent -> Gold
        isSafe && !isPassed -> Purple
        else -> Navy.copy(alpha = 0.55f)
    }
    val textColor = when {
        isCurrent -> Navy
        isPassed -> TextSecondary.copy(alpha = 0.6f)
        isSafe -> GoldSoft
        else -> Color.White
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(LadderRowHeight)
            .clip(RoundedCornerShape(5.dp))
            .background(background)
            .border(1.dp, Purple.copy(alpha = 0.5f), RoundedCornerShape(5.dp)),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = stringResource(Res.string.prize_value, formatGroups(prize)),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}

@Composable
fun LifelineButton(
    label: String,
    used: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val container = if (used) Purple.copy(alpha = 0.4f) else PlateBlue
    val content = if (used) TextSecondary.copy(alpha = 0.7f) else GoldSoft
    Box(
        modifier = modifier
            .size(62.dp)
            .clip(CircleShape)
            .background(container)
            .border(1.5.dp, content, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Letter shown next to an answer: A, B, C, D from its position in the displayed list, so the letters
 * always match what the player sees even though the option keys are shuffled for every question.
 */
fun displayLetter(displayOrder: List<AnswerOption>, option: AnswerOption): String =
    when (displayOrder.indexOf(option)) {
        0 -> "A"
        1 -> "B"
        2 -> "C"
        3 -> "D"
        else -> option.name
    }

@Composable
fun AudienceChartDialog(
    percentages: Map<AnswerOption, Int>,
    displayOrder: List<AnswerOption>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dialog_audience_title)) },
        text = { AudienceChart(percentages = percentages, displayOrder = displayOrder) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_close), color = Gold) }
        },
        containerColor = Navy,
        titleContentColor = Gold,
        textContentColor = Color.White,
    )
}

private val ChartAreaHeight = 196.dp
private val ChartBarMaxHeight = 150.dp
private val ChartBarMinHeight = 4.dp

@Composable
private fun AudienceChart(percentages: Map<AnswerOption, Int>, displayOrder: List<AnswerOption>) {
    // The leading answer owns the whole bar height, every other bar shrinks strictly in
    // proportion to it. Heights are plain dp so they can never collapse to equal sizes.
    val maxPercent = (percentages.values.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ChartAreaHeight)
            .padding(top = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        displayOrder.forEach { option ->
            val percent = percentages[option] ?: 0
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldSoft,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            (ChartBarMaxHeight * percent / maxPercent)
                                .coerceAtLeast(ChartBarMinHeight)
                        )
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(PlateGradient),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = displayLetter(displayOrder, option),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
fun PhoneFriendDialog(
    hint: FriendHint,
    letter: String,
    answerText: String,
    onDismiss: () -> Unit,
) {
    var connected by remember { mutableStateOf(false) }
    LaunchedEffect(hint) {
        connected = false
        kotlinx.coroutines.delay(1_500)
        connected = true
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dialog_friend_title)) },
        text = {
            if (!connected) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(26.dp), color = GoldSoft, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.size(14.dp))
                    Text(stringResource(Res.string.friend_calling, stringResource(hint.friend.nameRes)))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(hint.friend.nameRes),
                        style = MaterialTheme.typography.titleMedium,
                        color = Gold,
                    )
                    Text(
                        text = stringResource(hint.friend.descriptionRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Text(
                        text = stringResource(
                            Res.string.friend_answer,
                            stringResource(confidenceRes(hint)),
                            "$letter. $answerText",
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_thanks), color = Gold) }
        },
        containerColor = Navy,
        titleContentColor = Gold,
        textContentColor = Color.White,
    )
}

private fun confidenceRes(hint: FriendHint): StringResource = when {
    hint.confidence == Confidence.HIGH -> Res.string.friend_confidence_high
    hint.confidence == Confidence.MEDIUM -> Res.string.friend_confidence_medium
    else -> Res.string.friend_confidence_low
}
