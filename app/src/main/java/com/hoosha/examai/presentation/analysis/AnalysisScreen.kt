package com.hoosha.examai.presentation.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    onBack: () -> Unit,
    onCompleted: (String) -> Unit,
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.start()

        viewModel.events.collectLatest { event ->
            when (event) {
                is AnalysisEvent.NavigateToResults ->
                    onCompleted(event.examId)

                is AnalysisEvent.Message ->
                    snackbar.showSnackbar(event.text)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("تحلیل آزمون")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        enabled = !state.isRunning
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = state.status.persianTitle(),
                style = MaterialTheme.typography.headlineSmall
            )

            LinearProgressIndicator(
                progress = {
                    state.progress.coerceIn(0, 100) / 100f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            )

            Text(
                text = "${state.progress.coerceIn(0, 100)}٪",
                style = MaterialTheme.typography.headlineMedium
            )

            if (state.answers.isNotEmpty()) {
                Text(
                    text = "پاسخ قطعی: ${state.answeredCount}",
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    text = "منبع ناکافی: ${state.insufficientCount}"
                )
            }

            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }

            when {
                state.isRunning -> {
                    OutlinedButton(
                        onClick = viewModel::cancel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                    ) {
                        Text("لغو تحلیل")
                    }
                }

                state.status == "FAILED" ||
                    state.status == "CANCELLED" -> {
                    Button(
                        onClick = viewModel::retry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                    ) {
                        Text("تلاش دوباره")
                    }
                }
            }
        }
    }
}

private fun String.persianTitle(): String = when (this) {
    "IDLE" -> "آماده شروع"
    "PREPARING" -> "آماده‌سازی اطلاعات"
    "QUEUED" -> "در صف پردازش"
    "PROCESSING" -> "در حال تحلیل سؤال‌ها"
    "COMPLETED" -> "تحلیل کامل شد"
    "FAILED" -> "تحلیل ناموفق بود"
    "CANCELLED" -> "تحلیل لغو شد"
    else -> this
}