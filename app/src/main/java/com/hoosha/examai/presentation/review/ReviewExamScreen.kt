package com.hoosha.examai.presentation.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoosha.examai.domain.model.ExamQuestion
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewExamScreen(
    onBack: () -> Unit,
    onAnalyze: (String) -> Unit,
    viewModel: ReviewExamViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ReviewExamEvent.NavigateToAnalysis ->
                    onAnalyze(event.examId)

                is ReviewExamEvent.Message ->
                    snackbar.showSnackbar(event.text)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("بازبینی سؤال‌ها")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        enabled = !state.isSaving
                    ) {
                        Icon(
                            Icons.Outlined.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbar)
        }
    ) { padding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.reviewRequiredCount > 0) {
                Text(
                    text = "${state.reviewRequiredCount} سؤال نیازمند بازبینی است.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(
                    items = state.questions,
                    key = { _, question -> question.id }
                ) { index, question ->
                    QuestionEditor(
                        question = question,
                        canMoveUp = index > 0,
                        canMoveDown = index < state.questions.lastIndex,
                        onNumberChange = { number ->
                            viewModel.updateQuestionNumber(
                                question.id,
                                number
                            )
                        },
                        onQuestionChange = { text ->
                            viewModel.updateQuestionText(
                                question.id,
                                text
                            )
                        },
                        onOptionChange = { key, text ->
                            viewModel.updateOptionText(
                                question.id,
                                key,
                                text
                            )
                        },
                        onMoveUp = {
                            viewModel.moveQuestionUp(question.id)
                        },
                        onMoveDown = {
                            viewModel.moveQuestionDown(question.id)
                        },
                        onDelete = {
                            viewModel.deleteQuestion(question.id)
                        }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = viewModel::addQuestion,
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = null
                    )
                    Text("افزودن سؤال")
                }

                Button(
                    onClick = viewModel::saveAndContinue,
                    enabled = state.canSave,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    } else {
                        Text("شروع تحلیل")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionEditor(
    question: ExamQuestion,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onNumberChange: (Int) -> Unit,
    onQuestionChange: (String) -> Unit,
    onOptionChange: (String, String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = question.number.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let(onNumberChange)
                    },
                    label = {
                        Text("شماره")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp
                ) {
                    Icon(
                        Icons.Outlined.ArrowUpward,
                        contentDescription = "انتقال به بالا"
                    )
                }

                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown
                ) {
                    Icon(
                        Icons.Outlined.ArrowDownward,
                        contentDescription = "انتقال به پایین"
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "حذف سؤال"
                    )
                }
            }

            OutlinedTextField(
                value = question.text,
                onValueChange = onQuestionChange,
                label = {
                    Text("متن سؤال")
                },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            question.options
                .sortedBy { it.key }
                .forEach { option ->
                    OutlinedTextField(
                        value = option.text,
                        onValueChange = { text ->
                            onOptionChange(option.key, text)
                        },
                        label = {
                            Text(
                                "گزینه ${option.displayLabel} (${option.key})"
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
        }
    }
}