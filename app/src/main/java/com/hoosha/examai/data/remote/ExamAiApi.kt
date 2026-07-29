package com.hoosha.examai.data.remote

import com.hoosha.examai.data.remote.model.AnalyzeExamRequest
import com.hoosha.examai.data.remote.model.CreateJobResponse
import com.hoosha.examai.data.remote.model.HealthResponse
import com.hoosha.examai.data.remote.model.JobResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ExamAiApi {

    @GET("health")
    suspend fun health(): HealthResponse

    @POST("api/v1/exams/analyze")
    suspend fun analyzeExam(
        @Body request: AnalyzeExamRequest
    ): CreateJobResponse

    @GET("api/v1/jobs/{jobId}")
    suspend fun getJob(
        @Path("jobId") jobId: String
    ): JobResponse

    @DELETE("api/v1/jobs/{jobId}")
    suspend fun cancelJob(
        @Path("jobId") jobId: String
    )
}