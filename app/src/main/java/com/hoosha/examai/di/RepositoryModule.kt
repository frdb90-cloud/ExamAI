package com.hoosha.examai.di

import com.hoosha.examai.data.repository.AnalysisRepository
import com.hoosha.examai.data.repository.DefaultAnalysisRepository
import com.hoosha.examai.data.repository.DefaultExamRepository
import com.hoosha.examai.data.repository.DefaultStudySourceRepository
import com.hoosha.examai.data.repository.ExamRepository
import com.hoosha.examai.data.repository.StudySourceRepository
import com.hoosha.examai.service.ocr.MlKitOcrService
import com.hoosha.examai.service.ocr.OcrService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindStudySourceRepository(
        implementation: DefaultStudySourceRepository
    ): StudySourceRepository

    @Binds
    @Singleton
    abstract fun bindExamRepository(
        implementation: DefaultExamRepository
    ): ExamRepository

    @Binds
    @Singleton
    abstract fun bindAnalysisRepository(
        implementation: DefaultAnalysisRepository
    ): AnalysisRepository

    @Binds
    @Singleton
    abstract fun bindOcrService(
        implementation: MlKitOcrService
    ): OcrService
}