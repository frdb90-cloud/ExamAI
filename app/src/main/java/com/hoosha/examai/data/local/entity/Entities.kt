package com.hoosha.examai.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
 tableName = "study_sources",
 indices = [
 Index(value = ["storedPath"], unique = true),
 Index(value = ["createdAt"])
 ]
)
data class StudySourceEntity(
 @PrimaryKey val id: String,
 val displayName: String,
 val mimeType: String,
 val originalUri: String,
 val storedPath: String,
 val sizeBytes: Long,
 val pageCount: Int?,
 val status: String,
 val errorMessage: String?,
 val createdAt: Long,
 val updatedAt: Long
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
 Index("sourceId"),
 Index(value = ["sourceId", "pageNumber"])
 ]
)
data class SourceChunkEntity(
 @PrimaryKey val id: String,
 val sourceId: String,
 val content: String,
 val pageNumber: Int?,
 val section: String?,
 val chunkOrder: Int,
 val createdAt: Long
)

@Entity(
 tableName = "exams",
 indices = [Index("createdAt")]
)
data class ExamEntity(
 @PrimaryKey val id: String,
 val title: String,
 val status: String,
 val questionCount: Int,
 val answeredCount: Int,
 val insufficientCount: Int,
 val createdAt: Long,
 val updatedAt: Long
)

@Entity(
 tableName = "exam_images",
 foreignKeys = [
 ForeignKey(
 entity = ExamEntity::class,
 parentColumns = ["id"],
 childColumns = ["examId"],
 onDelete = ForeignKey.CASCADE
 )
 ],
 indices = [Index("examId")]
)
data class ExamImageEntity(
 @PrimaryKey val id: String,
 val examId: String,
 val storedPath: String,
 val originalUri: String,
 val pageOrder: Int,
 val ocrText: String?,
 val createdAt: Long
)

@Entity(
 tableName = "questions",
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
 Index(value = ["examId", "questionNumber"], unique = true)
 ]
)
data class QuestionEntity(
 @PrimaryKey val id: String,
 val examId: String,
 val questionNumber: Int,
 val questionText: String,
 val reviewRequired: Boolean,
 val createdAt: Long
)

@Entity(
 tableName = "options",
 foreignKeys = [
 ForeignKey(
 entity = QuestionEntity::class,
 parentColumns = ["id"],
 childColumns = ["questionId"],
 onDelete = ForeignKey.CASCADE
 )
 ],
 indices = [
 Index("questionId"),
 Index(value = ["questionId", "optionKey"], unique = true)
 ]
)
data class OptionEntity(
 @PrimaryKey val id: String,
 val questionId: String,
 val optionKey: String,
 val displayLabel: String,
 val optionText: String,
 val optionOrder: Int
)

@Entity(
 tableName = "answers",
 foreignKeys = [
 ForeignKey(
 entity = QuestionEntity::class,
 parentColumns = ["id"],
 childColumns = ["questionId"],
 onDelete = ForeignKey.CASCADE
 )
 ],
 indices = [Index(value = ["questionId"], unique = true)]
)
data class AnswerEntity(
 @PrimaryKey val id: String,
 val questionId: String,
 val correctOption: String?,
 val correctOptionLabel: String?,
 val correctOptionText: String?,
 val confidence: Double?,
 val status: String,
 val explanation: String,
 val optionAnalysisJson: String,
 val createdAt: Long
)

@Entity(
 tableName = "citations",
 foreignKeys = [
 ForeignKey(
 entity = AnswerEntity::class,
 parentColumns = ["id"],
 childColumns = ["answerId"],
 onDelete = ForeignKey.CASCADE
 ),
 ForeignKey(
 entity = StudySourceEntity::class,
 parentColumns = ["id"],
 childColumns = ["sourceId"],
 onDelete = ForeignKey.CASCADE
 )
 ],
 indices = [
 Index("answerId"),
 Index("sourceId")
 ]
)
data class CitationEntity(
 @PrimaryKey val id: String,
 val answerId: String,
 val sourceId: String,
 val fileName: String,
 val pageNumber: Int?,
 val section: String?,
 val quote: String
)

@Entity(
 tableName = "analysis_jobs",
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
 Index("remoteJobId")
 ]
)
data class AnalysisJobEntity(
 @PrimaryKey val id: String,
 val examId: String,
 val remoteJobId: String?,
 val status: String,
 val progress: Int,
 val errorMessage: String?,
 val createdAt: Long,
 val updatedAt: Long
)