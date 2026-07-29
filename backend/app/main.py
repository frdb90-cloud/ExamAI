from __future__ import annotations

import asyncio
import uuid
from contextlib import asynccontextmanager
from typing import Any

from fastapi import BackgroundTasks, FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.schemas import (
    AnalyzeExamRequest,
    CreateJobResponse,
    HealthResponse,
    JobResponse,
)
from app.services.ai_service import AiService
from app.services.job_store import JobStore
from app.services.retrieval_service import RetrievalService

job_store = JobStore()
retrieval_service = RetrievalService()
ai_service = AiService(settings=settings)


@asynccontextmanager
async def lifespan(_: FastAPI):
    yield
    await ai_service.close()


app = FastAPI(
    title="ExamAI Backend",
    description="Backend واسط برای تحلیل آزمون‌های چهارگزینه‌ای",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins,
    allow_credentials=False,
    allow_methods=["GET", "POST", "DELETE"],
    allow_headers=["Content-Type", "Authorization"],
)


@app.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    return HealthResponse(
        status="ok",
        service="ExamAI Backend",
        model=settings.ai_model,
    )


@app.post(
    "/api/v1/exams/analyze",
    response_model=CreateJobResponse,
    status_code=status.HTTP_202_ACCEPTED,
)
async def analyze_exam(
    request: AnalyzeExamRequest,
    background_tasks: BackgroundTasks,
) -> CreateJobResponse:
    job_id = str(uuid.uuid4())
    await job_store.create(job_id=job_id, exam_id=request.exam_id)

    background_tasks.add_task(
        process_exam,
        job_id,
        request,
    )

    return CreateJobResponse(
        job_id=job_id,
        exam_id=request.exam_id,
        status="QUEUED",
    )


@app.get("/api/v1/jobs/{job_id}", response_model=JobResponse)
async def get_job(job_id: str) -> JobResponse:
    job = await job_store.get(job_id)
    if job is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="کار پردازشی پیدا نشد.",
        )
    return JobResponse.model_validate(job)


@app.delete(
    "/api/v1/jobs/{job_id}",
    status_code=status.HTTP_204_NO_CONTENT,
)
async def delete_job(job_id: str) -> None:
    deleted = await job_store.delete(job_id)
    if not deleted:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="کار پردازشی پیدا نشد.",
        )


async def process_exam(
    job_id: str,
    request: AnalyzeExamRequest,
) -> None:
    try:
        await job_store.update(
            job_id,
            status="PROCESSING",
            progress=10,
        )

        answers: list[dict[str, Any]] = []
        total = max(len(request.questions), 1)

        for index, question in enumerate(request.questions):
            if await job_store.is_cancelled(job_id):
                return

            chunks = retrieval_service.retrieve(
                question=question,
                sources=request.sources,
                limit=settings.retrieval_limit,
            )

            answer = await ai_service.answer_question(
                question=question,
                source_chunks=chunks,
            )
            answers.append(answer.model_dump())

            progress = 10 + int(((index + 1) / total) * 85)
            await job_store.update(
                job_id,
                status="PROCESSING",
                progress=min(progress, 95),
            )

            if settings.request_delay_seconds > 0:
                await asyncio.sleep(settings.request_delay_seconds)

        result = {
            "examId": request.exam_id,
            "status": "COMPLETED",
            "answers": answers,
            "warnings": [],
        }

        await job_store.update(
            job_id,
            status="COMPLETED",
            progress=100,
            result=result,
        )

    except Exception as exc:
        await job_store.update(
            job_id,
            status="FAILED",
            progress=100,
            error=str(exc)[:500],
        )