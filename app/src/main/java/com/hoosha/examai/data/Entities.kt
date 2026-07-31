package com.hoosha.examai.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_sources"
)
data class StudySourceEntity(
    @PrimaryKey
    val id: String,
    val displayName: String,
    val mimeType: String,
    val originalUri: String,
    val characterCount: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "source_chunks",
    foreignKeys = [
        ForeignKey(
            entity = StudySourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sourceId")
    ]
)
data class SourceChunkEntity(
    @PrimaryKey
    val id: String,
    val sourceId: String,
    val sourceName: String,
    val chunkOrder: Int,
    val text: String,
    val normalizedText: String
)

@Entity(
    tableName = "exam_sessions"
)
data class ExamSessionEntity(
    @PrimaryKey
    val id: String,
    val imageUri: String,
    val extractedText: String,
    val questionCount: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "exam_answers",
    foreignKeys = [
        ForeignKey(
            entity = ExamSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["examId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("examId")
    ]
)
data class ExamAnswerEntity(
    @PrimaryKey
    val id: String,
    val examId: String,
    val questionNumber: Int,
    val questionText: String,
    val optionsText: String,
    val selectedOptionIndex: Int?,
    val confidence: Float,
    val evidence: String,
    val sourceName: String?,
    val status: String
)