package com.hoosha.examai.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

class OfflineOcrEngine {

    suspend fun recognize(
        context: Context,
        uri: Uri
    ): String {
        return withContext(Dispatchers.IO) {
            val sourceBitmap = decodeBitmap(context, uri)
            val preparedBitmap = prepareForOcr(sourceBitmap)

            try {
                recognizeInHorizontalSections(
                    context = context,
                    bitmap = preparedBitmap
                )
            } finally {
                if (preparedBitmap !== sourceBitmap) {
                    preparedBitmap.recycle()
                }

                if (!sourceBitmap.isRecycled) {
                    sourceBitmap.recycle()
                }
            }
        }
    }

    private fun recognizeInHorizontalSections(
        context: Context,
        bitmap: Bitmap
    ): String {
        val dataDirectory = prepareLanguageData(context)
        val tessBaseApi = TessBaseAPI()

        try {
            val initialized = tessBaseApi.init(
                dataDirectory.absolutePath,
                LANGUAGE
            )

            check(initialized) {
                "\u0645\u062F\u0644 OCR \u0641\u0627\u0631\u0633\u06CC \u0628\u0627\u0631\u06AF\u0630\u0627\u0631\u06CC \u0646\u0634\u062F."
            }

            tessBaseApi.pageSegMode =
                TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK

            tessBaseApi.setVariable(
                "preserve_interword_spaces",
                "1"
            )

            tessBaseApi.setVariable(
                "user_defined_dpi",
                "300"
            )

            val sectionHeight = (
                bitmap.height.toFloat() / SECTION_COUNT
            ).roundToInt().coerceAtLeast(1)

            val overlapHeight = (
                sectionHeight.toFloat() * SECTION_OVERLAP
            ).roundToInt()

            val recognizedSections = mutableListOf<String>()

            for (sectionIndex in 0 until SECTION_COUNT) {
                val nominalTop = sectionIndex * sectionHeight

                val cropTop = if (sectionIndex == 0) {
                    0
                } else {
                    (nominalTop - overlapHeight)
                        .coerceAtLeast(0)
                }

                val nominalBottom = if (
                    sectionIndex == SECTION_COUNT - 1
                ) {
                    bitmap.height
                } else {
                    (sectionIndex + 1) * sectionHeight
                }

                val cropBottom = if (
                    sectionIndex == SECTION_COUNT - 1
                ) {
                    bitmap.height
                } else {
                    (nominalBottom + overlapHeight)
                        .coerceAtMost(bitmap.height)
                }

                val cropHeight = cropBottom - cropTop

                if (cropHeight <= 0) {
                    continue
                }

                val sectionBitmap = Bitmap.createBitmap(
                    bitmap,
                    0,
                    cropTop,
                    bitmap.width,
                    cropHeight
                )

                try {
                    tessBaseApi.setImage(sectionBitmap)

                    val sectionText = normalizeRecognizedText(
                        tessBaseApi.utF8Text.orEmpty()
                    )

                    if (sectionText.isNotBlank()) {
                        recognizedSections += sectionText
                    }

                    tessBaseApi.clear()
                } finally {
                    sectionBitmap.recycle()
                }
            }

            val result = removeDuplicateLines(
                recognizedSections.joinToString("\n")
            )

            check(result.isNotBlank()) {
                "\u0645\u062A\u0646\u06CC \u062F\u0631 \u062A\u0635\u0648\u06CC\u0631 \u0634\u0646\u0627\u0633\u0627\u06CC\u06CC \u0646\u0634\u062F."
            }

            return result
        } finally {
            tessBaseApi.recycle()
        }
    }

