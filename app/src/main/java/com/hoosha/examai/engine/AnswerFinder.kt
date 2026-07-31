package com.hoosha.examai.engine

import com.hoosha.examai.data.SourceChunkEntity
import kotlin.math.min

data class AnswerSuggestion(
    val optionIndex: Int?,
    val confidence: Float,
    val evidence: String,
    val sourceName: String?,
    val status: String
)

class AnswerFinder {

    private val stopWords = setOf(
        "از", "به", "در", "با", "برای", "که", "این", "آن", "را",
        "و", "یا", "است", "هست", "بود", "شد", "شود", "می", "یک",
        "کدام", "کدامیک", "گزینه", "عبارت", "مورد", "موارد", "زیر",
        "صحیح", "درست", "نادرست", "نیست", "باشد", "چه", "چرا"
    )

    fun findAnswer(
        question: ParsedQuestion,
        chunks: List<SourceChunkEntity>
    ): AnswerSuggestion {
        if (chunks.isEmpty()) {
            return AnswerSuggestion(
                optionIndex = null,
                confidence = 0f,
                evidence = "ابتدا حداقل یک فایل درسی وارد کنید.",
                sourceName = null,
                status = "NO_SOURCE"
            )
        }

        if (question.options.size != 4) {
            return AnswerSuggestion(
                optionIndex = null,
                confidence = 0f,
                evidence = "چهار گزینه کامل از تصویر شناسایی نشد.",
                sourceName = null,
                status = "INVALID_QUESTION"
            )
        }

        val questionTokens = tokenize(question.text)

        val rankedChunks = chunks
            .map { chunk ->
                chunk to overlapScore(
                    questionTokens,
                    tokenize(chunk.normalizedText)
                )
            }
            .sortedByDescending { it.second }
            .take(8)

        val optionResults = question.options.mapIndexed { index, option ->
            val optionTokens = tokenize(option)

            val bestMatch = rankedChunks.maxByOrNull { (chunk, questionScore) ->
                val chunkTokens = tokenize(chunk.normalizedText)
                val optionScore = overlapScore(optionTokens, chunkTokens)
                val phraseBonus = phraseScore(option, chunk.normalizedText)

                questionScore * 0.45 +
                    optionScore * 0.45 +
                    phraseBonus * 0.10
            }

            val chunk = bestMatch?.first
            val chunkTokens = tokenize(chunk?.normalizedText.orEmpty())

            val optionScore = overlapScore(optionTokens, chunkTokens)
            val questionScore = overlapScore(questionTokens, chunkTokens)
            val phraseBonus = phraseScore(
                option,
                chunk?.normalizedText.orEmpty()
            )

            OptionResult(
                optionIndex = index,
                score = questionScore * 0.45 +
                    optionScore * 0.45 +
                    phraseBonus * 0.10,
                chunk = chunk
            )
        }.sortedByDescending { it.score }

        val best = optionResults.first()
        val second = optionResults.getOrNull(1)
        val difference = best.score - (second?.score ?: 0.0)

        if (best.chunk == null || best.score < 0.08) {
            return AnswerSuggestion(
                optionIndex = null,
                confidence = 0f,
                evidence = "پاسخ قابل اتکایی در منابع پیدا نشد.",
                sourceName = null,
                status = "NOT_FOUND"
            )
        }

        val confidence = min(
            0.95,
            0.35 + best.score * 0.40 + difference * 0.50
        ).toFloat()

        return AnswerSuggestion(
            optionIndex = best.optionIndex,
            confidence = confidence,
            evidence = shortenEvidence(best.chunk.text),
            sourceName = best.chunk.sourceName,
            status = "FOUND"
        )
    }

    private fun tokenize(text: String): Set<String> {
        return normalize(text)
            .split(" ")
            .asSequence()
            .map { it.trim() }
            .filter { it.length >= 2 }
            .filterNot { it in stopWords }
            .toSet()
    }

    private fun overlapScore(
        queryTokens: Set<String>,
        documentTokens: Set<String>
    ): Double {
        if (queryTokens.isEmpty() || documentTokens.isEmpty()) {
            return 0.0
        }

        val commonCount = queryTokens.count { token ->
            token in documentTokens
        }

        return commonCount.toDouble() / queryTokens.size.toDouble()
    }

    private fun phraseScore(
        option: String,
        normalizedDocument: String
    ): Double {
        val normalizedOption = normalize(option)

        if (normalizedOption.length >= 4 &&
            normalizedDocument.contains(normalizedOption)
        ) {
            return 1.0
        }

        return 0.0
    }

    private fun normalize(text: String): String {
        return text
            .lowercase()
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('ۀ', 'ه')
            .replace('ة', 'ه')
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun shortenEvidence(text: String): String {
        val cleaned = text
            .replace(Regex("\\s+"), " ")
            .trim()

        return if (cleaned.length <= 500) {
            cleaned
        } else {
            cleaned.take(500).trimEnd() + "…"
        }
    }

    private data class OptionResult(
        val optionIndex: Int,
        val score: Double,
        val chunk: SourceChunkEntity?
    )
}