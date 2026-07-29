package com.hoosha.examai.data.repository

import com.hoosha.examai.data.local.dao.AnswerDao
import com.hoosha.examai.data.local.dao.ExamDao
import com.hoosha.examai.data.local.entity.CitationEntity
import com.hoosha.examai.data.local.entity.ExamAnswerEntity
import com.hoosha.examai.data.local.entity.OptionAnalysisEntity
import com.hoosha.examai.domain.model.Citation
import com.hoosha.examai.domain.model.ExamAnswer
import com.hoosha.examai.domain.model.OptionAnalysis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface ResultRepository {

    fun observeResults(
        examId: String
    ): Flow<List<ExamAnswer>>

    suspend fun saveResults(
        examId: String,
        answers: List<ExamAnswer>
    ): Result<Unit>
}

@Singleton
class DefaultResultRepository @Inject constructor(
    private val answerDao: AnswerDao,
    private val examDao: ExamDao
) : ResultRepository {

    override fun observeResults(
        examId: String
    ): Flow<List<ExamAnswer>> =
        answerDao.observeAnswers(examId).map { entities ->
            val citations = answerDao
                .getCitationsForExam(examId)
                .groupBy(CitationEntity::answerId)

            val optionAnalyses = answerDao
                .getOptionAnalysesForExam(examId)
                .groupBy(OptionAnalysisEntity::answerId)

            entities.map { answer ->
                ExamAnswer(
                    questionNumber = answer.questionNumber,
                    questionText = answer.questionText,
                    correctOption = answer.correctOption,
                    correctOptionLabel = answer.correctOptionLabel,
                    correctOptionText = answer.correctOptionText,
                    confidence = answer.confidence,
                    status = answer.status,
                    explanation = answer.explanation,
                    citations = citations[answer.id]
                        .orEmpty()
                        .sortedBy(CitationEntity::citationOrder)
                        .map {
                            Citation(
                                sourceId = it.sourceId,
                                fileName = it.fileName,
                                pageNumber = it.pageNumber,
                                section = it.section,
                                quote = it.quote
                            )
                        },
                    optionAnalysis = optionAnalyses[answer.id]
                        .orEmpty()
                        .sortedBy(OptionAnalysisEntity::optionOrder)
                        .map {
                            OptionAnalysis(
                                option = it.optionKey,
                                isCorrect = it.isCorrect,
                                explanation = it.explanation
                            )
                        }
                )
            }
        }

    override suspend fun saveResults(
        examId: String,
        answers: List<ExamAnswer>
    ): Result<Unit> = runCatching {
        require(answers.isNotEmpty()) {
            "پاسخی برای ذخیره وجود ندارد."
        }

        val exam = examDao.getById(examId)
            ?: error("آزمون پیدا نشد.")

        val now = System.currentTimeMillis()
        val answerEntities = mutableListOf<ExamAnswerEntity>()
        val citationEntities = mutableListOf<CitationEntity>()
        val optionEntities = mutableListOf<OptionAnalysisEntity>()

        answers.forEach { answer ->
            val answerId = UUID.randomUUID().toString()

            answerEntities += ExamAnswerEntity(
                id = answerId,
                examId = examId,
                questionNumber = answer.questionNumber,
                questionText = answer.questionText,
                correctOption = answer.correctOption,
                correctOptionLabel = answer.correctOptionLabel,
                correctOptionText = answer.correctOptionText,
                confidence = answer.confidence,
                status = answer.status,
                explanation = answer.explanation,
                createdAt = now
            )

            citationEntities += answer.citations.mapIndexed { index, citation ->
                CitationEntity(
                    id = UUID.randomUUID().toString(),
                    answerId = answerId,
                    sourceId = citation.sourceId,
                    fileName = citation.fileName,
                    pageNumber = citation.pageNumber,
                    section = citation.section,
                    quote = citation.quote,
                    citationOrder = index
                )
            }

            optionEntities += answer.optionAnalysis.mapIndexed { index, item ->
                OptionAnalysisEntity(
                    id = UUID.randomUUID().toString(),
                    answerId = answerId,
                    optionKey = item.option,
                    isCorrect = item.isCorrect,
                    explanation = item.explanation,
                    optionOrder = index
                )
            }
        }

        answerDao.replaceExamResults(
            examId = examId,
            answers = answerEntities,
            citations = citationEntities,
            optionAnalyses = optionEntities
        )

        examDao.upsert(
            exam.copy(
                status = "COMPLETED",
                answeredCount = answers.count {
                    it.status == "ANSWERED"
                },
                insufficientCount = answers.count {
                    it.status == "INSUFFICIENT_SOURCE"
                },
                updatedAt = now
            )
        )
    }
}