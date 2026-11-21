package com.vibecoding.millionaire.ui.rules

import org.jetbrains.compose.resources.stringResource

import org.jetbrains.compose.resources.StringResource

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vibecoding.millionaire.generated.resources.Res
import com.vibecoding.millionaire.generated.resources.*
import com.vibecoding.millionaire.ui.common.CardPanel
import com.vibecoding.millionaire.ui.common.ScreenScaffold
import com.vibecoding.millionaire.ui.theme.Gold
import com.vibecoding.millionaire.ui.theme.TextSecondary

private data class RulesSection(val titleRes: StringResource, val bodyRes: StringResource)

private val sections = listOf(
    RulesSection(Res.string.rules_goal_title, Res.string.rules_goal_body),
    RulesSection(Res.string.rules_ladder_title, Res.string.rules_ladder_body),
    RulesSection(Res.string.rules_answer_title, Res.string.rules_answer_body),
    RulesSection(Res.string.rules_lifelines_title, Res.string.rules_lifelines_body),
    RulesSection(Res.string.rules_walkaway_title, Res.string.rules_walkaway_body),
    RulesSection(Res.string.rules_questions_title, Res.string.rules_questions_body),
    RulesSection(Res.string.rules_privacy_title, Res.string.rules_privacy_body),
)

@Composable
fun RulesScreen(onBack: () -> Unit) {
    ScreenScaffold(titleRes = Res.string.title_rules, onBack = onBack) {
        Text(
            text = stringResource(Res.string.rules_intro),
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
                text = stringResource(Res.string.rules_outro),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}
