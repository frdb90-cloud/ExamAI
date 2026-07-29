package com.hoosha.examai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hoosha.examai.data.local.dao.AnalysisJobDao
import com.hoosha.examai.data.local.dao.AnswerDao
import com.hoosha.examai.data.local.dao.ExamDao
import com.hoosha.examai.data.local.dao.ExamImageDao
import com.hoosha.examai.data.local.dao.QuestionDao
import com.hoosha.examai.data.local.dao.SourceChunkDao
import com.hoosha.examai.data.local.dao.StudySourceDao
import com.hoosha.examai.data.local.entity.AnalysisJobEntity
import com.hoosha.examai.data.local.entity.AnswerEntity
import com.hoosha.examai.data.local.entity.CitationEntity
import com.hoosha.examai.data.local.entity.ExamEntity
import com.hoosha.examai.data.local.entity.ExamImageEntity
import com.hoosha.examai.data.local.entity.OptionEntity
import com.hoosha.examai.data.local.entity.QuestionEntity
import com.hoosha.examai.data.local.entity.SourceChunkEntity
import com.hoosha.examai.data.local.entity.StudySourceEntity

@Database(
 entities = [
 StudySourceEntity::class,
 SourceChunkEntity::class,
 ExamEntity::class,
 ExamImageEntity::class,
 QuestionEntity::class,
 OptionEntity::class,
 AnswerEntity::class,
 CitationEntity::class,
 AnalysisJobEntity::class
 ],
 version = 1,
 exportSchema = true
)
abstract class ExamAiDatabase: RoomDatabase() {
 abstract fun studySourceDao(): StudySourceDao
 abstract fun sourceChunkDao(): SourceChunkDao
 abstract fun examDao(): ExamDao
 abstract fun examImageDao(): ExamImageDao
 abstract fun questionDao(): QuestionDao
 abstract fun answerDao(): AnswerDao
 abstract fun analysisJobDao(): AnalysisJobDao

 companion object {
 const val DATABASE_NAME = "exam_ai.db"
 }
}