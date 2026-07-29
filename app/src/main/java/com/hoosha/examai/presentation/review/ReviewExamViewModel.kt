package com.hoosha.examai.presentation.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoosha.examai.data.repository.ExamRepository
import com.hoosha.examai.domain.model.ExamOption
import com.hoosha.examai.domain.model.ExamQuestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ReviewExamUiState(
    val examId: String = "",
    val questions: List<ExamQuestion> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    val reviewRequiredCount: Int
        get() = questions.count(ExamQuestion::reviewRequired)

    val isValid: Boolean
        get() = questions.isNotEmpty() &&
            questions.map(ExamQuestion::number).distinct().size ==
            questions.size &&
            questions.all { question ->
                question.number > 0 &&
                    question.text.isNotBlank() &&
                    question.options.size == 4 &&
                    question.options.map(ExamOption::key).toSet() ==
                    setOf("A", "B", "C", "D") &&
                    question.options.all { it.text.isNotBlank() }
            }

    val canSave: Boolean
        get() = isValid && !isLoading && !isSaving
}

sealed interface ReviewExamEvent {
    data class NavigateToAnalysis(
        val examId: String
    ) : ReviewExamEvent

    data class Message(
        val text: String
    ) : ReviewExamEvent
}

@HiltViewModel
class ReviewExamViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ExamRepository
) : ViewModel() {

    private val examId: String =
        requireNotNull(savedStateHandle["examId"]) {
            "شناسه آزمون در مسیر ناوبری وجود ندارد."
        }

    private val editedQuestions =
        MutableStateFlow<List<ExamQuestion>?>(null)

    private val saving = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    private val _events = MutableSharedFlow<ReviewExamEvent>()
    val events = _events.asSharedFlow()

    val uiState: StateFlow<ReviewExamUiState> =
        kotlinx.coroutines.flow.combine(
            repository.observeQuestions(examId),
            editedQuestions,
            saving,
            error
        ) { storedQuestions, localQuestions, isSaving, message ->
            ReviewExamUiState(
                examId = examId,
                questions = localQuestions ?: storedQuestions,
                isLoading = false,
                isSaving = isSaving,
                errorMessage = message
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReviewExamUiState(
                examId = examId
            )
        )

    fun updateQuestionNumber(
        questionId: String,
        number: Int
    ) {
        updateQuestion(questionId) {
            it.copy(
                number = number,
                reviewRequired = true
            )
        }
    }

    fun updateQuestionText(
        questionId: String,
        text: String
    ) {
        updateQuestion(questionId) {
            it.copy(
                text = text,
                reviewRequired = true
            )
        }
    }

    fun updateOptionText(
        questionId: String,
        optionKey: String,
        text: String
    ) {
        updateQuestion(questionId) { question ->
            question.copy(
                options = question.options.map { option ->
                    if (option.key == optionKey) {
                        option.copy(text = text)
                    } else {
                        option
                    }
                },
                reviewRequired = true
            )
        }
    }

    fun addQuestion() {
        val current = currentQuestions()
        val nextNumber = (
            current.maxOfOrNull(ExamQuestion::number) ?: 0
        ) + 1

        editedQuestions.value = current + ExamQuestion(
            id = UUID.randomUUID().toString(),
            number = nextNumber,
            text = "",
            options = defaultOptions(),
            reviewRequired = true
        )

        error.value = null
    }

    fun deleteQuestion(
        questionId: String
    ) {
        val updated = currentQuestions()
            .filterNot { it.id == questionId }

        if (updated.isEmpty()) {
            showMessage("حداقل یک سؤال باید باقی بماند.")
            return
        }

        editedQuestions.value = updated
        error.value = null
    }

    fun moveQuestionUp(
        questionId: String
    ) {
        moveQuestion(
            questionId = questionId,
            offset = -1
        )
    }

    fun moveQuestionDown(
        questionId: String
    ) {
        moveQuestion(
            questionId = questionId,
            offset = 1
        )
    }

    fun saveAndContinue() {
        val state = uiState.value

        if (!state.isValid) {
            showMessage(
                "متن سؤال‌ها و چهار گزینه A، B، C و D را کامل کنید."
            )
            return
        }

        if (state.isSaving) return

        viewModelScope.launch {
            saving.value = true
            error.value = null

            val normalized = state.questions
                .sortedBy(ExamQuestion::number)
                .map { question ->
                    question.copy(
                        text = question.text.trim(),
                        options = question.options
                            .sortedBy(ExamOption::key)
                            .map { option ->
                                option.copy(
                                    text = option.text.trim()
                                )
                            },
                        reviewRequired = false
                    )
                }

            repository.replaceQuestions(
                examId = examId,
                questions = normalized
            ).onSuccess {
                editedQuestions.value = normalized

                _events.emit(
                    ReviewExamEvent.NavigateToAnalysis(examId)
                )
            }.onFailure { throwable ->
                val message = throwable.userMessage()
                error.value = message

                _events.emit(
                    ReviewExamEvent.Message(message)
                )
            }

            saving.value = false
        }
    }

    fun consumeError() {
        error.value = null
    }

    private fun updateQuestion(
        questionId: String,
        transform: (ExamQuestion) -> ExamQuestion
    ) {
        editedQuestions.value = currentQuestions().map { question ->
            if (question.id == questionId) {
                transform(question)
            } else {
                question
            }
        }

        error.value = null
    }

    private fun moveQuestion(
        questionId: String,
        offset: Int
    ) {
        val questions = currentQuestions().toMutableList()
        val currentIndex = questions.indexOfFirst {
            it.id == questionId
        }

        if (currentIndex < 0) return

        val targetIndex = currentIndex + offset
        if (targetIndex !in questions.indices) return

        val current = questions[currentIndex]
        questions[currentIndex] = questions[targetIndex]
        questions[targetIndex] = current

        editedQuestions.value = questions
        error.value = null
    }

    private fun currentQuestions(): List<ExamQuestion> =
        editedQuestions.value ?: uiState.value.questions

    private fun defaultOptions(): List<ExamOption> = listOf(
        ExamOption(
            key = "A",
            displayLabel = "الف",
            text = ""
        ),
        ExamOption(
            key = "B",
            displayLabel = "ب",
            text = ""
        ),
        ExamOption(
            key = "C",
            displayLabel = "ج",
            text = ""
        ),
        ExamOption(
            key = "D",
            displayLabel = "د",
            text = ""
        )
    )

    private fun showMessage(
        message: String
    ) {
        error.value = message

        viewModelScope.launch {
            _events.emit(
                ReviewExamEvent.Message(message)
            )
        }
    }

    private fun Throwable.userMessage(): String =
        message?.takeIf(String::isNotBlank)
            ?: "ذخیره تغییرات با خطا روبه‌رو شد."
}