    private fun normalizeRecognizedText(
        value: String
    ): String {
        return value
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace('\u064A', '\u06CC')
            .replace('\u0643', '\u06A9')
            .replace('\u06C0', '\u0647')
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

    private fun removeDuplicateLines(
        value: String
    ): String {
        val acceptedLines = mutableListOf<String>()
        val normalizedLines = mutableListOf<String>()

        value.lines().forEach { originalLine ->
            val cleanLine = originalLine
                .replace(Regex("\\s+"), " ")
                .trim()

            if (cleanLine.isBlank()) {
                return@forEach
            }

            val comparisonLine = normalizeForComparison(
                cleanLine
            )

            val duplicate = normalizedLines
                .takeLast(DUPLICATE_LOOKBACK)
                .any { previousLine ->
                    previousLine == comparisonLine ||
                        similarity(previousLine, comparisonLine) >=
                        DUPLICATE_SIMILARITY
                }

            if (!duplicate) {
                acceptedLines += cleanLine
                normalizedLines += comparisonLine
            }
        }

        return acceptedLines.joinToString("\n")
    }

    private fun normalizeForComparison(
        value: String
    ): String {
        return value
            .lowercase()
            .replace('\u064A', '\u06CC')
            .replace('\u0643', '\u06A9')
            .replace(Regex("[^\\p{L}\\p{N}]"), "")
            .trim()
    }

    private fun similarity(
        first: String,
        second: String
    ): Double {
        if (first.isBlank() || second.isBlank()) {
            return 0.0
        }

        if (first == second) {
            return 1.0
        }

        val shorter = if (
            first.length <= second.length
        ) {
            first
        } else {
            second
        }

        val longer = if (
            first.length > second.length
        ) {
            first
        } else {
            second
        }

        if (shorter.length < MINIMUM_DUPLICATE_LENGTH) {
            return 0.0
        }

        if (longer.contains(shorter)) {
            return shorter.length.toDouble() /
                longer.length.toDouble()
        }

        val commonCharacters = shorter.count { character ->
            character in longer
        }

        return commonCharacters.toDouble() /
            longer.length.toDouble()
    }

    private fun prepareLanguageData(
        context: Context
    ): File {
        val dataDirectory = File(
            context.filesDir,
            TESSERACT_DIRECTORY
        )

        val tessDataDirectory = File(
            dataDirectory,
            TESSDATA_DIRECTORY
        )

        if (!tessDataDirectory.exists()) {
            check(tessDataDirectory.mkdirs()) {
                "\u067E\u0648\u0634\u0647 OCR \u0627\u06CC\u062C\u0627\u062F \u0646\u0634\u062F."
            }
        }

        val destinationFile = File(
            tessDataDirectory,
            TRAINED_DATA_FILE
        )

        val assetLength = context.assets
            .open(ASSET_PATH)
            .use { input ->
                var totalLength = 0L
                val buffer = ByteArray(COPY_BUFFER_SIZE)

                while (true) {
                    val count = input.read(buffer)

                    if (count < 0) {
                        break
                    }

                    totalLength += count
                }

                totalLength
            }

        val mustCopy = !destinationFile.exists() ||
            destinationFile.length() != assetLength

        if (mustCopy) {
            val temporaryFile = File(
                tessDataDirectory,
                "$TRAINED_DATA_FILE.tmp"
            )

            context.assets.open(ASSET_PATH).use { input ->
                FileOutputStream(temporaryFile).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }

            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            check(temporaryFile.renameTo(destinationFile)) {
                temporaryFile.delete()

                "\u06A9\u067E\u06CC \u0645\u062F\u0644 OCR \u0641\u0627\u0631\u0633\u06CC \u0646\u0627\u0645\u0648\u0641\u0642 \u0628\u0648\u062F."
            }
        }

        check(
            destinationFile.exists() &&
                destinationFile.length() > 0L
        ) {
            "\u0641\u0627\u06CC\u0644 fas.traineddata \u067E\u06CC\u062F\u0627 \u0646\u0634\u062F."
        }

        return dataDirectory
    }

    private fun decodeBitmap(
        context: Context,
        uri: Uri
    ): Bitmap {
        val imageBytes = context.contentResolver
            .openInputStream(uri)
            ?.use { input ->
                input.readBytes()
            }
            ?: error(
                "\u062A\u0635\u0648\u06CC\u0631 \u0642\u0627\u0628\u0644 \u062E\u0648\u0627\u0646\u062F\u0646 \u0646\u06CC\u0633\u062A."
            )

        val boundsOptions =
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

        BitmapFactory.decodeByteArray(
            imageBytes,
            0,
            imageBytes.size,
            boundsOptions
        )

        check(
            boundsOptions.outWidth > 0 &&
                boundsOptions.outHeight > 0
        ) {
            "\u0641\u0631\u0645\u062A \u062A\u0635\u0648\u06CC\u0631 \u067E\u0634\u062A\u06CC\u0628\u0627\u0646\u06CC \u0646\u0645\u06CC\u200C\u0634\u0648\u062F."
        }

        val sampleSize = calculateSampleSize(
            width = boundsOptions.outWidth,
            height = boundsOptions.outHeight
        )

        val decodeOptions =
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig =
                    Bitmap.Config.ARGB_8888
            }

        return BitmapFactory.decodeByteArray(
            imageBytes,
            0,
            imageBytes.size,
            decodeOptions
        ) ?: error(
            "\u062A\u0628\u062F\u06CC\u0644 \u062A\u0635\u0648\u06CC\u0631 \u0646\u0627\u0645\u0648\u0641\u0642 \u0628\u0648\u062F."
        )
    }

