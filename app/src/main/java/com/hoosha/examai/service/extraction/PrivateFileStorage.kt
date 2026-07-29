package com.hoosha.examai.service.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class StoredFile(
    val originalUri: String,
    val displayName: String,
    val mimeType: String,
    val path: String,
    val sizeBytes: Long
)

@Singleton
class PrivateFileStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun copySource(uri: Uri): StoredFile =
        copyInto(uri, File(context.filesDir, "study_sources"))

    suspend fun copyExamImage(uri: Uri): StoredFile =
        copyInto(uri, File(context.filesDir, "exam_images"))

    suspend fun delete(path: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(path)
        val allowedRoot = context.filesDir.canonicalFile
        val target = file.canonicalFile

        require(target.path.startsWith(allowedRoot.path)) {
            "حذف فایل خارج از فضای خصوصی برنامه مجاز نیست."
        }

        !target.exists() || target.delete()
    }

    suspend fun readText(path: String): String = withContext(Dispatchers.IO) {
        validatePrivatePath(path).readText(Charsets.UTF_8)
    }

    private suspend fun copyInto(
        uri: Uri,
        directory: File
    ): StoredFile = withContext(Dispatchers.IO) {
        directory.mkdirs()

        val metadata = queryMetadata(uri)
        val extension = metadata.first.substringAfterLast(
            delimiter = ".",
            missingDelimiterValue = ""
        ).lowercase().take(10)

        val targetName = buildString {
            append(UUID.randomUUID())
            if (extension.isNotBlank()) {
                append(".")
                append(extension)
            }
        }

        val target = File(directory, targetName)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().buffered().use { output ->
                    input.copyTo(output)
                }
            } ?: error("باز کردن فایل انتخاب‌شده ممکن نبود.")
        } catch (error: Throwable) {
            target.delete()
            throw error
        }

        StoredFile(
            originalUri = uri.toString(),
            displayName = metadata.first,
            mimeType = context.contentResolver.getType(uri)
                ?: "application/octet-stream",
            path = target.absolutePath,
            sizeBytes = target.length()
        )
    }

    private fun queryMetadata(uri: Uri): Pair<String, Long?> {
        var name = uri.lastPathSegment ?: "file"
        var size: Long? = null

        context.contentResolver.query(
            uri,
            arrayOf(
                OpenableColumns.DISPLAY_NAME,
                OpenableColumns.SIZE
            ),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )
                val sizeIndex = cursor.getColumnIndex(
                    OpenableColumns.SIZE
                )

                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                    name = cursor.getString(nameIndex)
                }
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }

        return name to size
    }

    private fun validatePrivatePath(path: String): File {
        val root = context.filesDir.canonicalFile
        val target = File(path).canonicalFile
        require(target.path.startsWith(root.path)) {
            "مسیر فایل معتبر نیست."
        }
        return target
    }
}