package com.hoosha.examai.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiExamOption(
    val key: String,
    @SerialName("displayLabel")
    val displayLabel: String,
    val text: String
)

@Serializable
data class ApiExamQuestion(
    @SerialName("questionNumber")
    val questionNumber: Int,
    @SerialName("questionText")
    val questionText: String,
    val options: List<ApiExamOption>
)

@Serializable
data class ApiSourceChunk(
    @SerialName("sourceId")
    val sourceId: String,
    @SerialName("fileName")
    val fileName: String,
    val content: String,
    @SerialName("pageNumber")
    val pageNumber: Int? = null,
    val section: String? = null
)

@Serializable
data class AnalyzeExamRequest(
    @SerialName("examId")
    val examId: String,
    val questions: List<ApiExamQuestion>,
    val sources: List<ApiSourceChunk>
)

@Serializable
data class CreateJobResponse(
    @SerialName("job_id")
    val jobId: String,
    @SerialName("exam_id")
    val examId: String,
    val status: String
)

@Serializable
data class HealthResponse(
    val status: String,
    val service: String,
    val model: String
)

@Serializable
data class ApiCitation(
    @SerialName("sourceId")
    val sourceId: String,
    @SerialName("fileName")
    val fileName: String,
    @SerialName("pageNumber")
    val pageNumber: Int? = null,
    val section: String? = null,
    val quote: String
)

@Serializable
data class ApiOptionAnalysis(
    val option: String,
    @SerialName("isCorrect")
    val isCorrect: Boolean,
    val explanation: String
)

@Serializable
data class ApiQuestionAnswer(
    @SerialName("questionNumber")
    val questionNumber: Int,
    @SerialName("questionText")
    val questionText: String,
    @SerialName("correctOption")
    val correctOption: String? = null,
    @SerialName("correctOptionLabel")
    val correctOptionLabel: String? = null,
    @SerialName("correctOptionText")
    val correctOptionText: String? = null,
    val confidence: Double? = null,
    val status: String,
    val explanation: String,
    val citations: List<ApiCitation> = emptyList(),
    @SerialName("optionAnalysis")
    val optionAnalysis: List<ApiOptionAnalysis> = emptyList()
)

@Serializable
data class ApiExamResult(
    @SerialName("examId")
    val examId: String,
    val status: String,
    val answers: List<ApiQuestionAnswer>,
    val warnings: List<String> = emptyList()
)

@Serializable
data class JobResponse(
    @SerialName("job_id")
    val jobId: String,
    @SerialName("exam_id")
    val examId: String,
    val status: String,
    val progress: Int,
    val result: ApiExamResult? = null,
    val error: String? = null
)