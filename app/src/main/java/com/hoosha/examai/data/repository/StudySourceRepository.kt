package com.hoosha.examai.data.repository

import android.net.Uri
import com.hoosha.examai.data.local.dao.SourceChunkDao
import com.hoosha.examai.data.local.dao.StudySourceDao
import com.hoosha.examai.data.local.entity.SourceChunkEntity
import com.hoosha.examai.data.local.entity.StudySourceEntity
import com.hoosha.examai.domain.model.SourceStatus
import com.hoosha.examai.domain.model.StudySource
import com.hoosha.examai.domain.parser.TextChunker
import com.hoosha.examai.service.document.DocumentExtractor
import com.hoosha.examai.service.storage.PrivateFileStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface StudySourceRepository {
    fun observeSources(): Flow<List<StudySource>>
    fun observeSourceCount(): Flow<Int>
    suspend fun importAndProcess(uri: Uri): Result<String>
    suspend fun retry(sourceId: String): Result<Unit>
    suspend fun delete(sourceId: String): Result<Unit>
}

@Singleton
class DefaultStudySourceRepository @Inject constructor(
    private val sourceDao: StudySourceDao,
    private val chunkDao: SourceChunkDao,
    private val storage: PrivateFileStorage,
    private val extractor: DocumentExtractor,
    private val chunker: TextChunker
) : StudySourceRepository {

    override fun observeSources(): Flow<List<StudySource>> =
        sourceDao.observeAll().map { entities ->
            entities.map(StudySourceEntity::toDomain)
        }

    override fun observeSourceCount(): Flow<Int> =
        sourceDao.observeCount()

    override suspend fun importAndProcess(uri: Uri): Result<String> =
        runCatching {
            val stored = storage.copySource(uri)
            validateFile(stored.mimeType, stored.displayName, stored.sizeBytes)

            val now = System.currentTimeMillis()
            val id = UUID.randomUUID().toString()

            sourceDao.upsert(
                StudySourceEntity(
                    id = id,
                    displayName = stored.displayName,
                    mimeType = stored.mimeType,
                    originalUri = stored.originalUri,
                    storedPath = stored.path,
                    sizeBytes = stored.sizeBytes,
                    pageCount = null,
                    status = SourceStatus.PENDING.name,
                    errorMessage = null,
                    createdAt = now,
                    updatedAt = now
                )
            )

            process(id)
            id
        }

    override suspend fun retry(sourceId: String): Result<Unit> =
        runCatching {
            process(sourceId)
        }

    override suspend fun delete(sourceId: String): Result<Unit> =
        runCatching {
            val source = sourceDao.getById(sourceId)
                ?: return@runCatching

            sourceDao.deleteById(sourceId)
            storage.delete(source.storedPath)
        }

    private suspend fun process(sourceId: String) {
        val source = sourceDao.getById(sourceId)
            ?: error("منبع در پایگاه داده پیدا نشد.")

        sourceDao.updateStatus(
            id = sourceId,
            status = SourceStatus.EXTRACTING.name,
            error = null,
            updatedAt = System.currentTimeMillis()
        )

        try {
            val document = extractor.extract(
                file = File(source.storedPath),
                mimeType = source.mimeType
            )

            val chunks = chunker.chunk(document)
            if (chunks.isEmpty()) {
                error("متن قابل استفاده‌ای از این فایل استخراج نشد.")
            }

            chunkDao.deleteForSource(sourceId)
            chunkDao.insertAll(
                chunks.map { chunk ->
                    SourceChunkEntity(
                        id = chunk.id,
                        sourceId = sourceId,
                        content = chunk.content,
                        pageNumber = chunk.pageNumber,
                        section = chunk.section,
                        chunkOrder = chunk.order,
                        createdAt = System.currentTimeMillis()
                    )
                }
            )

            sourceDao.upsert(
                source.copy(
                    pageCount = document.pageCount,
                    status = SourceStatus.READY.name,
                    errorMessage = null,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } catch (error: CancellationException) {
            sourceDao.updateStatus(
                id = sourceId,
                status = SourceStatus.PENDING.name,
                error = null,
                updatedAt = System.currentTimeMillis()
            )
            throw error
        } catch (error: Throwable) {
            sourceDao.updateStatus(
                id = sourceId,
                status = SourceStatus.FAILED.name,
                error = error.userMessage(),
                updatedAt = System.currentTimeMillis()
            )
            throw error
        }
    }

    private fun validateFile(
        mimeType: String,
        fileName: String,
        sizeBytes: Long
    ) {
        require(sizeBytes in 1..MAX_FILE_SIZE) {
            "حجم فایل باید کمتر از ۵۰ مگابایت باشد."
        }

        val extension = fileName.substringAfterLast(
            delimiter = ".",
            missingDelimiterValue = ""
        ).lowercase()

        val supportedMimeTypes = setOf(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "text/plain",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )

        val supportedExtensions = setOf(
            "pdf",
            "jpg",
            "jpeg",
            "png",
            "txt",
            "docx"
        )

        require(
            mimeType in supportedMimeTypes ||
                extension in supportedExtensions
        ) {
            "فرمت فایل پشتیبانی نمی‌شود."
        }
    }

    private fun StudySourceEntity.toDomain() = StudySource(
        id = id,
        displayName = displayName,
        mimeType = mimeType,
        storedPath = storedPath,
        sizeBytes = sizeBytes,
        pageCount = pageCount,
        status = SourceStatus.from(status),
        errorMessage = errorMessage,
        createdAt = createdAt
    )

    private fun Throwable.userMessage(): String =
        message?.takeIf { it.isNotBlank() }?.take(300)
            ?: "پردازش فایل با خطا روبه‌رو شد."

    private companion object {
        const val MAX_FILE_SIZE = 50L * 1024L * 1024L
    }
}