package com.vibecoding.millionaire.ui.player

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecoding.millionaire.R
import com.vibecoding.millionaire.domain.game.NicknameValidator
import com.vibecoding.millionaire.domain.model.AgeGroup
import com.vibecoding.millionaire.ui.common.CardPanel
import com.vibecoding.millionaire.ui.common.GoldButton
import com.vibecoding.millionaire.ui.common.ScreenScaffold
import com.vibecoding.millionaire.ui.theme.Gold
import com.vibecoding.millionaire.ui.theme.Purple
import com.vibecoding.millionaire.ui.theme.TextSecondary

@Composable
fun PlayerFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: PlayerFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    ScreenScaffold(
        titleRes = if (state.editMode) R.string.title_edit_player else R.string.title_create_player,
        onBack = onBack,
    ) {
        CardPanel {
            OutlinedTextField(
                value = state.nickname,
                onValueChange = viewModel::onNicknameChange,
                label = { Text(stringResource(R.string.field_nickname)) },
                supportingText = {
                    Text(
                        text = stringResource(
                            R.string.nickname_counter,
                            state.nickname.trim().length,
                            NicknameValidator.MAX_LENGTH,
                        ),
                        color = TextSecondary,
                    )
                },
                isError = state.errorRes != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.save() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = Purple,
                    focusedLabelColor = Gold,
                    cursorColor = Gold,
                ),
            )
            state.errorRes?.let {
                Text(
                    text = stringResource(it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Text(
            text = stringResource(R.string.label_select_age_group),
            style = MaterialTheme.typography.titleMedium,
            color = Gold,
        )
        AgeGroup.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { ageGroup ->
                    AgeGroupCard(
                        ageGroup = ageGroup,
                        selected = state.ageGroup == ageGroup,
                        onClick = { viewModel.onAgeGroupChange(ageGroup) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        GoldButton(
            textRes = if (state.editMode) R.string.action_save else R.string.action_create,
            onClick = viewModel::save,
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AgeGroupCard(
    ageGroup: AgeGroup,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Gold else Purple,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(ageGroup.titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) Gold else Color.White,
        )
    }
}