from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator


OptionKey = Literal["A", "B", "C", "D"]
AnswerStatus = Literal[
    "ANSWERED",
    "INSUFFICIENT_SOURCE",
    "OCR_REVIEW_REQUIRED",
    "FAILED",
]
JobStatus = Literal[
    "QUEUED",
    "PROCESSING",
    "COMPLETED",
    "FAILED",
    "CANCELLED",
]


class StrictModel(BaseModel):
    model_config = ConfigDict(
        extra="forbid",
        populate_by_name=True,
        str_strip_whitespace=True,
    )


class ExamOption(StrictModel):
    key: OptionKey
    display_label: str = Field(alias="displayLabel", min_length=1, max_length=10)
    text: str = Field(min_length=1, max_length=5000)


class ExamQuestion(StrictModel):
    question_number: int = Field(alias="questionNumber", ge=1)
    question_text: str = Field(alias="questionText", min_length=1, max_length=20000)
    options: list[ExamOption] = Field(min_length=2, max_length=10)

    @field_validator("options")
    @classmethod
    def unique_option_keys(
        cls,
        value: list[ExamOption],
    ) -> list[ExamOption]:
        keys = [option.key for option in value]
        if len(keys) != len(set(keys)):
            raise ValueError("کلید گزینه‌ها باید یکتا باشد.")
        return value


class SourceChunk(StrictModel):
    source_id: str = Field(alias="sourceId", min_length=1, max_length=100)
    file_name: str = Field(alias="fileName", min_length=1, max_length=500)
    content: str = Field(min_length=1, max_length=50000)
    page_number: int | None = Field(default=None, alias="pageNumber", ge=1)
    section: str | None = Field(default=None, max_length=500)


class AnalyzeExamRequest(StrictModel):
    exam_id: str = Field(alias="examId", min_length=1, max_length=100)
    questions: list[ExamQuestion] = Field(min_length=1, max_length=500)
    sources: list[SourceChunk] = Field(min_length=1, max_length=5000)


class Citation(StrictModel):
    source_id: str = Field(alias="sourceId")
    file_name: str = Field(alias="fileName")
    page_number: int | None = Field(default=None, alias="pageNumber")
    section: str | None = None
    quote: str


class OptionAnalysis(StrictModel):
    option: OptionKey
    is_correct: bool = Field(alias="isCorrect")
    explanation: str


class QuestionAnswer(StrictModel):
    question_number: int = Field(alias="questionNumber")
    question_text: str = Field(alias="questionText")
    correct_option: OptionKey | None = Field(
        default=None,
        alias="correctOption",
    )
    correct_option_label: str | None = Field(
        default=None,
        alias="correctOptionLabel",
    )
    correct_option_text: str | None = Field(
        default=None,
        alias="correctOptionText",
    )
    confidence: float | None = Field(default=None, ge=0.0, le=1.0)
    status: AnswerStatus
    explanation: str
    citations: list[Citation] = Field(default_factory=list)
    option_analysis: list[OptionAnalysis] = Field(
        default_factory=list,
        alias="optionAnalysis",
    )


class ExamResult(StrictModel):
    exam_id: str = Field(alias="examId")
    status: Literal["COMPLETED", "FAILED"]
    answers: list[QuestionAnswer]
    warnings: list[str] = Field(default_factory=list)


class CreateJobResponse(StrictModel):
    job_id: str
    exam_id: str
    status: JobStatus


class JobResponse(StrictModel):
    job_id: str
    exam_id: str
    status: JobStatus
    progress: int = Field(ge=0, le=100)
    result: dict | None = None
    error: str | None = None


class HealthResponse(StrictModel):
    status: str
    service: str
    model: str