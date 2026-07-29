package com.hoosha.examai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.hoosha.examai.data.local.entity.AnalysisJobEntity
import com.hoosha.examai.data.local.entity.AnswerEntity
import com.hoosha.examai.data.local.entity.CitationEntity
import com.hoosha.examai.data.local.entity.ExamEntity
import com.hoosha.examai.data.local.entity.ExamImageEntity
import com.hoosha.examai.data.local.entity.OptionEntity
import com.hoosha.examai.data.local.entity.QuestionEntity
import com.hoosha.examai.data.local.entity.SourceChunkEntity
import com.hoosha.examai.data.local.entity.StudySourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySourceDao {
 @Query("SELECT * FROM study_sources ORDER BY createdAt DESC")
 fun observeAll(): Flow<List<StudySourceEntity>>

 @Query("SELECT * FROM study_sources WHERE id =:id LIMIT 1")
 suspend fun getById(id: String): StudySourceEntity?

 @Query("SELECT * FROM study_sources WHERE status = 'READY' ORDER BY createdAt DESC")
 suspend fun getReadySources(): List<StudySourceEntity>

 @Insert(onConflict = OnConflictStrategy.REPLACE)
 suspend fun upsert(source: StudySourceEntity)

 @Query(
 """
 UPDATE study_sources
 SET status =:status, errorMessage =:error, updatedAt =:updatedAt
 WHERE id =:id
 """
 )
 suspend fun updateStatus(
 id: String,
 status: String,
 error: String?,
 updatedAt: Long
 )

 @Query("DELETE FROM study_sources WHERE id =:id")
 suspend fun deleteById(id: String)

 @Query("SELECT COUNT(*) FROM study_sources")
 fun observeCount(): Flow<Int>
}

@Dao
interface SourceChunkDao {
 @Insert(onConflict = OnConflictStrategy.REPLACE)
 suspend fun insertAll(chunks: List<SourceChunkEntity>)

 @Query(
 """
 SELECT * FROM source_chunks
 WHERE sourceId =:sourceId
 ORDER BY chunkOrder ASC
 """
 )
 suspend fun getForSource(sourceId: String): List<SourceChunkEntity>

 @Query(
 """
 SELECT * FROM source_chunks
 WHERE content LIKE '%' ||:query || '%'
 ORDER BY sourceId, chunkOrder
 LIMIT:limit
 """
 )
 suspend fun search(query: String, limit: Int = 20): List<SourceChunkEntity>

 @Query("DELETE FROM source_chunks WHERE sourceId =:sourceId")
 suspend fun deleteForSource(sourceId: String)
}

@Dao
interface ExamDao {
 @Query("SELECT * FROM exams ORDER BY createdAt DESC")
 fun observeAll(): Flow<List<ExamEntity>>

 @Query("SELECT * FROM exams WHERE id =:id LIMIT 1")
 fun observeById(id: String): Flow<ExamEntity?>

 @Query("SELECT * FROM exams WHERE id =:id LIMIT 1")
 suspend fun getById(id: String): ExamEntity?

 @Insert(onConflict = OnConflictStrategy.REPLACE)
 suspend fun upsert(exam: ExamEntity)

 @Query("DELETE FROM exams WHERE id =:id")
 suspend fun deleteById(id: String)

 @Query(
 """
 UPDATE exams SET title =:title, updatedAt =:updatedAt
 WHERE id =:id
 """
 )
 suspend fun rename(id: String, title: String, updatedAt: Long)
}

@Dao
interface ExamImageDao {
 @Insert(onConflict = OnConflictStrategy.REPLACE)
 suspend fun insertAll(images: List<ExamImageEntity>)

 @Query("SELECT * FROM exam_images WHERE examId =:examId ORDER BY pageOrder")
 suspend fun getForExam(examId: String): List<ExamImageEntity>

 @Query("DELETE FROM exam_images WHERE id =:id")
 suspend fun deleteById(id: String)
}

@Dao
interface QuestionDao {
 @Insert(onConflict = OnConflictStrategy.REPLACE)
 suspend fun insertQuestions(questions: List<QuestionEntity>)

 @Insert(onConflict = OnConflictStrategy.REPLACE)
 suspend fun insertOptions(options: List<OptionEntity>)

 @Query("SELECT * FROM questions WHERE examId =:examId ORDER BY questionNumber")
 fun observeForExam(examId: String): Flow<List<QuestionEntity>>

 @Query("SELECT * FROM options WHERE questionId =:questionId ORDER BY optionOrder")
 suspend fun getOptions(questionId: String): List<OptionEntity>

 @Query("DELETE FROM questions WHERE id =:id")
 suspend fun deleteQuestion(id: String)
}

@Dao
interface AnswerDao {
 @Insert(onConflict = OnConflictStrategy.REPLACE)
 suspend fun insertAnswers(answers: List<AnswerEntity>)

 @Insert(onConflict = OnConflictStrategy.REPLACE)
 suspend fun insertCitations(citations: List<CitationEntity>)

 @Query(
 """
 SELECT answers.* FROM answers
 INNER JOIN questions ON questions.id = answers.questionId
 WHERE questions.examId =:examId
 ORDER BY questions.questionNumber
 """
 )
 fun observeForExam(examId: String): Flow<List<AnswerEntity>>

 @Query("SELECT * FROM citations WHERE answerId =:answerId")
 suspend fun getCitations(answerId: String): List<CitationEntity>
}

@Dao
interface AnalysisJobDao {
 @Insert(onConflict = OnConflictStrategy.REPLACE)
 suspend fun upsert(job: AnalysisJobEntity)

 @Query("SELECT * FROM analysis_jobs WHERE examId =:examId ORDER BY createdAt DESC LIMIT 1")
 fun observeLatestForExam(examId: String): Flow<AnalysisJobEntity?>

 @Query(
 """
 UPDATE analysis_jobs
 SET status =:status,
 progress =:progress,
 errorMessage =:error,
 updatedAt =:updatedAt
 WHERE id =:id
 """
 )
 suspend fun update(
 id: String,
 status: String,
 progress: Int,
 error: String?,
 updatedAt: Long
 )
}