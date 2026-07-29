package com.hoosha.examai.presentation.createexam

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExamScreen(
    onBack: () -> Unit,
    onReview: (String) -> Unit,
    viewModel: CreateExamViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        viewModel.addImages(uris)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is CreateExamEvent.NavigateToReview ->
                    onReview(event.examId)

                is CreateExamEvent.Message ->
                    snackbar.showSnackbar(event.text)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("تصاویر آزمون")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        enabled = !state.isProcessing
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::updateTitle,
                enabled = !state.isProcessing,
                label = {
                    Text("عنوان آزمون")
                },
                placeholder = {
                    Text("مثلاً آزمون زیست فصل اول")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = {
                    imagePicker.launch(
                        arrayOf("image/jpeg", "image/png")
                    )
                },
                enabled = !state.isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("انتخاب تصاویر سؤال‌ها")
            }

            Text(
                text = "${state.selectedImages.size} تصویر انتخاب شده",
                style = MaterialTheme.typography.titleSmall
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = state.selectedImages,
                    key = Uri::toString
                ) { uri ->
                    SelectedImageRow(
                        uri = uri,
                        enabled = !state.isProcessing,
                        onRemove = {
                            viewModel.removeImage(uri)
                        }
                    )
                }
            }

            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = viewModel::processImages,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 10.dp),
                        strokeWidth = 2.dp
                    )
                    Text("در حال استخراج سؤال‌ها")
                } else {
                    Text("استخراج و بازبینی سؤال‌ها")
                }
            }
        }
    }
}

@Composable
private fun SelectedImageRow(
    uri: Uri,
    enabled: Boolean,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val displayName = remember(uri) {
        context.contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(
                    android.provider.OpenableColumns.DISPLAY_NAME
                )
                if (index >= 0) cursor.getString(index) else null
            } else {
                null
            }
        } ?: uri.lastPathSegment ?: "تصویر"
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayName,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )

            IconButton(
                onClick = onRemove,
                enabled = enabled
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "حذف تصویر"
                )
            }
        }
    }
}