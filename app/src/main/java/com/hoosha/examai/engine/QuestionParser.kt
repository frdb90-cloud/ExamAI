package com.hoosha.examai.engine

data class ParsedQuestion(
    val number: Int,
    val text: String,
    val options: List<String>
)

class QuestionParser {

    private val questionMarkerPattern = Regex(
        "(?m)(?:^|\\n)\\s*" +
            "(?:[\\(\\[\\{]\\s*)?" +
            "([0-9\\u06F0-\\u06F9\\u0660-\\u0669]{1,2})" +
            "\\s*(?:[\\)\\]\\}]|[\\.\\-:\\uFF1A])" +
            "\\s*"
    )

    private val reversedQuestionMarkerPattern = Regex(
        "(?m)(?:^|\\n)\\s*" +
            "(?:[\\)\\]\\}]\\s*)?" +
            "([0-9\\u06F0-\\u06F9\\u0660-\\u0669]{1,2})" +
            "\\s*[\\(\\[\\{]" +
            "\\s*"
    )

    private val optionMarkerPattern = Regex(
        "(?<![\\p{L}\\p{N}])" +
            "(?:[\\(\\[\\{]\\s*)?" +
            "(ط§ظ„ظپ|ط§|ط¨|ظ¾|ط¬|ع†|ط¯|A|B|C|D|a|b|c|d)" +
            "\\s*(?:[\\)\\]\\}]|[\\.\\-:\\uFF1A])" +
            "\\s*"
    )

    fun parse(
        rawText: String
    ): List<ParsedQuestion> {
        val normalizedText = normalizeDocument(rawText)

        if (normalizedText.isBlank()) {
            return emptyList()
        }

        val candidates = mutableListOf<ParsedQuestion>()

        parseQuestionBlocks(
            text = normalizedText,
            markerPattern = questionMarkerPattern
        ).forEach(candidates::add)

        parseQuestionBlocks(
            text = normalizedText,
            markerPattern = reversedQuestionMarkerPattern
        ).forEach(candidates::add)

        if (candidates.isEmpty()) {
            parseQuestionWithoutNumber(
                normalizedText
            )?.let(candidates::add)
        }

        return candidates
            .groupBy { it.number }
            .mapNotNull { (_, versions) ->
                versions.maxByOrNull {
                    qualityScore(it)
                }
            }
            .sortedBy { it.number }
    }

    private fun parseQuestionBlocks(
        text: String,
        markerPattern: Regex
    ): List<ParsedQuestion> {
        val matches = markerPattern
            .findAll(text)
            .toList()

        if (matches.isEmpty()) {
            return emptyList()
        }

        val results = mutableListOf<ParsedQuestion>()

        matches.forEachIndexed { index, marker ->
            val number = convertDigits(
                marker.groupValues[1]
            ).toIntOrNull() ?: return@forEachIndexed

            if (number !in MINIMUM_QUESTION_NUMBER..
                MAXIMUM_QUESTION_NUMBER
            ) {
                return@forEachIndexed
            }

            val contentStart = marker.range.last + 1
            val contentEnd = if (index + 1 < matches.size) {
                matches[index + 1].range.first
            } else {
                text.length
            }

            if (contentEnd <= contentStart) {
                return@forEachIndexed
            }

            val content = text
                .substring(contentStart, contentEnd)
                .trim()

            parseContent(
                number = number,
                content = content
            )?.let(results::add)
        }

        return results
    }

