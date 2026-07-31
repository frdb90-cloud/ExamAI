package com.hoosha.examai.ui

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.hoosha.examai.data.ExamAnswerEntity
import com.hoosha.examai.data.OfflineExamRepository
import com.hoosha.examai.data.StudySourceEntity
import kotlinx.coroutines.launch
import java.io.File

private object FaText {
    const val APP_TITLE = "\u0622\u0632\u0645\u0648\u0646\u200C\u06CC\u0627\u0631 \u0622\u0641\u0644\u0627\u06CC\u0646"
    const val SOURCES_TITLE = "\u06F1. \u0645\u0646\u0627\u0628\u0639 \u062F\u0631\u0633\u06CC"
    const val SOURCES_HELP = "\u0627\u0628\u062A\u062F\u0627 \u0641\u0627\u06CC\u0644\u200C\u0647\u0627\u06CC PDF \u06CC\u0627 TXT \u06A9\u062A\u0627\u0628\u200C\u0647\u0627 \u0648 \u062C\u0632\u0648\u0647\u200C\u0647\u0627 \u0631\u0627 \u0648\u0627\u0631\u062F \u06A9\u0646\u06CC\u062F."
    const val ADD_SOURCE = "\u0627\u0641\u0632\u0648\u062F\u0646 \u0641\u0627\u06CC\u0644 \u062F\u0631\u0633\u06CC"
    const val NO_SOURCE = "\u0647\u0646\u0648\u0632 \u0647\u06CC\u0686 \u0645\u0646\u0628\u0639 \u062F\u0631\u0633\u06CC \u0627\u0636\u0627\u0641\u0647 \u0646\u0634\u062F\u0647 \u0627\u0633\u062A."
    const val SOURCE_ADDED = "\u0645\u0646\u0628\u0639 \u062F\u0631\u0633\u06CC \u0628\u0627 \u0645\u0648\u0641\u0642\u06CC\u062A \u0627\u0636\u0627\u0641\u0647 \u0634\u062F."
    const val SOURCE_FAILED = "\u062E\u0648\u0627\u0646\u062F\u0646 \u0641\u0627\u06CC\u0644 \u0645\u0646\u0628\u0639 \u0646\u0627\u0645\u0648\u0641\u0642 \u0628\u0648\u062F."
    const val SOURCE_DELETED = "\u0645\u0646\u0628\u0639 \u062D\u0630\u0641 \u0634\u062F."
    const val DELETE_SOURCE = "\u062D\u0630\u0641 \u0645\u0646\u0628\u0639"
    const val CHARACTERS = "\u0646\u0648\u06CC\u0633\u0647 \u0627\u0633\u062A\u062E\u0631\u0627\u062C \u0634\u062F\u0647"

    const val EXAM_TITLE = "\u06F2. \u062A\u0635\u0648\u06CC\u0631 \u0622\u0632\u0645\u0648\u0646"
    const val EXAM_HELP = "\u0627\u0632 \u0628\u0631\u06AF\u0647 \u0622\u0632\u0645\u0648\u0646 \u0639\u06A9\u0633 \u0628\u06AF\u06CC\u0631\u06CC\u062F \u06CC\u0627 \u062A\u0635\u0648\u06CC\u0631 \u0645\u0648\u062C\u0648\u062F \u062F\u0631 \u06AF\u0648\u0634\u06CC \u0631\u0627 \u0627\u0646\u062A\u062E\u0627\u0628 \u06A9\u0646\u06CC\u062F."
    const val TAKE_PHOTO = "\u06AF\u0631\u0641\u062A\u0646 \u0639\u06A9\u0633 \u0622\u0632\u0645\u0648\u0646"
    const val SELECT_IMAGE = "\u0627\u0646\u062A\u062E\u0627\u0628 \u062A\u0635\u0648\u06CC\u0631 \u0627\u0632 \u06AF\u0648\u0634\u06CC"
    const val SOURCE_REQUIRED = "\u0628\u0631\u0627\u06CC \u0628\u0631\u0631\u0633\u06CC \u0622\u0632\u0645\u0648\u0646 \u0627\u0628\u062A\u062F\u0627 \u06CC\u06A9 \u0645\u0646\u0628\u0639 \u062F\u0631\u0633\u06CC \u0627\u0636\u0627\u0641\u0647 \u06A9\u0646\u06CC\u062F."
    const val IMAGE_FAILED = "\u062E\u0648\u0627\u0646\u062F\u0646 \u062A\u0635\u0648\u06CC\u0631 \u0622\u0632\u0645\u0648\u0646 \u0646\u0627\u0645\u0648\u0641\u0642 \u0628\u0648\u062F."
    const val NO_QUESTION = "\u0633\u0624\u0627\u0644 \u0686\u0647\u0627\u0631\u06AF\u0632\u06CC\u0646\u0647\u200C\u0627\u06CC \u06A9\u0627\u0645\u0644\u06CC \u062F\u0631 \u062A\u0635\u0648\u06CC\u0631 \u0634\u0646\u0627\u0633\u0627\u06CC\u06CC \u0646\u0634\u062F."
    const val QUESTIONS_CHECKED = "\u0633\u0624\u0627\u0644 \u0628\u0631\u0631\u0633\u06CC \u0634\u062F."

