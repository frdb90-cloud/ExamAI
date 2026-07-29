from __future__ import annotations

from functools import lru_cache

from pydantic import Field, HttpUrl, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_environment: str = "development"

    ai_base_url: HttpUrl = Field(
        default="https://api.openai.com/v1"
    )
    ai_api_key: str = Field(default="")
    ai_model: str = Field(default="gpt-4.1-mini")
    ai_timeout_seconds: float = Field(default=120, ge=10, le=600)
    ai_temperature: float = Field(default=0.0, ge=0.0, le=2.0)

    retrieval_limit: int = Field(default=8, ge=1, le=20)
    request_delay_seconds: float = Field(default=0.0, ge=0.0, le=10.0)
    max_questions_per_request: int = Field(default=100, ge=1, le=500)
    max_sources_per_request: int = Field(default=500, ge=1, le=5000)

    allowed_origins: list[str] = Field(default=["*"])

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    @field_validator("ai_base_url")
    @classmethod
    def normalize_base_url(cls, value: HttpUrl) -> HttpUrl:
        return HttpUrl(str(value).rstrip("/"))

    @field_validator("ai_api_key")
    @classmethod
    def validate_api_key(cls, value: str) -> str:
        return value.strip()


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()