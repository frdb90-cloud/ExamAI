package com.hoosha.examai.engine

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class ExtractedDocument(
    val displayName: String,
    val mimeType: String,
    val text: String
)

class DocumentExtractor(
    private val context: Context
) {
    suspend fun extract(uri: Uri): ExtractedDocument {
        return withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val displayName = findDisplayName(uri)
            val mimeType = resolver.getType(uri) ?: inferMimeType(displayName)

            val text = when {
                mimeType.equals("application/pdf", ignoreCase = true) ||
                    displayName.endsWith(".pdf", ignoreCase = true) -> {
                    extractPdf(uri)
                }

                else -> {
                    extractPlainText(uri)
                }
            }

            require(text.isNotBlank()) {
                "\u0645\u062A\u0646\u06CC \u0627\u0632 \u0641\u0627\u06CC\u0644 \u0627\u0633\u062A\u062E\u0631\u0627\u062C \u0646\u0634\u062F."
            }

            ExtractedDocument(
                displayName = displayName,
                mimeType = mimeType,
                text = text.trim()
            )
        }
    }

    fun splitIntoChunks(
        text: String,
        maximumLength: Int = 1200,
        overlap: Int = 200
    ): List<String> {
        val cleaned = text
            .replace("\r\n", "\n")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()

        if (cleaned.isBlank()) return emptyList()
        if (cleaned.length <= maximumLength) return listOf(cleaned)

        val chunks = mutableListOf<String>()
        var start = 0

        while (start < cleaned.length) {
            var end = minOf(start + maximumLength, cleaned.length)

            if (end < cleaned.length) {
                val paragraphBreak = cleaned.lastIndexOf(
                    "\n",
                    startIndex = end
                )

                val sentenceBreak = findLastSentenceBreak(
                    text = cleaned,
                    startIndex = start,
                    endIndex = end
                )

                val preferredEnd = maxOf(
                    paragraphBreak,
                    sentenceBreak
                )

                if (preferredEnd > start + maximumLength / 2) {
                    end = preferredEnd + 1
                }
            }

            val chunk = cleaned.substring(start, end).trim()

            if (chunk.isNotBlank()) {
                chunks += chunk
            }

            if (end >= cleaned.length) break

            start = maxOf(
                end - overlap,
                start + 1
            )
        }

        return chunks
    }

    private fun findLastSentenceBreak(
        text: String,
        startIndex: Int,
        endIndex: Int
    ): Int {
        val sentenceEndings = setOf(
            '.',
            '?',
            '!',
            '\u061F',
            '\u061B'
        )

        val safeEndIndex = minOf(
            endIndex,
            text.lastIndex
        )

        for (index in safeEndIndex downTo startIndex) {
            if (text[index] in sentenceEndings) {
                return index
            }
        }

        return -1
    }

    private fun extractPlainText(uri: Uri): String {
        return context.contentResolver
            .openInputStream(uri)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { reader ->
                reader.readText()
            }
            ?: error(
                "\u0641\u0627\u06CC\u0644 \u0642\u0627\u0628\u0644 \u062E\u0648\u0627\u0646\u062F\u0646 \u0646\u06CC\u0633\u062A."
            )
    }

    private fun extractPdf(uri: Uri): String {
        val bytes = context.contentResolver
            .openInputStream(uri)
            ?.use { input ->
                val output = ByteArrayOutputStream()
                input.copyTo(output)
                output.toByteArray()
            }
            ?: error(
                "\u0641\u0627\u06CC\u0644 PDF \u0642\u0627\u0628\u0644 \u062E\u0648\u0627\u0646\u062F\u0646 \u0646\u06CC\u0633\u062A."
            )

        return PDDocument.load(bytes).use { document ->
            PDFTextStripper().getText(document)
        }
    }

    private fun findDisplayName(uri: Uri): String {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )

                if (index >= 0) {
                    return cursor.getString(index)
                }
            }
        }

        return uri.lastPathSegment
            ?: "\u0645\u0646\u0628\u0639 \u062F\u0631\u0633\u06CC"
    }

    private fun inferMimeType(displayName: String): String {
        return when {
            displayName.endsWith(".pdf", ignoreCase = true) -> {
                "application/pdf"
            }

            displayName.endsWith(".txt", ignoreCase = true) -> {
                "text/plain"
            }

            else -> {
                "text/plain"
            }
        }
    }
}
