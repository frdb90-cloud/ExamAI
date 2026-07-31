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
        "\u0627\u0632", "\u0628\u0647", "\u062F\u0631", "\u0628\u0627",
        "\u0628\u0631\u0627\u06CC", "\u06A9\u0647", "\u0627\u06CC\u0646",
        "\u0622\u0646", "\u0631\u0627", "\u0648", "\u06CC\u0627",
        "\u0627\u0633\u062A", "\u0647\u0633\u062A", "\u0628\u0648\u062F",
        "\u0634\u062F", "\u0634\u0648\u062F", "\u0645\u06CC", "\u06CC\u06A9",
        "\u06A9\u062F\u0627\u0645", "\u06A9\u062F\u0627\u0645\u06CC\u06A9",
        "\u06AF\u0632\u06CC\u0646\u0647", "\u0639\u0628\u0627\u0631\u062A",
        "\u0645\u0648\u0631\u062F", "\u0645\u0648\u0627\u0631\u062F",
        "\u0632\u06CC\u0631", "\u0635\u062D\u06CC\u062D", "\u062F\u0631\u0633\u062A",
        "\u0646\u0627\u062F\u0631\u0633\u062A", "\u0646\u06CC\u0633\u062A",
        "\u0628\u0627\u0634\u062F", "\u0686\u0647", "\u0686\u0631\u0627"
    )

    fun findAnswer(
        question: ParsedQuestion,
        chunks: List<SourceChunkEntity>
    ): AnswerSuggestion {
        if (chunks.isEmpty()) {
            return AnswerSuggestion(
                optionIndex = null,
                confidence = 0f,
                evidence = "\u0627\u0628\u062A\u062F\u0627 \u062D\u062F\u0627\u0642\u0644 \u06CC\u06A9 \u0641\u0627\u06CC\u0644 \u062F\u0631\u0633\u06CC \u0648\u0627\u0631\u062F \u06A9\u0646\u06CC\u062F.",
                sourceName = null,
                status = "NO_SOURCE"
            )
        }

        if (question.options.size != 4) {
            return AnswerSuggestion(
                optionIndex = null,
                confidence = 0f,
                evidence = "\u0686\u0647\u0627\u0631 \u06AF\u0632\u06CC\u0646\u0647 \u06A9\u0627\u0645\u0644 \u0627\u0632 \u062A\u0635\u0648\u06CC\u0631 \u0634\u0646\u0627\u0633\u0627\u06CC\u06CC \u0646\u0634\u062F.",
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

            val bestMatch = rankedChunks.maxByOrNull { pair ->
                val chunk = pair.first
                val questionScore = pair.second
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
                evidence = "\u067E\u0627\u0633\u062E \u0642\u0627\u0628\u0644 \u0627\u062A\u06A9\u0627\u06CC\u06CC \u062F\u0631 \u0645\u0646\u0627\u0628\u0639 \u067E\u06CC\u062F\u0627 \u0646\u0634\u062F.",
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

        return if (
            normalizedOption.length >= 4 &&
            normalizedDocument.contains(normalizedOption)
        ) {
            1.0
        } else {
            0.0
        }
    }

    private fun normalize(text: String): String {
        return text
            .lowercase()
            .replace('\u064A', '\u06CC')
            .replace('\u0643', '\u06A9')
            .replace('\u06C0', '\u0647')
            .replace('\u0629', '\u0647')
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
            cleaned.take(500).trimEnd() + "\u2026"
        }
    }

    private data class OptionResult(
        val optionIndex: Int,
        val score: Double,
        val chunk: SourceChunkEntity?
    )
}
