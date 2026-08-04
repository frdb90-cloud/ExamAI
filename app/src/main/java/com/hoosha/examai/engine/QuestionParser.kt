package com.hoosha.examai.engine

data class ParsedQuestion(
    val number: Int,
    val text: String,
    val options: List<String>
)

class QuestionParser {

    private val questionStartPattern = Regex(
        "(?m)^\\s*[\\(\\[]?\\s*([0-9\\u06F0-\\u06F9\\u0660-\\u0669]{1,3})" +
            "\\s*[\\)\\]\\.\\-:\\uFF1A]\\s*(.+)$"
    )

    private val optionMarkerPattern = Regex(
        "(?<![\\p{L}\\p{N}])" +
            "[\\(\\[]?\\s*" +
            "(الف|ا|ب|ج|د|A|B|C|D|a|b|c|d|[1-4]|[\\u06F1-\\u06F4])" +
            "\\s*[\\)\\]\\.\\-:\\uFF1A]" +
            "\\s*"
    )

    fun parse(
        rawText: String
    ): List<ParsedQuestion> {
        val normalizedText = normalizeDocument(rawText)

        if (normalizedText.isBlank()) {
            return emptyList()
        }

        val questionMatches = questionStartPattern
            .findAll(normalizedText)
            .toList()

        if (questionMatches.isEmpty()) {
            return parseSingleQuestion(normalizedText)
        }

        val results = mutableListOf<ParsedQuestion>()

        questionMatches.forEachIndexed { index, match ->
            val blockStart = match.range.first
            val blockEnd = if (index + 1 < questionMatches.size) {
                questionMatches[index + 1].range.first
            } else {
                normalizedText.length
            }

            val block = normalizedText
                .substring(blockStart, blockEnd)
                .trim()

            parseQuestionBlock(block)?.let(results::add)
        }

        return results
           .distinctBy { it.number }
           .sortedBy { it.number }
    }

    private fun parseQuestionBlock(
        block: String
    ): ParsedQuestion? {
        val headerMatch = questionStartPattern.find(block)
            ?: return null

        val questionNumber = convertDigits(
            headerMatch.groupValues[1]
        ).toIntOrNull() ?: return null

        val contentStart = headerMatch.groups[2]
            ?.range
            ?.first
            ?: return null

        val content = block
            .substring(contentStart)
            .trim()

        val optionMatches = optionMarkerPattern
            .findAll(content)
            .toList()
            .filter { match ->
                isLikelyOptionMarker(match.groupValues[1])
            }

        if (optionMatches.size < REQUIRED_OPTIONS) {
            return null
        }

        val selectedMatches = chooseFourOptionMatches(
            optionMatches
        ) ?: return null

        val questionText = content
            .substring(
                0,
                selectedMatches.first().range.first
            )
            .cleanPart()

        if (questionText.isBlank()) {
            return null
        }

        val options = selectedMatches.mapIndexed {
                index,
                optionMatch ->

            val optionStart = optionMatch.range.last + 1

            val optionEnd = if (
                index + 1 < selectedMatches.size
            ) {
                selectedMatches[index + 1].range.first
            } else {
                content.length
            }

            content
                .substring(optionStart, optionEnd)
                .cleanPart()
        }

        if (
            options.size != REQUIRED_OPTIONS ||
            options.any { it.isBlank() }
        ) {
            return null
        }

        return ParsedQuestion(
            number = questionNumber,
            text = questionText,
            options = options
        )
    }

    private fun chooseFourOptionMatches(
        matches: List<MatchResult>
    ): List<MatchResult>? {
        if (matches.size < REQUIRED_OPTIONS) {
            return null
        }

        val expectedSequences = listOf(
            listOf("الف", "ب", "ج", "د"),
            listOf("ا", "ب", "ج", "د"),
            listOf("a", "b", "c", "d"),
            listOf("1", "2", "3", "4")
        )

        for (startIndex in matches.indices) {
            val remaining = matches.drop(startIndex)

            if (remaining.size < REQUIRED_OPTIONS) {
                break
            }

            val candidate = remaining.take(REQUIRED_OPTIONS)
            val markers = candidate.map {
                normalizeMarker(it.groupValues[1])
            }

            if (expectedSequences.any { sequence ->
                    markers == sequence
                }
            ) {
                return candidate
            }
        }

        return matches.take(REQUIRED_OPTIONS)
    }

    private fun parseSingleQuestion(
        text: String
    ): List<ParsedQuestion> {
        val syntheticBlock = if (
            questionStartPattern.containsMatchIn(text)
        ) {
            text
        } else {
            "1) $text"
        }

        return listOfNotNull(
            parseQuestionBlock(syntheticBlock)
        )
    }

    private fun isLikelyOptionMarker(
        marker: String
    ): Boolean {
        return normalizeMarker(marker) in setOf(
            "الف",
            "ا",
            "ب",
            "ج",
            "د",
            "a",
            "b",
            "c",
            "d",
            "1",
            "2",
            "3",
            "4"
        )
    }

    private fun normalizeMarker(
        marker: String
    ): String {
        val normalized = marker
            .trim()
            .lowercase()
            .replace('\u064A', '\u06CC')
            .replace('\u0643', '\u06A9')

        return when (normalized) {
            "\u06F1", "\u0661" -> "1"
            "\u06F2", "\u0662" -> "2"
            "\u06F3", "\u0663" -> "3"
            "\u06F4", "\u0664" -> "4"
            else -> normalized
        }
    }

    private fun normalizeDocument(
        value: String
    ): String {
        return value
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace('\u064A', '\u06CC')
            .replace('\u0643', '\u06A9')
            .replace('\u200F', ' ')
            .replace('\u200E', ' ')
            .replace('\u202A', ' ')
            .replace('\u202B', ' ')
            .replace('\u202C', ' ')
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex(" *\\n *"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private fun String.cleanPart(): String {
        return replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim(
                '.',
                '-',
                ':',
                '\u061B',
                '\u060C'
            )
            .trim()
    }

    private fun convertDigits(
        value: String
    ): String {
        return buildString {
            value.forEach { character ->
                append(
                    when (character) {
                        '\u06F0', '\u0660' -> '0'
                        '\u06F1', '\u0661' -> '1'
                        '\u06F2', '\u0662' -> '2'
                        '\u06F3', '\u0663' -> '3'
                        '\u06F4', '\u0664' -> '4'
                        '\u06F5', '\u0665' -> '5'
                        '\u06F6', '\u0666' -> '6'
                        '\u06F7', '\u0667' -> '7'
                        '\u06F8', '\u0668' -> '8'
                        '\u06F9', '\u0669' -> '9'
                        else -> character
                    }
                )
            }
        }
    }

    private companion object {
        const val REQUIRED_OPTIONS = 4
    }
}
