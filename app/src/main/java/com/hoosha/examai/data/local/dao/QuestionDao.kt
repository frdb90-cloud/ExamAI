package com.hoosha.examai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.hoosha.examai.data.local.entity.OptionEntity
import com.hoosha.examai.data.local.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(
        questions: List<QuestionEntity>
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptions(
        options: List<OptionEntity>
    )

    @Query(
        """
        SELECT * FROM questions
        WHERE examId = :examId
        ORDER BY questionNumber ASC
        """
    )
    fun observeForExam(
        examId: String
    ): Flow<List<QuestionEntity>>

    @Query(
        """
        SELECT * FROM questions
        WHERE examId = :examId
        ORDER BY questionNumber ASC
        """
    )
    suspend fun getForExam(
        examId: String
    ): List<QuestionEntity>

    @Query(
        """
        SELECT * FROM options
        WHERE questionId = :questionId
        ORDER BY optionOrder ASC
        """
    )
    suspend fun getOptions(
        questionId: String
    ): List<OptionEntity>

    @Query(
        """
        SELECT * FROM options
        WHERE questionId IN (
            SELECT id FROM questions WHERE examId = :examId
        )
        ORDER BY questionId, optionOrder ASC
        """
    )
    suspend fun getOptionsForExam(
        examId: String
    ): List<OptionEntity>

    @Query(
        """
        DELETE FROM questions
        WHERE examId = :examId
        """
    )
    suspend fun deleteForExam(
        examId: String
    )

    @Query(
        """
        DELETE FROM questions
        WHERE id = :id
        """
    )
    suspend fun deleteQuestion(
        id: String
    )

    @Transaction
    suspend fun replaceForExam(
        examId: String,
        questions: List<QuestionEntity>,
        options: List<OptionEntity>
    ) {
        deleteForExam(examId)

        if (questions.isNotEmpty()) {
            insertQuestions(questions)
        }

        if (options.isNotEmpty()) {
            insertOptions(options)
        }
    }
}