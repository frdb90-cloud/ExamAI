package com.hoosha.examai.data.repository

import android.net.Uri
import com.hoosha.examai.data.local.dao.ExamDao
import com.hoosha.examai.data.local.dao.ExamImageDao
import com.hoosha.examai.data.local.dao.QuestionDao
import com.hoosha.examai.data.local.entity.ExamEntity
import com.hoosha.examai.data.local.entity.ExamImageEntity
import com.hoosha.examai.data.local.entity.OptionEntity
import com.hoosha.examai.data.local.entity.QuestionEntity
import com.hoosha.examai.domain.model.ExamHistoryItem
import com.hoosha.examai.domain.model.ExamOption
import com.hoosha.examai.domain.model.ExamQuestion
import com.hoosha.examai.domain.parser.QuestionNormalizer
import com.hoosha.examai.service.ocr.OcrService
import com.hoosha.examai.service.storage.PrivateFileStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class OcrExamResult(
    val examId: String,
    val imageId: String,
    val rawText: String,
    val questions: List<ExamQuestion>
)

interface ExamRepository {
    fun observeHistory(): Flow<List<ExamHistoryItem>>
    fun observeQuestions(examId: String): Flow<List<ExamQuestion>>
    suspend fun createFromImages(
        uris: List<Uri>,
        title: String
    ): Result<OcrExamResult>
    suspend fun replaceQuestions(
        examId: String,
        questions: List<ExamQuestion>
    ): Result<Unit>
    suspend fun rename(examId: String, title: String): Result<Unit>
    suspend fun delete(examId: String): Result<Unit>
}

