package com.hoosha.examai.presentation.sources

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoosha.examai.data.repository.StudySourceRepository
import com.hoosha.examai.domain.model.SourceStatus
import com.hoosha.examai.domain.model.StudySource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SourcesUiState(
    val sources: List<StudySource> = emptyList(),
    val sourceCount: Int = 0,
    val activeImports: Int = 0,
    val isLoading: Boolean = true
) {
    val readyCount: Int
        get() = sources.count { it.status == SourceStatus.READY }

    val failedCount: Int
        get() = sources.count { it.status == SourceStatus.FAILED }

    val canStartExam: Boolean
        get() = readyCount > 0 && activeImports == 0
}

sealed interface SourcesEvent {
    data class Message(
        val text: String
    ) : SourcesEvent
}

@HiltViewModel
class SourcesViewModel @Inject constructor(
    private val repository: StudySourceRepository
) : ViewModel() {

    private val activeImports = MutableStateFlow(0)
    private val importJobs = mutableMapOf<String, Job>()

    private val _events = MutableSharedFlow<SourcesEvent>()
    val events = _events.asSharedFlow()

    val uiState: StateFlow<SourcesUiState> = combine(
        repository.observeSources(),
        repository.observeSourceCount(),
        activeImports
    ) { sources, count, imports ->
        SourcesUiState(
            sources = sources,
            sourceCount = count,
            activeImports = imports,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SourcesUiState()
    )

    fun importFiles(
        uris: List<Uri>
    ) {
        if (uris.isEmpty()) return

        uris.distinct().forEach { uri ->
            val key = uri.toString()
            if (importJobs[key]?.isActive == true) {
                return@forEach
            }

            importJobs[key] = viewModelScope.launch {
                activeImports.value += 1

                try {
                    repository.importAndProcess(uri)
                        .onFailure { error ->
                            _events.emit(
                                SourcesEvent.Message(
                                    error.userMessage()
                                )
                            )
                        }
                } finally {
                    activeImports.value =
                        (activeImports.value - 1).coerceAtLeast(0)
                    importJobs.remove(key)
                }
            }
        }
    }

    fun retry(
        sourceId: String
    ) {
        viewModelScope.launch {
            repository.retry(sourceId)
                .onFailure { error ->
                    _events.emit(
                        SourcesEvent.Message(
                            error.userMessage()
                        )
                    )
                }
        }
    }

    fun delete(
        sourceId: String
    ) {
        viewModelScope.launch {
            repository.delete(sourceId)
                .onFailure { error ->
                    _events.emit(
                        SourcesEvent.Message(
                            error.userMessage()
                        )
                    )
                }
        }
    }

    private fun Throwable.userMessage(): String =
        message?.takeIf(String::isNotBlank)
            ?: "انجام عملیات با خطا روبه‌رو شد."
}