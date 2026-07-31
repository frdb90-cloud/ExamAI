package com.hoosha.examai.engine

data class ParsedQuestion(
    val number: Int,
    val text: String,
    val options: List<String>
)

class QuestionParser {

    private val questionStartPattern = Regex(
        """^\s*([۰-۹0-9]{1,3})\s*[\.\-ـ\):：]\s*(.*)$"""
    )

    private val optionPattern = Regex(
        """^\s*(?:[\(\[]?\s*)?([الفبپتثجچدABCDabcd1-4۱-۴])(?:\s*[\)\]\.\-ـ:：])\s*(.+)$"""
    )

    fun parse(rawText: String): List<ParsedQuestion> {
        val lines = rawText
            .replace("\r\n", "\n")
            .lines()
            .map { normalizeLine(it) }
            .filter { it.isNotBlank() }

        val blocks = splitQuestionBlocks(lines)
        val parsed = blocks.mapNotNull { parseBlock(it) }

        if (parsed.isNotEmpty()) {
            return parsed
        }

        return parseAsSingleQuestion(lines)
    }

    private fun splitQuestionBlocks(
        lines: List<String>
    ): List<List<String>> {
        val blocks = mutableListOf<MutableList<String>>()
        var current = mutableListOf<String>()

        for (line in lines) {
            val startsQuestion = questionStartPattern.matches(line)

            if (startsQuestion && current.isNotEmpty()) {
                blocks += current
                current = mutableListOf()
            }

            current += line
        }

        if (current.isNotEmpty()) {
            blocks += current
        }

        return blocks
    }

    private fun parseBlock(
        lines: List<String>
    ): ParsedQuestion? {
        if (lines.isEmpty()) return null

        val firstMatch = questionStartPattern.find(lines.first())
        val number = firstMatch
            ?.groupValues
            ?.getOrNull(1)
            ?.let(::convertPersianDigits)
            ?.toIntOrNull()
            ?: 1

        val questionParts = mutableListOf<String>()
        val options = mutableListOf<String>()

        val firstText = firstMatch
            ?.groupValues
            ?.getOrNull(2)
            ?.trim()
            ?: lines.first()

        if (firstText.isNotBlank()) {
            questionParts += firstText
        }

        for (line in lines.drop(1)) {
            val optionMatch = optionPattern.find(line)

            if (optionMatch != null) {
                options += optionMatch.groupValues[2].trim()
            } else if (options.isEmpty()) {
                questionParts += line
            } else {
                val lastIndex = options.lastIndex
                options[lastIndex] = "${options[lastIndex]} $line".trim()
            }
        }

        if (options.size != 4) return null

        return ParsedQuestion(
            number = number,
            text = questionParts.joinToString(" ").trim(),
            options = options
        )
    }

    private fun parseAsSingleQuestion(
        lines: List<String>
    ): List<ParsedQuestion> {
        val optionLines = lines.mapIndexedNotNull { index, line ->
            val match = optionPattern.find(line)
            if (match != null) {
                index to match.groupValues[2].trim()
            } else {
                null
            }
        }

        if (optionLines.size != 4) return emptyList()

        val firstOptionIndex = optionLines.first().first
        val questionText = lines
            .take(firstOptionIndex)
            .joinToString(" ")
            .replace(questionStartPattern, "$2")
            .trim()

        if (questionText.isBlank()) return emptyList()

        return listOf(
            ParsedQuestion(
                number = 1,
                text = questionText,
                options = optionLines.map { it.second }
            )
        )
    }

    private fun normalizeLine(value: String): String {
        return value
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun convertPersianDigits(value: String): String {
        val persian = "۰۱۲۳۴۵۶۷۸۹"
        val english = "0123456789"

        return value.map { character ->
            val index = persian.indexOf(character)
            if (index >= 0) english[index] else character
        }.joinToString("")
    }
}