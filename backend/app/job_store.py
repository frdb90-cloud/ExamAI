from __future__ import annotations

import asyncio
from copy import deepcopy
from typing import Any


class JobStore:
    def __init__(self) -> None:
        self._jobs: dict[str, dict[str, Any]] = {}
        self._lock = asyncio.Lock()

    async def create(
        self,
        job_id: str,
        exam_id: str,
    ) -> dict[str, Any]:
        job = {
            "job_id": job_id,
            "exam_id": exam_id,
            "status": "QUEUED",
            "progress": 0,
            "result": None,
            "error": None,
        }
        async with self._lock:
            self._jobs[job_id] = job
        return deepcopy(job)

    async def get(self, job_id: str) -> dict[str, Any] | None:
        async with self._lock:
            job = self._jobs.get(job_id)
            return deepcopy(job) if job is not None else None

    async def update(
        self,
        job_id: str,
        *,
        status: str | None = None,
        progress: int | None = None,
        result: dict[str, Any] | None = None,
        error: str | None = None,
    ) -> None:
        async with self._lock:
            job = self._jobs.get(job_id)
            if job is None:
                return

            if status is not None:
                job["status"] = status
            if progress is not None:
                job["progress"] = max(0, min(progress, 100))
            if result is not None:
                job["result"] = result
            if error is not None:
                job["error"] = error

    async def delete(self, job_id: str) -> bool:
        async with self._lock:
            job = self._jobs.get(job_id)
            if job is None:
                return False

            if job["status"] in {"QUEUED", "PROCESSING"}:
                job["status"] = "CANCELLED"
                job["progress"] = 100
                return True

            del self._jobs[job_id]
            return True

    async def is_cancelled(self, job_id: str) -> bool:
        async with self._lock:
            job = self._jobs.get(job_id)
            return job is None or job["status"] == "CANCELLED"