@Singleton
class DefaultExamRepository @Inject constructor(
    private val examDao: ExamDao,
    private val imageDao: ExamImageDao,
    private val questionDao: QuestionDao,
    private val storage: PrivateFileStorage,
    private val ocrService: OcrService
) : ExamRepository {

    override fun observeHistory(): Flow<List<ExamHistoryItem>> =
        examDao.observeAll().map { exams ->
            exams.map { exam ->
                ExamHistoryItem(
                    id = exam.id,
                    title = exam.title,
                    status = exam.status,
                    questionCount = exam.questionCount,
                    answeredCount = exam.answeredCount,
                    insufficientCount = exam.insufficientCount,
                    createdAt = exam.createdAt
                )
            }
        }

    override fun observeQuestions(
        examId: String
    ): Flow<List<ExamQuestion>> =
        questionDao.observeForExam(examId).map { questions ->
            questions.map { entity ->
                ExamQuestion(
                    id = entity.id,
                    number = entity.questionNumber,
                    text = entity.questionText,
                    options = questionDao.getOptions(entity.id).map {
                        ExamOption(
                            key = it.optionKey,
                            displayLabel = it.displayLabel,
                            text = it.optionText
                        )
                    },
                    reviewRequired = entity.reviewRequired
                )
            }
        }

    override suspend fun createFromImages(
        uris: List<Uri>,
        title: String
    ): Result<OcrExamResult> = runCatching {
        require(uris.isNotEmpty()) {
            "حداقل یک تصویر آزمون انتخاب کنید."
        }

        val now = System.currentTimeMillis()
        val examId = UUID.randomUUID().toString()

        examDao.upsert(
            ExamEntity(
                id = examId,
                title = title.ifBlank { "آزمون جدید" },
                status = "OCR_PROCESSING",
                questionCount = 0,
                answeredCount = 0,
                insufficientCount = 0,
                createdAt = now,
                updatedAt = now
            )
        )

        val allText = StringBuilder()
        val imageEntities = mutableListOf<ExamImageEntity>()

        uris.forEachIndexed { index, uri ->
            val stored = storage.copyExamImage(uri)
            require(
                stored.mimeType == "image/jpeg" ||
                    stored.mimeType == "image/png" ||
                    stored.displayName.endsWith(".jpg", true) ||
                    stored.displayName.endsWith(".jpeg", true) ||
                    stored.displayName.endsWith(".png", true)
            ) {
                "فرمت تصویر آزمون معتبر نیست."
            }

            val ocr = ocrService.recognize(Uri.fromFile(File(stored.path)))
            if (allText.isNotEmpty()) allText.appendLine()
            allText.append(ocr.text)

            imageEntities += ExamImageEntity(
                id = UUID.randomUUID().toString(),
                examId = examId,
                storedPath = stored.path,
                originalUri = stored.originalUri,
                pageOrder = index,
                ocrText = ocr.text,
                createdAt = System.currentTimeMillis()
            )
        }

        imageDao.insertAll(imageEntities)

        val parsed = QuestionNormalizer.parse(allText.toString())
        require(parsed.isNotEmpty()) {
            "هیچ سؤال قابل تشخیصی در تصاویر پیدا نشد."
        }

        val questions = parsed.map { parsedQuestion ->
            ExamQuestion(
                id = UUID.randomUUID().toString(),
                number = parsedQuestion.number,
                text = parsedQuestion.text,
                options = parsedQuestion.options.map {
                    ExamOption(
                        key = it.key,
                        displayLabel = it.displayLabel,
                        text = it.text
                    )
                },
                reviewRequired = parsedQuestion.reviewRequired
            )
        }

        saveQuestions(examId, questions)

        examDao.upsert(
            examDao.getById(examId)!!.copy(
                status = "REVIEW_REQUIRED",
                questionCount = questions.size,
                updatedAt = System.currentTimeMillis()
            )
        )

        OcrExamResult(
            examId = examId,
            imageId = imageEntities.first().id,
            rawText = allText.toString(),
            questions = questions
        )
    }

    override suspend fun replaceQuestions(
        examId: String,
        questions: List<ExamQuestion>
    ): Result<Unit> = runCatching {
        require(questions.isNotEmpty()) {
            "حداقل یک سؤال باید باقی بماند."
        }

        questions.forEach {
            require(it.number > 0) { "شماره سؤال معتبر نیست." }
            require(it.text.isNotBlank()) { "متن سؤال نباید خالی باشد." }
            require(it.options.map(ExamOption::key).distinct().size ==
                it.options.size
            ) {
                "کلید گزینه‌های هر سؤال باید یکتا باشد."
            }
        }

        val existing = examDao.getById(examId)
            ?: error("آزمون پیدا نشد.")

        questionDao.observeForExam(examId)

        saveQuestions(examId, questions)

        examDao.upsert(
            existing.copy(
                status = "READY_TO_ANALYZE",
                questionCount = questions.size,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun rename(
        examId: String,
        title: String
    ): Result<Unit> = runCatching {
        require(title.isNotBlank()) {
            "نام آزمون نباید خالی باشد."
        }
        examDao.rename(
            id = examId,
            title = title.trim().take(100),
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun delete(examId: String): Result<Unit> =
        runCatching {
            val images = imageDao.getForExam(examId)
            examDao.deleteById(examId)
            images.forEach { storage.delete(it.storedPath) }
        }

    private suspend fun saveQuestions(
        examId: String,
        questions: List<ExamQuestion>
    ) {
        questionDao.insertQuestions(
            questions.map {
                QuestionEntity(
                    id = it.id,
                    examId = examId,
                    questionNumber = it.number,
                    questionText = it.text.trim(),
                    reviewRequired = it.reviewRequired ||
                        it.options.size != 4,
                    createdAt = System.currentTimeMillis()
                )
            }
        )

        questionDao.insertOptions(
            questions.flatMap { question ->
                question.options.mapIndexed { index, option ->
                    OptionEntity(
                        id = UUID.randomUUID().toString(),
                        questionId = question.id,
                        optionKey = option.key,
                        displayLabel = option.displayLabel,
                        optionText = option.text.trim(),
                        optionOrder = index
                    )
                }
            }
        )
    }
}