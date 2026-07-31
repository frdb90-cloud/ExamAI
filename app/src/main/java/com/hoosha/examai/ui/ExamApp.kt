package com.hoosha.examai.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.hoosha.examai.data.ExamAnswerEntity
import com.hoosha.examai.data.OfflineExamRepository
import com.hoosha.examai.data.StudySourceEntity
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamApp() {
    val context = LocalContext.current
    val repository = remember {
        OfflineExamRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val sources by repository.observeSources()
        .collectAsState(initial = emptyList())

    var answers by remember {
        mutableStateOf<List<ExamAnswerEntity>>(emptyList())
    }
    var isBusy by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    fun analyzeImage(uri: Uri) {
        scope.launch {
            isBusy = true

            runCatching {
                repository.analyzeExamImage(uri)
            }.onSuccess { result ->
                answers = result.answers

                if (result.answers.isEmpty()) {
                    snackbarHostState.showSnackbar(
                        "ط³ط¤ط§ظ„ ع†ظ‡ط§ط±ع¯ط²غŒظ†ظ‡â€Œط§غŒ ع©ط§ظ…ظ„غŒ ط¯ط± طھطµظˆغŒط± ط´ظ†ط§ط³ط§غŒغŒ ظ†ط´ط¯."
                    )
                } else {
                    snackbarHostState.showSnackbar(
                        "${result.answers.size} ط³ط¤ط§ظ„ ط¨ط±ط±ط³غŒ ط´ط¯."
                    )
                }
            }.onFailure { error ->
                snackbarHostState.showSnackbar(
                    error.message ?: "ط®ظˆط§ظ†ط¯ظ† طھطµظˆغŒط± ط¢ط²ظ…ظˆظ† ظ†ط§ظ…ظˆظپظ‚ ط¨ظˆط¯."
                )
            }

            isBusy = false
        }
    }

    val sourceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            persistReadPermission(context, uri)

            scope.launch {
                isBusy = true

                runCatching {
                    repository.importSource(uri)
                }.onSuccess {
                    snackbarHostState.showSnackbar(
                        "ظ…ظ†ط¨ط¹ ط¯ط±ط³غŒ ط¨ط§ ظ…ظˆظپظ‚غŒطھ ط§ط¶ط§ظپظ‡ ط´ط¯."
                    )
                }.onFailure { error ->
                    snackbarHostState.showSnackbar(
                        error.message ?: "ط®ظˆط§ظ†ط¯ظ† ظپط§غŒظ„ ظ…ظ†ط¨ط¹ ظ†ط§ظ…ظˆظپظ‚ ط¨ظˆط¯."
                    )
                }

                isBusy = false
            }
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            analyzeImage(uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraUri

        if (success && uri != null) {
            analyzeImage(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("ط¢ط²ظ…ظˆظ†â€ŒغŒط§ط± ط¢ظپظ„ط§غŒظ†")
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "غ±. ظ…ظ†ط§ط¨ط¹ ط¯ط±ط³غŒ",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "ط§ط¨طھط¯ط§ ظپط§غŒظ„â€Œظ‡ط§غŒ PDF غŒط§ TXT ع©طھط§ط¨â€Œظ‡ط§ ظˆ ط¬ط²ظˆظ‡â€Œظ‡ط§ ط±ط§ ظˆط§ط±ط¯ ع©ظ†غŒط¯.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            sourceLauncher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "text/plain"
                                )
                            )
                        },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                        Text(
                            text = " ط§ظپط²ظˆط¯ظ† ظپط§غŒظ„ ط¯ط±ط³غŒ"
                        )
                    }
                }

                if (sources.isEmpty()) {
                    item {
                        InformationCard(
                            message = "ظ‡ظ†ظˆط² ظ‡غŒع† ظ…ظ†ط¨ط¹ ط¯ط±ط³غŒ ط§ط¶ط§ظپظ‡ ظ†ط´ط¯ظ‡ ط§ط³طھ."
                        )
                    }
                } else {
                    items(
                        items = sources,
                        key = { it.id }
                    ) { source ->
                        SourceCard(
                            source = source,
                            enabled = !isBusy,
                            onDelete = {
                                scope.launch {
                                    repository.deleteSource(source.id)
                                    snackbarHostState.showSnackbar(
                                        "ظ…ظ†ط¨ط¹ ط­ط°ظپ ط´ط¯."
                                    )
                                }
                            }
                        )
                    }
                }

                item {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "غ². طھطµظˆغŒط± ط¢ط²ظ…ظˆظ†",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "ط§ط² ط¨ط±ع¯ظ‡ ط¢ط²ظ…ظˆظ† ط¹ع©ط³ ط¨ع¯غŒط±غŒط¯ غŒط§ طھطµظˆغŒط± ظ…ظˆط¬ظˆط¯ ط¯ط± ع¯ظˆط´غŒ ط±ط§ ط§ظ†طھط®ط§ط¨ ع©ظ†غŒط¯.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val uri = createCameraUri(context)
                            cameraUri = uri
                            cameraLauncher.launch(uri)
                        },
                        enabled = !isBusy && sources.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null
                        )
                        Text(text = " ع¯ط±ظپطھظ† ط¹ع©ط³ ط¢ط²ظ…ظˆظ†")
                    }

                    OutlinedButton(
                        onClick = {
                            imageLauncher.launch("image/*")
                        },
                        enabled = !isBusy && sources.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null
                        )
                        Text(text = " ط§ظ†طھط®ط§ط¨ طھطµظˆغŒط± ط§ط² ع¯ظˆط´غŒ")
                    }

                    if (sources.isEmpty()) {
                        Text(
                            text = "ط¨ط±ط§غŒ ط¨ط±ط±ط³غŒ ط¢ط²ظ…ظˆظ† ط§ط¨طھط¯ط§ غŒع© ظ…ظ†ط¨ط¹ ط¯ط±ط³غŒ ط§ط¶ط§ظپظ‡ ع©ظ†غŒط¯.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (answers.isNotEmpty()) {
                    item {
                        HorizontalDivider()

                        Text(
                            text = "غ³. ظ¾ط§ط³ط®â€Œظ‡ط§غŒ ظ¾غŒط´ظ†ظ‡ط§ط¯غŒ",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(
                        items = answers,
                        key = { it.id }
                    ) { answer ->
                        AnswerCard(answer)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            if (isBusy) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("ط¯ط± ط­ط§ظ„ ظ¾ط±ط¯ط§ط²ط´â€¦")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceCard(
    source: StudySourceEntity,
    enabled: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = source.displayName,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${source.characterCount} ظ†ظˆغŒط³ظ‡ ط§ط³طھط®ط±ط§ط¬ ط´ط¯ظ‡",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(
                onClick = onDelete,
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "ط­ط°ظپ ظ…ظ†ط¨ط¹"
                )
            }
        }
    }
}

@Composable
private fun AnswerCard(
    answer: ExamAnswerEntity
) {
    val optionLetters = listOf("ط§ظ„ظپ", "ط¨", "ط¬", "ط¯")
    val selectedText = answer.selectedOptionIndex
        ?.takeIf { it in optionLetters.indices }
        ?.let { optionLetters[it] }
        ?: "ظ†ط§ظ…ط´ط®طµ"

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "ط³ط¤ط§ظ„ ${answer.questionNumber}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(answer.questionText)

            Text(
                text = answer.optionsText,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "ظ¾ط§ط³ط® ظ¾غŒط´ظ†ظ‡ط§ط¯غŒ: ع¯ط²غŒظ†ظ‡ $selectedText",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "ط§ط·ظ…غŒظ†ط§ظ† طھظ‚ط±غŒط¨غŒ: ${(answer.confidence * 100).toInt()}ظھ",
                style = MaterialTheme.typography.bodySmall
            )

            if (answer.sourceName != null) {
                Text(
                    text = "ظ…ظ†ط¨ط¹: ${answer.sourceName}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = answer.evidence,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun InformationCard(
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp)
        )
    }
}

private fun createCameraUri(
    context: Context
): Uri {
    val imageDirectory = File(
        context.cacheDir,
        "exam_images"
    ).apply {
        mkdirs()
    }

    val imageFile = File.createTempFile(
        "exam_",
        ".jpg",
        imageDirectory
    )

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.files",
        imageFile
    )
}

private fun persistReadPermission(
    context: Context,
    uri: Uri
) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}
