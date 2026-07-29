package com.hoosha.examai.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoosha.examai.data.repository.ExamRepository
import com.hoosha.examai.domain.model.ExamHistoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val exams: List<ExamHistoryItem> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface HistoryEvent {

    data class Message(
        val text: String
    ) : HistoryEvent
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ExamRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<HistoryEvent>()
    val events = _events.asSharedFlow()

    val uiState: StateFlow<HistoryUiState> =
        repository.observeHistory()
            .map { exams ->
                HistoryUiState(
                    exams = exams,
                    isLoading = false
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HistoryUiState()
            )

    fun delete(
        examId: String
    ) {
        viewModelScope.launch {
            repository.delete(examId)
                .onFailure { throwable ->
                    _events.emit(
                        HistoryEvent.Message(
                            throwable.message
                                ?: "حذف آزمون ناموفق بود."
                        )
                    )
                }
        }
    }
}