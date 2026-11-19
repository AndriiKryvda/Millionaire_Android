package com.vibecoding.millionaire.ui.rules

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vibecoding.millionaire.R
import com.vibecoding.millionaire.ui.common.CardPanel
import com.vibecoding.millionaire.ui.common.ScreenScaffold
import com.vibecoding.millionaire.ui.theme.Gold
import com.vibecoding.millionaire.ui.theme.TextSecondary

private data class RulesSection(@StringRes val titleRes: Int, @StringRes val bodyRes: Int)

private val sections = listOf(
    RulesSection(R.string.rules_goal_title, R.string.rules_goal_body),
    RulesSection(R.string.rules_ladder_title, R.string.rules_ladder_body),
    RulesSection(R.string.rules_answer_title, R.string.rules_answer_body),
    RulesSection(R.string.rules_lifelines_title, R.string.rules_lifelines_body),
    RulesSection(R.string.rules_walkaway_title, R.string.rules_walkaway_body),
    RulesSection(R.string.rules_questions_title, R.string.rules_questions_body),
    RulesSection(R.string.rules_privacy_title, R.string.rules_privacy_body),
)

@Composable
fun RulesScreen(onBack: () -> Unit) {
    ScreenScaffold(titleRes = R.string.title_rules, onBack = onBack) {
        Text(
            text = stringResource(R.string.rules_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        sections.forEach { section ->
            CardPanel {
                Text(
                    text = stringResource(section.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = Gold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(section.bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.rules_outro),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}