    private fun parseContent(
        number: Int,
        content: String
    ): ParsedQuestion? {
        val optionMatches = optionMarkerPattern
            .findAll(content)
            .toList()

        val selectedMarkers = findOptionSequence(
            optionMatches
        ) ?: return null

        val firstMarker = selectedMarkers.first()

        val questionText = content
            .substring(0, firstMarker.range.first)
            .cleanPart()

        if (
            questionText.length <
            MINIMUM_QUESTION_TEXT_LENGTH
        ) {
            return null
        }

        val options = selectedMarkers.mapIndexed {
                index,
                marker ->

            val optionStart = marker.range.last + 1

            val optionEnd = if (
                index + 1 < selectedMarkers.size
            ) {
                selectedMarkers[index + 1].range.first
            } else {
                findTrailingBoundary(
                    content = content,
                    startIndex = optionStart
                )
            }

            if (optionEnd <= optionStart) {
                ""
            } else {
                content
                    .substring(optionStart, optionEnd)
                    .cleanPart()
            }
        }

        if (
            options.size != REQUIRED_OPTION_COUNT ||
            options.any {
                it.length < MINIMUM_OPTION_LENGTH
            }
        ) {
            return null
        }

        return ParsedQuestion(
            number = number,
            text = questionText,
            options = options
        )
    }

    private fun findOptionSequence(
        matches: List<MatchResult>
    ): List<MatchResult>? {
        if (matches.size < REQUIRED_OPTION_COUNT) {
            return null
        }

        for (start in 0..matches.size -
            REQUIRED_OPTION_COUNT
        ) {
            val candidate = matches.subList(
                start,
                start + REQUIRED_OPTION_COUNT
            )

            val markers = candidate.map {
                normalizeOptionMarker(
                    it.groupValues[1]
                )
            }

            if (
                markers == listOf(
                    OPTION_A,
                    OPTION_B,
                    OPTION_C,
                    OPTION_D
                )
            ) {
                return candidate
            }
        }

        return null
    }

    private fun normalizeOptionMarker(
        value: String
    ): String {
        return when (
            value
                .trim()
                .lowercase()
                .replace('\u064A', '\u06CC')
                .replace('\u0643', '\u06A9')
        ) {
            "\u0627",
            "\u0627\u0644\u0641",
            "a" -> OPTION_A

            "\u0628",
            "\u067E",
            "b" -> OPTION_B

            "\u062C",
            "\u0686",
            "c" -> OPTION_C

            "\u062F",
            "d" -> OPTION_D

            else -> ""
        }
    }

    private fun findTrailingBoundary(
        content: String,
        startIndex: Int
    ): Int {
        if (startIndex >= content.length) {
            return content.length
        }

        val remaining = content.substring(startIndex)

        val nextQuestion = questionMarkerPattern.find(
            remaining
        )

        val nextReversedQuestion =
            reversedQuestionMarkerPattern.find(
                remaining
            )

        val boundaries = listOfNotNull(
            nextQuestion?.range?.first,
            nextReversedQuestion?.range?.first
        )

        return if (boundaries.isEmpty()) {
            content.length
        } else {
            startIndex + boundaries.minOrNull()!!
        }
    }

    private fun parseQuestionWithoutNumber(
        text: String
    ): ParsedQuestion? {
        return parseContent(
            number = 1,
            content = text
        )
    }

    private fun qualityScore(
        question: ParsedQuestion
    ): Int {
        val questionScore = question.text.length
            .coerceAtMost(MAXIMUM_QUESTION_SCORE)

        val optionScore = question.options.sumOf {
            it.length.coerceAtMost(
                MAXIMUM_OPTION_SCORE
            )
        }

        return questionScore + optionScore
    }

    private fun normalizeDocument(
        value: String
    ): String {
        return value
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace('\u064A', '\u06CC')
            .replace('\u0643', '\u06A9')
            .replace('\u06C0', '\u0647')
            .replace('\u0629', '\u0647')
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
                '\u060C',
                '\u061F',
                '(',
                ')',
                '[',
                ']'
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
        const val OPTION_A = "A"
        const val OPTION_B = "B"
        const val OPTION_C = "C"
        const val OPTION_D = "D"

        const val REQUIRED_OPTION_COUNT = 4
        const val MINIMUM_QUESTION_NUMBER = 1
        const val MAXIMUM_QUESTION_NUMBER = 99
        const val MINIMUM_QUESTION_TEXT_LENGTH = 5
        const val MINIMUM_OPTION_LENGTH = 1

        const val MAXIMUM_QUESTION_SCORE = 500
        const val MAXIMUM_OPTION_SCORE = 250
    }
}
