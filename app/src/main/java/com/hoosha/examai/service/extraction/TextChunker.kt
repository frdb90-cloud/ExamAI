package com.hoosha.examai.domain.parser

import com.hoosha.examai.service.document.ExtractedDocument
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class TextChunk(
    val id: String,
    val content: String,
    val pageNumber: Int?,
    val section: String?,
    val order: Int
)

@Singleton
class TextChunker @Inject constructor() {

    fun chunk(
        document: ExtractedDocument,
        targetCharacters: Int = 3200,
        overlapCharacters: Int = 500
    ): List<TextChunk> {
        require(targetCharacters in 500..12000)
        require(overlapCharacters in 0 until targetCharacters)

        val result = mutableListOf<TextChunk>()
        var globalOrder = 0

        document.pages.forEach { page ->
            val normalized = normalize(page.text)
            if (normalized.isBlank()) return@forEach

            val pageChunks = splitText(
                text = normalized,
                targetCharacters = targetCharacters,
                overlapCharacters = overlapCharacters
            )

            pageChunks.forEach { content ->
                result += TextChunk(
                    id = UUID.randomUUID().toString(),
                    content = content,
                    pageNumber = page.pageNumber,
                    section = page.section,
                    order = globalOrder++
                )
            }
        }

        return result
    }

    private fun splitText(
        text: String,
        targetCharacters: Int,
        overlapCharacters: Int
    ): List<String> {
        if (text.length <= targetCharacters) return listOf(text)

        val chunks = mutableListOf<String>()
        var start = 0

        while (start < text.length) {
            var end = minOf(start + targetCharacters, text.length)

            if (end < text.length) {
                val preferredBreak = findPreferredBreak(
                    text = text,
                    lowerBound = start + targetCharacters / 2,
                    upperBound = end
                )
                if (preferredBreak > start) end = preferredBreak
            }

            val chunk = text.substring(start, end).trim()
            if (chunk.isNotBlank()) chunks += chunk

            if (end >= text.length) break

            val nextStart = (end - overlapCharacters)
                .coerceAtLeast(start + 1)

            start = nextStart
        }

        return chunks.distinct()
    }

    private fun findPreferredBreak(
        text: String,
        lowerBound: Int,
        upperBound: Int
    ): Int {
        val safeLower = lowerBound.coerceIn(0, text.length)
        val safeUpper = upperBound.coerceIn(safeLower, text.length)

        val separators = charArrayOf('\n', '.', '؟', '!', '؛')
        for (separator in separators) {
            val index = text.lastIndexOf(
                char = separator,
                startIndex = safeUpper - 1
            )
            if (index >= safeLower) return index + 1
        }

        val whitespace = text.lastIndexOf(
            char = ' ',
            startIndex = safeUpper - 1
        )
        return if (whitespace >= safeLower) whitespace + 1 else safeUpper
    }

    private fun normalize(text: String): String =
        text
            .replace("\u0000", "")
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace(Regex("[\\t ]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
}