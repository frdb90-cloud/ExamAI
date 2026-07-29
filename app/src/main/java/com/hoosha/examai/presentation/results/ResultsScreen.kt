package com.hoosha.examai.presentation.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoosha.examai.domain.model.ExamAnswer
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    onBack: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("نتایج آزمون")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Outlined.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.answers.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("هنوز نتیجه‌ای برای این آزمون ذخیره نشده است.")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ResultSummary(state)
                    }

                    items(
                        items = state.answers,
                        key = ExamAnswer::questionNumber
                    ) { answer ->
                        AnswerCard(answer)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultSummary(
    state: ResultsUiState
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "خلاصه تحلیل",
                style = MaterialTheme.typography.titleLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                SummaryItem(
                    label = "کل سؤال‌ها",
                    value = state.answers.size.toString()
                )

                SummaryItem(
                    label = "پاسخ‌داده‌شده",
                    value = state.answeredCount.toString()
                )

                SummaryItem(
                    label = "منبع ناکافی",
                    value = state.insufficientCount.toString()
                )
            }

            state.averageConfidence?.let { confidence ->
                Text(
                    text = "میانگین اطمینان: ${
                        NumberFormat.getPercentInstance(
                            Locale("fa")
                        ).format(confidence.coerceIn(0.0, 1.0))
                    }"
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun AnswerCard(
    answer: ExamAnswer
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "سؤال ${answer.questionNumber}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = answer.questionText,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = answer.status.persianTitle(),
                color = if (answer.status == "ANSWERED") {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                fontWeight = FontWeight.Bold
            )

            if (answer.correctOption != null) {
                Text(
                    text = buildString {
                        append("پاسخ صحیح: ")
                        append(
                            answer.correctOptionLabel
                                ?: answer.correctOption
                        )

                        answer.correctOptionText
                            ?.takeIf(String::isNotBlank)
                            ?.let {
                                append(" — ")
                                append(it)
                            }
                    },
                    style = MaterialTheme.typography.titleSmall
                )
            }

            answer.confidence?.let { confidence ->
                Text(
                    text = "میزان اطمینان: ${
                        NumberFormat.getPercentInstance(
                            Locale("fa")
                        ).format(confidence.coerceIn(0.0, 1.0))
                    }"
                )
            }

            if (answer.explanation.isNotBlank()) {
                HorizontalDivider()

                Text(
                    text = "توضیح",
                    fontWeight = FontWeight.Bold
                )

                Text(answer.explanation)
            }

            if (answer.optionAnalysis.isNotEmpty()) {
                HorizontalDivider()

                Text(
                    text = "بررسی گزینه‌ها",
                    fontWeight = FontWeight.Bold
                )

                answer.optionAnalysis.forEach { item ->
                    Text(
                        text = "${item.option}: ${item.explanation}",
                        color = if (item.isCorrect) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }

            if (answer.citations.isNotEmpty()) {
                HorizontalDivider()

                Text(
                    text = "منابع پاسخ",
                    fontWeight = FontWeight.Bold
                )

                answer.citations.forEachIndexed { index, citation ->
                    Text(
                        text = buildString {
                            append("${index + 1}. ")
                            append(citation.fileName)

                            citation.pageNumber?.let {
                                append("، صفحه $it")
                            }

                            citation.section
                                ?.takeIf(String::isNotBlank)
                                ?.let {
                                    append("، بخش $it")
                                }
                        },
                        fontWeight = FontWeight.Medium
                    )

                    if (citation.quote.isNotBlank()) {
                        Text(
                            text = "«${citation.quote}»",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

private fun String.persianTitle(): String = when (this) {
    "ANSWERED" -> "پاسخ بر اساس منابع"
    "INSUFFICIENT_SOURCE" -> "اطلاعات منابع کافی نیست"
    "INVALID_QUESTION" -> "سؤال نامعتبر"
    "FAILED" -> "تحلیل این سؤال ناموفق بود"
    else -> this
}