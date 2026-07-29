package com.hoosha.examai.di

import android.content.Context
import androidx.room.Room
import com.hoosha.examai.data.local.ExamAiDatabase
import com.hoosha.examai.data.local.dao.AnalysisJobDao
import com.hoosha.examai.data.local.dao.AnswerDao
import com.hoosha.examai.data.local.dao.ExamDao
import com.hoosha.examai.data.local.dao.ExamImageDao
import com.hoosha.examai.data.local.dao.QuestionDao
import com.hoosha.examai.data.local.dao.SourceChunkDao
import com.hoosha.examai.data.local.dao.StudySourceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

 @Provides
 @Singleton
 fun provideDatabase(
 @ApplicationContext context: Context
 ): ExamAiDatabase = Room.databaseBuilder(
 context,
 ExamAiDatabase::class.java,
 ExamAiDatabase.DATABASE_NAME
 ).fallbackToDestructiveMigration(dropAllTables = true).build()

 @Provides
 fun provideStudySourceDao(database: ExamAiDatabase): StudySourceDao =
 database.studySourceDao()

 @Provides
 fun provideSourceChunkDao(database: ExamAiDatabase): SourceChunkDao =
 database.sourceChunkDao()

 @Provides
 fun provideExamDao(database: ExamAiDatabase): ExamDao =
 database.examDao()

 @Provides
 fun provideExamImageDao(database: ExamAiDatabase): ExamImageDao =
 database.examImageDao()

 @Provides
 fun provideQuestionDao(database: ExamAiDatabase): QuestionDao =
 database.questionDao()

 @Provides
 fun provideAnswerDao(database: ExamAiDatabase): AnswerDao =
 database.answerDao()

 @Provides
 fun provideAnalysisJobDao(database: ExamAiDatabase): AnalysisJobDao =
 database.analysisJobDao()
}