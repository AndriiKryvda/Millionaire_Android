package com.vibecoding.millionaire.ui.settings

import org.jetbrains.compose.resources.stringResource

import org.jetbrains.compose.resources.StringResource

import com.vibecoding.millionaire.di.AppGraph
import com.vibecoding.millionaire.ui.common.appViewModel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecoding.millionaire.generated.resources.Res
import com.vibecoding.millionaire.generated.resources.*
import com.vibecoding.millionaire.ui.common.CardPanel
import com.vibecoding.millionaire.ui.common.ConfirmDialog
import com.vibecoding.millionaire.ui.common.GhostButton
import com.vibecoding.millionaire.ui.common.LabeledValue
import com.vibecoding.millionaire.ui.common.ScreenScaffold
import com.vibecoding.millionaire.ui.common.SectionTitle
import com.vibecoding.millionaire.ui.theme.Gold
import com.vibecoding.millionaire.ui.theme.GoldSoft
import com.vibecoding.millionaire.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    playerId: Long,
    graph: AppGraph,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = appViewModel {
        SettingsViewModel(
            playerId = playerId,
            settingsRepository = graph.settingsRepository,
            resetGameHistory = graph.resetGameHistory,
            questionBankRepository = graph.questionBankRepository,
        )
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ScreenScaffold(titleRes = Res.string.title_settings, onBack = onBack) {
        SectionTitle(textRes = Res.string.section_game)
        CardPanel {
            SettingSwitch(
                titleRes = Res.string.setting_sound,
                subtitleRes = Res.string.setting_sound_hint,
                checked = state.soundEnabled,
                onCheckedChange = viewModel::setSoundEnabled,
            )
            SettingSwitch(
                titleRes = Res.string.setting_vibration,
                subtitleRes = Res.string.setting_vibration_hint,
                checked = state.vibrationEnabled,
                onCheckedChange = viewModel::setVibrationEnabled,
            )
        }

        SectionTitle(textRes = Res.string.section_history)
        CardPanel {
            Text(
                text = stringResource(Res.string.reset_history_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            GhostButton(
                textRes = Res.string.action_reset_history,
                onClick = viewModel::askResetHistory,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionTitle(textRes = Res.string.section_about)
        CardPanel {
            LabeledValue(labelRes = Res.string.about_version, value = graph.appVersion)
            LabeledValue(labelRes = Res.string.about_questions, value = state.questionCount.toString())
            Text(
                text = stringResource(Res.string.about_offline),
                style = MaterialTheme.typography.bodySmall,
                color = GoldSoft,
            )
        }
    }

    if (state.showResetDialog) {
        ConfirmDialog(
            titleRes = Res.string.dialog_reset_history_title,
            messageRes = Res.string.dialog_reset_history_message,
            confirmRes = Res.string.action_reset,
            dismissRes = Res.string.action_cancel,
            onConfirm = viewModel::confirmResetHistory,
            onDismiss = viewModel::dismissResetDialog,
        )
    }
}

@Composable
private fun SettingSwitch(
    titleRes: StringResource,
    subtitleRes: StringResource,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleMedium, color = Gold)
            Text(
                text = stringResource(subtitleRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Gold,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
    }
}
