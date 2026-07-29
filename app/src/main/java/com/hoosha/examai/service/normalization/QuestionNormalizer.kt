package com.hoosha.examai.domain.parser

data class ParsedOption(
 val key: String,
 val displayLabel: String,
 val text: String
)

data class ParsedQuestion(
 val number: Int,
 val text: String,
 val options: List<ParsedOption>,
 val reviewRequired: Boolean
)

object QuestionNormalizer {

 private val persianDigits = "۰۱۲۳۴۵۶۷۸۹"
 private val arabicDigits = "٠١٢٣٤٥٦٧٨٩"
 private val latinDigits = "0123456789"

 private val questionPattern = Regex(
 pattern = """(?im)^\s*(?:س[ؤو]ال\s*|question\s*|q\s*)?([0-9۰-۹٠-٩]+)\s*[\)\.\-:،]?\s*(.*)$"""
 )

 private val optionPattern = Regex(
 pattern = """(?im)^\s*[\(\[（]?\s*(الف|ا|ب|ج|د|a|b|c|d|A|B|C|D|[1-4۱-۴١-٤])\s*[\)\]\.:\-]?\s+(.+)$"""
 )

 fun normalizeDigits(value: String): String = buildString(value.length) {
 value.forEach { character ->
 val persianIndex = persianDigits.indexOf(character)
 val arabicIndex = arabicDigits.indexOf(character)

 append(
 when {
 persianIndex >= 0 -> latinDigits[persianIndex]
 arabicIndex >= 0 -> latinDigits[arabicIndex]
 else -> character
 }
 )
 }
 }

 fun normalizeOptionKey(raw: String): String? {
 val value = normalizeDigits(raw.trim())
.replace("(", "")
.replace(")", "")
.replace(".", "")
.trim()

 return when (value.lowercase()) {
 "الف", "ا", "a", "1" -> "A"
 "ب", "b", "2" -> "B"
 "ج", "c", "3" -> "C"
 "د", "d", "4" -> "D"
 else -> null
 }
 }

 fun displayLabel(key: String): String = when (key.uppercase()) {
 "A" -> "الف"
 "B" -> "ب"
 "C" -> "ج"
 "D" -> "د"
 else -> key
 }

 fun parse(ocrText: String): List<ParsedQuestion> {
 val lines = ocrText
.replace("\r\n", "\n")
.replace('\r', '\n')
.lines()
.map { it.trim() }
.filter { it.isNotBlank() }

 val questions = mutableListOf<ParsedQuestion>()
 var currentNumber: Int? = null
 val questionText = mutableListOf<String>()
 val options = mutableListOf<ParsedOption>()
 var activeOptionIndex: Int? = null

 fun flush() {
 val number = currentNumber?: return
 val normalizedOptions = options
.distinctBy { it.key }
.sortedBy { it.key }

 questions += ParsedQuestion(
 number = number,
 text = questionText.joinToString(" ").trim(),
 options = normalizedOptions,
 reviewRequired = normalizedOptions.size!= 4 ||
 questionText.joinToString("").isBlank()
 )

 questionText.clear()
 options.clear()
 activeOptionIndex = null
 }

 for (line in lines) {
 val optionMatch = optionPattern.matchEntire(line)
 if (optionMatch!= null && currentNumber!= null) {
 val key = normalizeOptionKey(optionMatch.groupValues[1])
 val text = optionMatch.groupValues[2].trim()

 if (key!= null && text.isNotBlank()) {
 options += ParsedOption(
 key = key,
 displayLabel = displayLabel(key),
 text = text
 )
 activeOptionIndex = options.lastIndex
 continue
 }
 }

 val questionMatch = questionPattern.matchEntire(line)
 val possibleNumber = questionMatch
?.groupValues
?.getOrNull(1)
?.let(::normalizeDigits)
?.toIntOrNull()

 val looksLikeQuestion = possibleNumber!= null &&
 (
 currentNumber == null ||
 options.isNotEmpty() ||
 line.contains("سوال") ||
 line.contains("سؤال") ||
 line.lowercase().startsWith("q")
 )

 if (looksLikeQuestion) {
 flush()
 currentNumber = possibleNumber
 questionText += questionMatch?.groupValues?.getOrNull(2).orEmpty()
 continue
 }

 val optionIndex = activeOptionIndex
 if (optionIndex!= null && optionIndex in options.indices) {
 val previous = options[optionIndex]
 options[optionIndex] = previous.copy(
 text = "${previous.text} $line".trim()
 )
 } else if (currentNumber!= null) {
 questionText += line
 }
 }

 flush()
 return questions
.distinctBy { it.number }
.sortedBy { it.number }
 }
}