    const val RESULTS_TITLE = "\u06F3. \u067E\u0627\u0633\u062E\u200C\u0647\u0627\u06CC \u067E\u06CC\u0634\u0646\u0647\u0627\u062F\u06CC"
    const val QUESTION = "\u0633\u0624\u0627\u0644"
    const val SUGGESTED_ANSWER = "\u067E\u0627\u0633\u062E \u067E\u06CC\u0634\u0646\u0647\u0627\u062F\u06CC: \u06AF\u0632\u06CC\u0646\u0647"
    const val CONFIDENCE = "\u0627\u0637\u0645\u06CC\u0646\u0627\u0646 \u062A\u0642\u0631\u06CC\u0628\u06CC"
    const val SOURCE = "\u0645\u0646\u0628\u0639"
    const val UNKNOWN = "\u0646\u0627\u0645\u0634\u062E\u0635"
    const val PROCESSING = "\u062F\u0631 \u062D\u0627\u0644 \u067E\u0631\u062F\u0627\u0632\u0634\u2026"

    const val OPTION_A = "\u0627\u0644\u0641"
    const val OPTION_B = "\u0628"
    const val OPTION_C = "\u062C"
    const val OPTION_D = "\u062F"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamApp() {
    val context = LocalContext.current
    val repository = remember {
        OfflineExamRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val sources by repository.observeSources().collectAsState(initial = emptyList())

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
                    snackbarHostState.showSnackbar(FaText.NO_QUESTION)
                } else {
                    snackbarHostState.showSnackbar(
                        "${result.answers.size} ${FaText.QUESTIONS_CHECKED}"
                    )
                }
            }.onFailure { error ->
                snackbarHostState.showSnackbar(
                    error.message ?: FaText.IMAGE_FAILED
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
                    snackbarHostState.showSnackbar(FaText.SOURCE_ADDED)
                }.onFailure { error ->
                    snackbarHostState.showSnackbar(
                        error.message ?: FaText.SOURCE_FAILED
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
                    PersianText(
                        text = FaText.APP_TITLE,
                        style = MaterialTheme.typography.titleLarge
                    )
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

                    PersianText(
                        text = FaText.SOURCES_TITLE,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    PersianText(
                        text = FaText.SOURCES_HELP,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            sourceLauncher.launch(
                                arrayOf("application/pdf", "text/plain")
                            )
                        },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                        PersianText(text = FaText.ADD_SOURCE)
                    }
                }

                if (sources.isEmpty()) {
                    item {
                        InformationCard(FaText.NO_SOURCE)
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
                                        FaText.SOURCE_DELETED
                                    )
                                }
                            }
                        )
                    }
                }

                item {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(4.dp))

                    PersianText(
                        text = FaText.EXAM_TITLE,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    PersianText(
                        text = FaText.EXAM_HELP,
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
                        PersianText(text = FaText.TAKE_PHOTO)
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
                        PersianText(text = FaText.SELECT_IMAGE)
                    }

                    if (sources.isEmpty()) {
                        PersianText(
                            text = FaText.SOURCE_REQUIRED,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (answers.isNotEmpty()) {
                    item {
                        HorizontalDivider()

                        PersianText(
                            text = FaText.RESULTS_TITLE,
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
                            PersianText(text = FaText.PROCESSING)
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                PersianText(
                    text = source.displayName,
                    fontWeight = FontWeight.Bold
                )

                PersianText(
                    text = "${source.characterCount} ${FaText.CHARACTERS}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(
                onClick = onDelete,
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = FaText.DELETE_SOURCE
                )
            }
        }
    }
}

@Composable
private fun AnswerCard(answer: ExamAnswerEntity) {
    val optionLetters = listOf(
        FaText.OPTION_A,
        FaText.OPTION_B,
        FaText.OPTION_C,
        FaText.OPTION_D
    )

    val selectedText = answer.selectedOptionIndex
        ?.takeIf { it in optionLetters.indices }
        ?.let { optionLetters[it] }
        ?: FaText.UNKNOWN

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PersianText(
                text = "${FaText.QUESTION} ${answer.questionNumber}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            PersianText(text = answer.questionText)
            PersianText(
                text = answer.optionsText,
                style = MaterialTheme.typography.bodyMedium
            )

            PersianText(
                text = "${FaText.SUGGESTED_ANSWER} $selectedText",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            PersianText(
                text = "${FaText.CONFIDENCE}: ${(answer.confidence * 100).toInt()}\u066A",
                style = MaterialTheme.typography.bodySmall
            )

            if (answer.sourceName != null) {
                PersianText(
                    text = "${FaText.SOURCE}: ${answer.sourceName}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            PersianText(
                text = answer.evidence,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun InformationCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        PersianText(
            text = message,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun PersianText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle.Default,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    fontWeight: FontWeight? = null
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = style.copy(textDirection = TextDirection.Rtl),
        color = color,
        fontWeight = fontWeight
    )
}

private fun createCameraUri(context: Context): Uri {
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
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}
