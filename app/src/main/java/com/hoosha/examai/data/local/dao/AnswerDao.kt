package com.hoosha.examai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.hoosha.examai.data.local.entity.CitationEntity
import com.hoosha.examai.data.local.entity.ExamAnswerEntity
import com.hoosha.examai.data.local.entity.OptionAnalysisEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnswerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswers(
        answers: List<ExamAnswerEntity>
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCitations(
        citations: List<CitationEntity>
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptionAnalyses(
        analyses: List<OptionAnalysisEntity>
    )

    @Query(
        """
        SELECT * FROM exam_answers
        WHERE examId = :examId
        ORDER BY questionNumber ASC
        """
    )
    fun observeAnswers(
        examId: String
    ): Flow<List<ExamAnswerEntity>>

    @Query(
        """
        SELECT * FROM answer_citations
        WHERE answerId IN (
            SELECT id FROM exam_answers WHERE examId = :examId
        )
        ORDER BY answerId, citationOrder ASC
        """
    )
    suspend fun getCitationsForExam(
        examId: String
    ): List<CitationEntity>

    @Query(
        """
        SELECT * FROM option_analyses
        WHERE answerId IN (
            SELECT id FROM exam_answers WHERE examId = :examId
        )
        ORDER BY answerId, optionOrder ASC
        """
    )
    suspend fun getOptionAnalysesForExam(
        examId: String
    ): List<OptionAnalysisEntity>

    @Query("DELETE FROM exam_answers WHERE examId = :examId")
    suspend fun deleteForExam(
        examId: String
    )

    @Transaction
    suspend fun replaceExamResults(
        examId: String,
        answers: List<ExamAnswerEntity>,
        citations: List<CitationEntity>,
        optionAnalyses: List<OptionAnalysisEntity>
    ) {
        deleteForExam(examId)

        if (answers.isNotEmpty()) {
            insertAnswers(answers)
        }

        if (citations.isNotEmpty()) {
            insertCitations(citations)
        }

        if (optionAnalyses.isNotEmpty()) {
            insertOptionAnalyses(optionAnalyses)
        }
    }
}