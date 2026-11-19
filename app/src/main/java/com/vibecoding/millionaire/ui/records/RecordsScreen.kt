package com.vibecoding.millionaire.ui.records

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecoding.millionaire.R
import com.vibecoding.millionaire.domain.model.AgeGroup
import com.vibecoding.millionaire.domain.model.GameResult
import com.vibecoding.millionaire.ui.common.CardPanel
import com.vibecoding.millionaire.ui.common.EmptyState
import com.vibecoding.millionaire.ui.common.ScreenScaffold
import com.vibecoding.millionaire.ui.common.formatGroups
import com.vibecoding.millionaire.ui.theme.Gold
import com.vibecoding.millionaire.ui.theme.TextSecondary
import java.text.DateFormat
import java.util.Date

@Composable
fun RecordsScreen(
    onBack: () -> Unit,
    viewModel: RecordsViewModel = hiltViewModel(),
) {
    val results by viewModel.results.collectAsStateWithLifecycle()
    val filter by viewModel.ageGroupFilter.collectAsStateWithLifecycle()

    ScreenScaffold(titleRes = R.string.title_records, onBack = onBack, scroll = false) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = filter == null,
                onClick = { viewModel.setFilter(null) },
                label = { Text(stringResource(R.string.filter_all)) },
            )
            AgeGroup.entries.forEach { ageGroup ->
                FilterChip(
                    selected = filter == ageGroup,
                    onClick = { viewModel.setFilter(if (filter == ageGroup) null else ageGroup) },
                    label = { Text(stringResource(ageGroup.titleRes)) },
                )
            }
        }

        if (results.isEmpty()) {
            EmptyState(textRes = R.string.records_empty)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                results.forEachIndexed { index, result ->
                    ResultRow(rank = index + 1, result = result)
                }
            }
        }
    }
}

@Composable
private fun ResultRow(rank: Int, result: GameResult) {
    CardPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$rank.",
                style = MaterialTheme.typography.titleLarge,
                color = TextSecondary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = result.nickname, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(
                        R.string.records_row_details,
                        stringResource(result.ageGroup.titleRes),
                        result.correctAnswers,
                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(result.finishedAt)),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            Text(
                text = stringResource(R.string.prize_value, formatGroups(result.prize)),
                style = MaterialTheme.typography.titleLarge,
                color = Gold,
            )
        }
    }
}