    private fun calculateSampleSize(
        width: Int,
        height: Int
    ): Int {
        var sampleSize = 1
        var currentWidth = width
        var currentHeight = height

        while (
            currentWidth > MAX_IMAGE_DIMENSION ||
            currentHeight > MAX_IMAGE_DIMENSION
        ) {
            sampleSize *= 2
            currentWidth /= 2
            currentHeight /= 2
        }

        return sampleSize
    }

    private fun prepareForOcr(
        source: Bitmap
    ): Bitmap {
        val targetWidth = source.width
            .coerceIn(
                MINIMUM_OCR_WIDTH,
                MAXIMUM_OCR_WIDTH
            )

        val scale = targetWidth.toFloat() /
            source.width.toFloat()

        val targetHeight = (
            source.height.toFloat() * scale
        ).roundToInt().coerceAtLeast(1)

        val scaledBitmap = if (
            targetWidth != source.width ||
            targetHeight != source.height
        ) {
            Bitmap.createScaledBitmap(
                source,
                targetWidth,
                targetHeight,
                true
            )
        } else {
            source
        }

        val preparedBitmap = Bitmap.createBitmap(
            scaledBitmap.width,
            scaledBitmap.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(preparedBitmap)
        canvas.drawColor(Color.WHITE)

        val colorMatrix = ColorMatrix().apply {
            setSaturation(0f)

            postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1.45f, 0f, 0f, 0f, -38f,
                        0f, 1.45f, 0f, 0f, -38f,
                        0f, 0f, 1.45f, 0f, -38f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        val paint = Paint(
            Paint.ANTI_ALIAS_FLAG or
                Paint.FILTER_BITMAP_FLAG
        ).apply {
            colorFilter =
                ColorMatrixColorFilter(colorMatrix)
        }

        canvas.drawBitmap(
            scaledBitmap,
            0f,
            0f,
            paint
        )

        if (scaledBitmap !== source) {
            scaledBitmap.recycle()
        }

        return preparedBitmap
    }

    private companion object {
        const val LANGUAGE = "fas"
        const val TESSERACT_DIRECTORY = "tesseract"
        const val TESSDATA_DIRECTORY = "tessdata"
        const val TRAINED_DATA_FILE =
            "fas.traineddata"
        const val ASSET_PATH =
            "tessdata/fas.traineddata"

        const val SECTION_COUNT = 6
        const val SECTION_OVERLAP = 0.20f

        const val DUPLICATE_LOOKBACK = 12
        const val DUPLICATE_SIMILARITY = 0.92
        const val MINIMUM_DUPLICATE_LENGTH = 12

        const val MINIMUM_OCR_WIDTH = 2000
        const val MAXIMUM_OCR_WIDTH = 2800
        const val MAX_IMAGE_DIMENSION = 6000
        const val COPY_BUFFER_SIZE = 8192
    }
}
