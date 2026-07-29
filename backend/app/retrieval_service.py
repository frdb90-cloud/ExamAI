from __future__ import annotations

import math
import re
from collections import Counter

from app.schemas import ExamQuestion, SourceChunk


class RetrievalService:
    _token_pattern = re.compile(
        r"[A-Za-z0-9\u0600-\u06FF]+",
        flags=re.UNICODE,
    )

    _stop_words = {
        "از",
        "به",
        "در",
        "با",
        "برای",
        "که",
        "این",
        "آن",
        "را",
        "و",
        "یا",
        "یک",
        "است",
        "هست",
        "شود",
        "شده",
        "کدام",
        "گزینه",
        "صحیح",
        "نادرست",
        "the",
        "a",
        "an",
        "of",
        "to",
        "and",
        "or",
        "is",
        "are",
    }

    def retrieve(
        self,
        question: ExamQuestion,
        sources: list[SourceChunk],
        limit: int = 8,
    ) -> list[SourceChunk]:
        query = " ".join(
            [
                question.question_text,
                *[option.text for option in question.options],
            ]
        )
        query_tokens = self._tokens(query)

        scored: list[tuple[float, SourceChunk]] = []
        for source in sources:
            score = self._score(query_tokens, self._tokens(source.content))
            if score > 0:
                scored.append((score, source))

        scored.sort(
            key=lambda item: (
                -item[0],
                item[1].file_name,
                item[1].page_number or 0,
            )
        )
        return [source for _, source in scored[:limit]]

    def _tokens(self, text: str) -> list[str]:
        normalized = (
            text.lower()
            .replace("ي", "ی")
            .replace("ك", "ک")
            .replace("\u200c", " ")
        )
        return [
            token
            for token in self._token_pattern.findall(normalized)
            if len(token) > 1 and token not in self._stop_words
        ]

    @staticmethod
    def _score(
        query_tokens: list[str],
        document_tokens: list[str],
    ) -> float:
        if not query_tokens or not document_tokens:
            return 0.0

        query_counts = Counter(query_tokens)
        document_counts = Counter(document_tokens)
        overlap = set(query_counts) & set(document_counts)

        if not overlap:
            return 0.0

        weighted_overlap = sum(
            min(query_counts[token], document_counts[token])
            for token in overlap
        )

        query_norm = math.sqrt(
            sum(value * value for value in query_counts.values())
        )
        document_norm = math.sqrt(
            sum(value * value for value in document_counts.values())
        )

        if query_norm == 0 or document_norm == 0:
            return 0.0

        return weighted_overlap / (query_norm * document_norm)