package com.vibecoding.millionaire.ui.settings

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecoding.millionaire.BuildConfig
import com.vibecoding.millionaire.R
import com.vibecoding.millionaire.ui.common.CardPanel
import com.vibecoding.millionaire.ui.common.ConfirmDialog
import com.vibecoding.millionaire.ui.common.GhostButton
import com.vibecoding.millionaire.ui.common.LabeledValue
import com.vibecoding.millionaire.ui.common.ScreenScaffold
import com.vibecoding.millionaire.ui.common.SectionTitle
import com.vibecoding.millionaire.ui.theme.Gold
import com.vibecoding.millionaire.ui.theme.GoldSoft

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ScreenScaffold(titleRes = R.string.title_settings, onBack = onBack) {
        SectionTitle(textRes = R.string.section_game)
        CardPanel {
            SettingSwitch(
                titleRes = R.string.setting_sound,
                subtitleRes = R.string.setting_sound_hint,
                checked = state.soundEnabled,
                onCheckedChange = viewModel::setSoundEnabled,
            )
            SettingSwitch(
                titleRes = R.string.setting_vibration,
                subtitleRes = R.string.setting_vibration_hint,
                checked = state.vibrationEnabled,
                onCheckedChange = viewModel::setVibrationEnabled,
            )
        }

        SectionTitle(textRes = R.string.section_history)
        CardPanel {
            Text(
                text = stringResource(R.string.reset_history_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
            GhostButton(
                textRes = R.string.action_reset_history,
                onClick = viewModel::askResetHistory,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionTitle(textRes = R.string.section_about)
        CardPanel {
            LabeledValue(labelRes = R.string.about_version, value = BuildConfig.VERSION_NAME)
            LabeledValue(labelRes = R.string.about_questions, value = state.questionCount.toString())
            Text(
                text = stringResource(R.string.about_offline),
                style = MaterialTheme.typography.bodySmall,
                color = GoldSoft,
            )
        }
    }

    if (state.showResetDialog) {
        ConfirmDialog(
            titleRes = R.string.dialog_reset_history_title,
            messageRes = R.string.dialog_reset_history_message,
            confirmRes = R.string.action_reset,
            dismissRes = R.string.action_cancel,
            onConfirm = viewModel::confirmResetHistory,
            onDismiss = viewModel::dismissResetDialog,
        )
    }
}

@Composable
private fun SettingSwitch(
    titleRes: Int,
    subtitleRes: Int,
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