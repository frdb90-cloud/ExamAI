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
                "ظ…طھظ†غŒ ط§ط² ظپط§غŒظ„ ط§ط³طھط®ط±ط§ط¬ ظ†ط´ط¯."
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
                val paragraphBreak = cleaned.lastIndexOf("\n", end)
                val sentenceBreak = cleaned.lastIndexOfAny(
                    charArrayOf('.', 'طں', '!', 'ط›'),
                    end
                )

                val preferredEnd = maxOf(paragraphBreak, sentenceBreak)
                if (preferredEnd > start + maximumLength / 2) {
                    end = preferredEnd + 1
                }
            }

            val chunk = cleaned.substring(start, end).trim()
            if (chunk.isNotBlank()) {
                chunks += chunk
            }

            if (end >= cleaned.length) break
            start = maxOf(end - overlap, start + 1)
        }

        return chunks
    }

    private fun extractPlainText(uri: Uri): String {
        return context.contentResolver
            .openInputStream(uri)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            ?: error("ظپط§غŒظ„ ظ‚ط§ط¨ظ„ ط®ظˆط§ظ†ط¯ظ† ظ†غŒط³طھ.")
    }

    private fun extractPdf(uri: Uri): String {
        val bytes = context.contentResolver
            .openInputStream(uri)
            ?.use { input ->
                val output = ByteArrayOutputStream()
                input.copyTo(output)
                output.toByteArray()
            }
            ?: error("ظپط§غŒظ„ PDF ظ‚ط§ط¨ظ„ ط®ظˆط§ظ†ط¯ظ† ظ†غŒط³طھ.")

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

        return uri.lastPathSegment ?: "ظ…ظ†ط¨ط¹ ط¯ط±ط³غŒ"
    }

    private fun inferMimeType(displayName: String): String {
        return when {
            displayName.endsWith(".pdf", ignoreCase = true) ->
                "application/pdf"

            displayName.endsWith(".txt", ignoreCase = true) ->
                "text/plain"

            else -> "text/plain"
        }
    }
}
