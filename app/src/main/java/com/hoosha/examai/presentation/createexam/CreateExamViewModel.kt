package com.hoosha.examai.presentation.createexam

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoosha.examai.data.repository.ExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateExamUiState(
    val title: String = "",
    val selectedImages: List<Uri> = emptyList(),
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
) {
    val canSubmit: Boolean
        get() = selectedImages.isNotEmpty() && !isProcessing
}

sealed interface CreateExamEvent {
    data class NavigateToReview(
        val examId: String
    ) : CreateExamEvent

    data class Message(
        val text: String
    ) : CreateExamEvent
}

@HiltViewModel
class CreateExamViewModel @Inject constructor(
    private val repository: ExamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateExamUiState())
    val uiState: StateFlow<CreateExamUiState> =
        _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CreateExamEvent>()
    val events = _events.asSharedFlow()

    fun updateTitle(
        value: String
    ) {
        _uiState.update {
            it.copy(
                title = value.take(100),
                errorMessage = null
            )
        }
    }

    fun setImages(
        uris: List<Uri>
    ) {
        _uiState.update {
            it.copy(
                selectedImages = uris
                    .distinct()
                    .take(MAX_IMAGE_COUNT),
                errorMessage = null
            )
        }
    }

    fun addImages(
        uris: List<Uri>
    ) {
        _uiState.update { state ->
            state.copy(
                selectedImages = (
                    state.selectedImages + uris
                ).distinct().take(MAX_IMAGE_COUNT),
                errorMessage = null
            )
        }
    }

    fun removeImage(
        uri: Uri
    ) {
        _uiState.update {
            it.copy(
                selectedImages = it.selectedImages - uri,
                errorMessage = null
            )
        }
    }

    fun clearImages() {
        _uiState.update {
            it.copy(
                selectedImages = emptyList(),
                errorMessage = null
            )
        }
    }

    fun processImages() {
        val state = _uiState.value

        if (state.isProcessing) return

        if (state.selectedImages.isEmpty()) {
            showError("حداقل یک تصویر آزمون انتخاب کنید.")
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    errorMessage = null
                )
            }

            repository.createFromImages(
                uris = state.selectedImages,
                title = state.title
            ).onSuccess { result ->
                _events.emit(
                    CreateExamEvent.NavigateToReview(
                        examId = result.examId
                    )
                )
            }.onFailure { error ->
                val message = error.userMessage()

                _uiState.update {
                    it.copy(errorMessage = message)
                }

                _events.emit(
                    CreateExamEvent.Message(message)
                )
            }

            _uiState.update {
                it.copy(isProcessing = false)
            }
        }
    }

    fun consumeError() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }

    private fun showError(
        message: String
    ) {
        _uiState.update {
            it.copy(errorMessage = message)
        }

        viewModelScope.launch {
            _events.emit(
                CreateExamEvent.Message(message)
            )
        }
    }

    private fun Throwable.userMessage(): String =
        message?.takeIf(String::isNotBlank)
            ?: "پردازش تصاویر با خطا روبه‌رو شد."

    private companion object {
        const val MAX_IMAGE_COUNT = 20
    }
}