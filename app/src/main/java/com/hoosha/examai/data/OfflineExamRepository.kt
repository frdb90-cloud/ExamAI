package com.hoosha.examai.data

import android.content.Context
import android.net.Uri
import com.hoosha.examai.engine.AnswerFinder
import com.hoosha.examai.engine.DocumentExtractor
import com.hoosha.examai.engine.OfflineOcrEngine
import com.hoosha.examai.engine.ParsedQuestion
import com.hoosha.examai.engine.QuestionParser
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class ExamResult(
    val examId: String,
    val extractedText: String,
    val answers: List<ExamAnswerEntity>
)

class OfflineExamRepository(
    private val context: Context
) {
    private val dao = ExamDatabase.getInstance(context).examDao()
    private val documentExtractor = DocumentExtractor(context)
    private val ocrEngine = OfflineOcrEngine()
    private val questionParser = QuestionParser()
    private val answerFinder = AnswerFinder()

    fun observeSources(): Flow<List<StudySourceEntity>> {
        return dao.observeSources()
    }

    fun observeExamSessions(): Flow<List<ExamSessionEntity>> {
        return dao.observeExamSessions()
    }

    fun observeAnswers(examId: String): Flow<List<ExamAnswerEntity>> {
        return dao.observeAnswers(examId)
    }

    suspend fun importSource(uri: Uri): StudySourceEntity {
        val extractedDocument = documentExtractor.extract(uri)
        val sourceId = UUID.randomUUID().toString()

        val source = StudySourceEntity(
            id = sourceId,
            displayName = extractedDocument.displayName,
            mimeType = extractedDocument.mimeType,
            originalUri = uri.toString(),
            characterCount = extractedDocument.text.length
        )

        val chunks = documentExtractor
            .splitIntoChunks(extractedDocument.text)
            .mapIndexed { index, text ->
                SourceChunkEntity(
                    id = UUID.randomUUID().toString(),
                    sourceId = sourceId,
                    sourceName = extractedDocument.displayName,
                    chunkOrder = index,
                    text = text,
                    normalizedText = normalize(text)
                )
            }

        dao.insertSourceWithChunks(source, chunks)
        return source
    }

    suspend fun analyzeExamImage(uri: Uri): ExamResult {
        val extractedText = ocrEngine.recognize(context, uri)
        val questions = questionParser.parse(extractedText)
        val chunks = dao.getAllChunks()
        val examId = UUID.randomUUID().toString()

        val answers = questions.map { question ->
            createAnswer(examId, question, chunks)
        }

        val session = ExamSessionEntity(
            id = examId,
            imageUri = uri.toString(),
            extractedText = extractedText,
            questionCount = questions.size
        )

        dao.insertExamWithAnswers(session, answers)

        return ExamResult(
            examId = examId,
            extractedText = extractedText,
            answers = answers
        )
    }

    suspend fun deleteSource(sourceId: String) {
        dao.deleteSource(sourceId)
    }

    suspend fun deleteExam(examId: String) {
        dao.deleteExam(examId)
    }

    private fun createAnswer(
        examId: String,
        question: ParsedQuestion,
        chunks: List<SourceChunkEntity>
    ): ExamAnswerEntity {
        val suggestion = answerFinder.findAnswer(question, chunks)

        return ExamAnswerEntity(
            id = UUID.randomUUID().toString(),
            examId = examId,
            questionNumber = question.number,
            questionText = question.text,
            optionsText = question.options.joinToString("\n"),
            selectedOptionIndex = suggestion.optionIndex,
            confidence = suggestion.confidence,
            evidence = suggestion.evidence,
            sourceName = suggestion.sourceName,
            status = suggestion.status
        )
    }

    private fun normalize(text: String): String {
        return text
            .lowercase()
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}