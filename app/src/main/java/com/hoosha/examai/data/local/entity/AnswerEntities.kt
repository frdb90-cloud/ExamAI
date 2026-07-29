package com.hoosha.examai.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exam_answers",
    foreignKeys = [
        ForeignKey(
            entity = ExamEntity::class,
            parentColumns = ["id"],
            childColumns = ["examId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("examId"),
        Index(
            value = ["examId", "questionNumber"],
            unique = true
        )
    ]
)
data class ExamAnswerEntity(
    @PrimaryKey
    val id: String,
    val examId: String,
    val questionNumber: Int,
    val questionText: String,
    val correctOption: String?,
    val correctOptionLabel: String?,
    val correctOptionText: String?,
    val confidence: Double?,
    val status: String,
    val explanation: String,
    val createdAt: Long
)

@Entity(
    tableName = "answer_citations",
    foreignKeys = [
        ForeignKey(
            entity = ExamAnswerEntity::class,
            parentColumns = ["id"],
            childColumns = ["answerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("answerId")]
)
data class CitationEntity(
    @PrimaryKey
    val id: String,
    val answerId: String,
    val sourceId: String,
    val fileName: String,
    val pageNumber: Int?,
    val section: String?,
    val quote: String,
    val citationOrder: Int
)

@Entity(
    tableName = "option_analyses",
    foreignKeys = [
        ForeignKey(
            entity = ExamAnswerEntity::class,
            parentColumns = ["id"],
            childColumns = ["answerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("answerId")]
)
data class OptionAnalysisEntity(
    @PrimaryKey
    val id: String,
    val answerId: String,
    val optionKey: String,
    val isCorrect: Boolean,
    val explanation: String,
    val optionOrder: Int
)