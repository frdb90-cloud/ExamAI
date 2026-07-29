package com.hoosha.examai.presentation.sources

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoosha.examai.domain.model.SourceStatus
import com.hoosha.examai.domain.model.StudySource
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    onCreateExam: () -> Unit,
    viewModel: SourcesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        viewModel.importFiles(uris)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SourcesEvent.Message ->
                    snackbar.showSnackbar(event.text)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("منابع درسی")
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbar)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    filePicker.launch(
                        arrayOf(
                            "application/pdf",
                            "text/plain",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "image/jpeg",
                            "image/png"
                        )
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "افزودن منبع"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SourceSummary(
                total = state.sourceCount,
                ready = state.readyCount,
                failed = state.failedCount
            )

            if (state.activeImports > 0) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "در حال پردازش ${state.activeImports} فایل",
                    modifier = Modifier.padding(16.dp)
                )
            }

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.sources.isEmpty() -> {
                    EmptySources(
                        modifier = Modifier.weight(1f)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = state.sources,
                            key = StudySource::id
                        ) { source ->
                            SourceCard(
                                source = source,
                                onRetry = {
                                    viewModel.retry(source.id)
                                },
                                onDelete = {
                                    viewModel.delete(source.id)
                                }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onCreateExam,
                enabled = state.canStartExam,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("ساخت آزمون جدید")
            }
        }
    }
}

@Composable
private fun SourceSummary(
    total: Int,
    ready: Int,
    failed: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryValue("همه", total)
            SummaryValue("آماده", ready)
            SummaryValue("ناموفق", failed)
        }
    }
}

@Composable
private fun SummaryValue(
    label: String,
    value: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SourceCard(
    source: StudySource,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = source.displayName,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = source.status.title(),
                    color = source.status.color(),
                    style = MaterialTheme.typography.bodyMedium
                )

                source.pageCount?.let { count ->
                    Text(
                        text = "$count صفحه",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                source.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (
                    source.status == SourceStatus.PENDING ||
                    source.status == SourceStatus.EXTRACTING
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (source.status == SourceStatus.FAILED) {
                IconButton(onClick = onRetry) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "تلاش دوباره"
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "حذف منبع"
                )
            }
        }
    }
}

@Composable
private fun EmptySources(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "هنوز منبعی اضافه نشده است.",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "فایل PDF، Word، متن یا تصویر اضافه کنید.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun SourceStatus.title(): String = when (this) {
    SourceStatus.PENDING -> "در انتظار"
    SourceStatus.EXTRACTING -> "در حال استخراج متن"
    SourceStatus.READY -> "آماده"
    SourceStatus.FAILED -> "ناموفق"
    SourceStatus.UNSUPPORTED -> "پشتیبانی‌نشده"
}

@Composable
private fun SourceStatus.color() = when (this) {
    SourceStatus.READY ->
        MaterialTheme.colorScheme.primary

    SourceStatus.FAILED,
    SourceStatus.UNSUPPORTED ->
        MaterialTheme.colorScheme.error

    else ->
        MaterialTheme.colorScheme.secondary
}