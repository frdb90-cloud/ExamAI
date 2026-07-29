package com.hoosha.examai.di

import com.hoosha.examai.data.local.ExamAiDatabase
import com.hoosha.examai.data.local.dao.AnswerDao
import com.hoosha.examai.data.repository.DefaultResultRepository
import com.hoosha.examai.data.repository.ResultRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ResultStorageModule {

    @Binds
    @Singleton
    abstract fun bindResultRepository(
        implementation: DefaultResultRepository
    ): ResultRepository

    companion object {

        @Provides
        fun provideAnswerDao(
            database: ExamAiDatabase
        ): AnswerDao = database.answerDao()
    }
}