package com.hoosha.examai.presentation.analysis

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoosha.examai.data.repository.AnalysisRepository
import com.hoosha.examai.data.repository.ExamRepository
import com.hoosha.examai.domain.model.ExamAnswer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalysisUiState(
    val examId: String = "",
    val jobId: String? = null,
    val status: String = "IDLE",
    val progress: Int = 0,
    val answers: List<ExamAnswer> = emptyList(),
    val isRunning: Boolean = false,
    val errorMessage: String? = null
) {
    val isCompleted: Boolean
        get() = status == "COMPLETED"

    val answeredCount: Int
        get() = answers.count { it.status == "ANSWERED" }

    val insufficientCount: Int
        get() = answers.count {
            it.status == "INSUFFICIENT_SOURCE"
        }
}

sealed interface AnalysisEvent {
    data class NavigateToResults(
        val examId: String
    ) : AnalysisEvent

    data class Message(
        val text: String
    ) : AnalysisEvent
}

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val examRepository: ExamRepository,
    private val analysisRepository: AnalysisRepository
) : ViewModel() {

    private val examId: String =
        requireNotNull(savedStateHandle["examId"]) {
            "شناسه آزمون در مسیر ناوبری وجود ندارد."
        }

    private val _uiState = MutableStateFlow(
        AnalysisUiState(examId = examId)
    )
    val uiState: StateFlow<AnalysisUiState> =
        _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AnalysisEvent>()
    val events = _events.asSharedFlow()

    private var analysisJob: Job? = null

    fun start() {
        if (analysisJob?.isActive == true) return
        if (_uiState.value.isCompleted) return

        analysisJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    status = "PREPARING",
                    progress = 1,
                    answers = emptyList(),
                    isRunning = true,
                    errorMessage = null
                )
            }

            val questions = runCatching {
                examRepository.getQuestions(examId)
            }.getOrElse { throwable ->
                fail(throwable.userMessage())
                return@launch
            }

            analysisRepository.analyze(
                examId = examId,
                questions = questions
            ).catch { throwable ->
                if (throwable is CancellationException) {
                    throw throwable
                }

                fail(throwable.userMessage())
            }.collect { progress ->
                _uiState.update {
                    it.copy(
                        jobId = progress.jobId ?: it.jobId,
                        status = progress.status,
                        progress = progress.progress.coerceIn(0, 100),
                        answers = if (
                            progress.answers.isNotEmpty()
                        ) {
                            progress.answers
                        } else {
                            it.answers
                        },
                        isRunning = progress.status in RUNNING_STATUSES,
                        errorMessage = progress.error
                    )
                }

                if (progress.status == "COMPLETED") {
                    _events.emit(
                        AnalysisEvent.NavigateToResults(examId)
                    )
                }
            }
        }
    }

    fun cancel() {
        val jobId = _uiState.value.jobId

        analysisJob?.cancel()
        analysisJob = null

        _uiState.update {
            it.copy(
                status = "CANCELLED",
                isRunning = false,
                progress = 100
            )
        }

        if (jobId != null) {
            viewModelScope.launch {
                analysisRepository.cancel(jobId)
                    .onFailure { throwable ->
                        _events.emit(
                            AnalysisEvent.Message(
                                throwable.userMessage()
                            )
                        )
                    }
            }
        }
    }

    fun retry() {
        analysisJob?.cancel()
        analysisJob = null

        _uiState.value = AnalysisUiState(
            examId = examId
        )

        start()
    }

    fun consumeError() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }

    private suspend fun fail(
        message: String
    ) {
        _uiState.update {
            it.copy(
                status = "FAILED",
                isRunning = false,
                errorMessage = message
            )
        }

        _events.emit(
            AnalysisEvent.Message(message)
        )
    }

    private fun Throwable.userMessage(): String =
        message?.takeIf(String::isNotBlank)
            ?: "تحلیل آزمون با خطا روبه‌رو شد."

    private companion object {
        val RUNNING_STATUSES = setOf(
            "PREPARING",
            "QUEUED",
            "PROCESSING"
        )
    }
}