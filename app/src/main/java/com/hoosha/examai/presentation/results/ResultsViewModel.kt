package com.hoosha.examai.presentation.results

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoosha.examai.data.repository.ResultRepository
import com.hoosha.examai.domain.model.ExamAnswer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ResultsUiState(
    val examId: String = "",
    val answers: List<ExamAnswer> = emptyList(),
    val isLoading: Boolean = true
) {
    val answeredCount: Int
        get() = answers.count { it.status == "ANSWERED" }

    val insufficientCount: Int
        get() = answers.count {
            it.status == "INSUFFICIENT_SOURCE"
        }

    val averageConfidence: Double?
        get() {
            val values = answers.mapNotNull(ExamAnswer::confidence)
            return values.takeIf { it.isNotEmpty() }?.average()
        }
}

@HiltViewModel
class ResultsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: ResultRepository
) : ViewModel() {

    private val examId: String =
        requireNotNull(savedStateHandle["examId"]) {
            "شناسه آزمون در مسیر ناوبری وجود ندارد."
        }

    val uiState: StateFlow<ResultsUiState> =
        repository.observeResults(examId)
            .map { answers ->
                ResultsUiState(
                    examId = examId,
                    answers = answers,
                    isLoading = false
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ResultsUiState(
                    examId = examId
                )
            )
}