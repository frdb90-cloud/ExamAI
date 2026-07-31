package com.hoosha.examai.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: StudySourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<SourceChunkEntity>)

    @Transaction
    suspend fun insertSourceWithChunks(
        source: StudySourceEntity,
        chunks: List<SourceChunkEntity>
    ) {
        insertSource(source)
        insertChunks(chunks)
    }

    @Query("SELECT * FROM study_sources ORDER BY createdAt DESC")
    fun observeSources(): Flow<List<StudySourceEntity>>

    @Query("SELECT * FROM source_chunks ORDER BY sourceId, chunkOrder")
    suspend fun getAllChunks(): List<SourceChunkEntity>

    @Query("DELETE FROM study_sources WHERE id = :sourceId")
    suspend fun deleteSource(sourceId: String)

    @Query("DELETE FROM study_sources")
    suspend fun deleteAllSources()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamSession(session: ExamSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswers(answers: List<ExamAnswerEntity>)

    @Transaction
    suspend fun insertExamWithAnswers(
        session: ExamSessionEntity,
        answers: List<ExamAnswerEntity>
    ) {
        insertExamSession(session)
        insertAnswers(answers)
    }

    @Query("SELECT * FROM exam_sessions ORDER BY createdAt DESC")
    fun observeExamSessions(): Flow<List<ExamSessionEntity>>

    @Query(
        "SELECT * FROM exam_answers WHERE examId = :examId ORDER BY questionNumber"
    )
    fun observeAnswers(examId: String): Flow<List<ExamAnswerEntity>>

    @Query("DELETE FROM exam_sessions WHERE id = :examId")
    suspend fun deleteExam(examId: String)
}