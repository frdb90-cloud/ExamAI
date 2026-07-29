package com.hoosha.examai.domain.model

data class StudySource(
    val id: String,
    val displayName: String,
    val mimeType: String,
    val storedPath: String,
    val sizeBytes: Long,
    val pageCount: Int?,
    val status: SourceStatus,
    val errorMessage: String?,
    val createdAt: Long
)

enum class SourceStatus {
    PENDING,
    EXTRACTING,
    READY,
    FAILED,
    UNSUPPORTED;

    companion object {
        fun from(value: String): SourceStatus =
            entries.firstOrNull { it.name == value } ?: FAILED
    }
}

data class ExamOption(
    val key: String,
    val displayLabel: String,
    val text: String
)

data class ExamQuestion(
    val id: String,
    val number: Int,
    val text: String,
    val options: List<ExamOption>,
    val reviewRequired: Boolean
)

data class Citation(
    val sourceId: String,
    val fileName: String,
    val pageNumber: Int?,
    val section: String?,
    val quote: String
)

data class OptionAnalysis(
    val option: String,
    val isCorrect: Boolean,
    val explanation: String
)

data class ExamAnswer(
    val questionNumber: Int,
    val questionText: String,
    val correctOption: String?,
    val correctOptionLabel: String?,
    val correctOptionText: String?,
    val confidence: Double?,
    val status: String,
    val explanation: String,
    val citations: List<Citation>,
    val optionAnalysis: List<OptionAnalysis>
)

data class ExamHistoryItem(
    val id: String,
    val title: String,
    val status: String,
    val questionCount: Int,
    val answeredCount: Int,
    val insufficientCount: Int,
    val createdAt: Long
)

data class AnalysisProgress(
    val jobId: String?,
    val status: String,
    val progress: Int,
    val answers: List<ExamAnswer> = emptyList(),
    val error: String? = null
)