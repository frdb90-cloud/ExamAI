package com.hoosha.examai.data.repository

import com.hoosha.examai.data.local.dao.SourceChunkDao
import com.hoosha.examai.data.local.dao.StudySourceDao
import com.hoosha.examai.data.remote.ExamAiApi
import com.hoosha.examai.data.remote.model.AnalyzeExamRequest
import com.hoosha.examai.data.remote.model.ApiExamOption
import com.hoosha.examai.data.remote.model.ApiExamQuestion
import com.hoosha.examai.data.remote.model.ApiSourceChunk
import com.hoosha.examai.domain.model.AnalysisProgress
import com.hoosha.examai.domain.model.Citation
import com.hoosha.examai.domain.model.ExamAnswer
import com.hoosha.examai.domain.model.ExamQuestion
import com.hoosha.examai.domain.model.OptionAnalysis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

interface AnalysisRepository {
    fun analyze(
        examId: String,
        questions: List<ExamQuestion>
    ): Flow<AnalysisProgress>

    suspend fun cancel(jobId: String): Result<Unit>
}

@Singleton
class DefaultAnalysisRepository @Inject constructor(
    private val api: ExamAiApi,
    private val sourceDao: StudySourceDao,
    private val chunkDao: SourceChunkDao
) : AnalysisRepository {

    override fun analyze(
        examId: String,
        questions: List<ExamQuestion>
    ): Flow<AnalysisProgress> = flow {
        require(questions.isNotEmpty()) {
            "هیچ سؤالی برای تحلیل وجود ندارد."
        }
        require(questions.all { it.options.size == 4 }) {
            "همه سؤال‌ها باید دقیقاً چهار گزینه داشته باشند."
        }

        emit(
            AnalysisProgress(
                jobId = null,
                status = "PREPARING",
                progress = 2
            )
        )

        val readySources = sourceDao.getReadySources()
        require(readySources.isNotEmpty()) {
            "حداقل یک منبع آماده برای تحلیل لازم است."
        }

        val chunks = readySources.flatMap { source ->
            chunkDao.getForSource(source.id).map { chunk ->
                ApiSourceChunk(
                    sourceId = source.id,
                    fileName = source.displayName,
                    content = chunk.content,
                    pageNumber = chunk.pageNumber,
                    section = chunk.section
                )
            }
        }

        require(chunks.isNotEmpty()) {
            "متن استخراج‌شده‌ای برای ارسال وجود ندارد."
        }

        val request = AnalyzeExamRequest(
            examId = examId,
            questions = questions.map { question ->
                ApiExamQuestion(
                    questionNumber = question.number,
                    questionText = question.text,
                    options = question.options.map {
                        ApiExamOption(
                            key = it.key,
                            displayLabel = it.displayLabel,
                            text = it.text
                        )
                    }
                )
            },
            sources = chunks
        )

        val created = api.analyzeExam(request)
        emit(
            AnalysisProgress(
                jobId = created.jobId,
                status = created.status,
                progress = 5
            )
        )

        var attempts = 0
        while (attempts < MAX_POLL_ATTEMPTS) {
            delay(POLL_INTERVAL_MS)
            attempts++

            val job = api.getJob(created.jobId)
            val resultAnswers = job.result?.answers.orEmpty().map { answer ->
                ExamAnswer(
                    questionNumber = answer.questionNumber,
                    questionText = answer.questionText,
                    correctOption = answer.correctOption,
                    correctOptionLabel = answer.correctOptionLabel,
                    correctOptionText = answer.correctOptionText,
                    confidence = answer.confidence,
                    status = answer.status,
                    explanation = answer.explanation,
                    citations = answer.citations.map {
                        Citation(
                            sourceId = it.sourceId,
                            fileName = it.fileName,
                            pageNumber = it.pageNumber,
                            section = it.section,
                            quote = it.quote
                        )
                    },
                    optionAnalysis = answer.optionAnalysis.map {
                        OptionAnalysis(
                            option = it.option,
                            isCorrect = it.isCorrect,
                            explanation = it.explanation
                        )
                    }
                )
            }

            emit(
                AnalysisProgress(
                    jobId = job.jobId,
                    status = job.status,
                    progress = job.progress,
                    answers = resultAnswers,
                    error = job.error
                )
            )

            when (job.status) {
                "COMPLETED" -> return@flow
                "FAILED" -> error(
                    job.error ?: "تحلیل آزمون در سرور ناموفق بود."
                )
                "CANCELLED" -> throw CancellationException(
                    "تحلیل آزمون لغو شد."
                )
            }
        }

        runCatching { api.cancelJob(created.jobId) }
        error("مهلت انتظار برای تحلیل آزمون به پایان رسید.")
    }

    override suspend fun cancel(jobId: String): Result<Unit> =
        runCatching {
            api.cancelJob(jobId)
        }

    private companion object {
        const val POLL_INTERVAL_MS = 2_000L
        const val MAX_POLL_ATTEMPTS = 300
    }
}