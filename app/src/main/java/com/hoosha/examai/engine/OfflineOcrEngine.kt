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
    ): String = withContext(Dispatchers.IO) {
        val sourceBitmap = decodeBitmap(context, uri)
        val preparedBitmap = prepareForOcr(sourceBitmap)

        try {
            recognizeWithOverlappingWindows(
                context = context,
                bitmap = preparedBitmap
            )
        } finally {
            if (
                preparedBitmap !== sourceBitmap &&
                !preparedBitmap.isRecycled
            ) {
                preparedBitmap.recycle()
            }

            if (!sourceBitmap.isRecycled) {
                sourceBitmap.recycle()
            }
        }
    }

    private fun recognizeWithOverlappingWindows(
        context: Context,
        bitmap: Bitmap
    ): String {
        val dataDirectory = prepareLanguageData(context)
        val tessBaseApi = TessBaseAPI()

        try {
            check(
                tessBaseApi.init(
                    dataDirectory.absolutePath,
                    LANGUAGE
                )
            ) {
                "\u0645\u062F\u0644 OCR \u0641\u0627\u0631\u0633\u06CC \u0628\u0627\u0631\u06AF\u0630\u0627\u0631\u06CC \u0646\u0634\u062F."
            }

            tessBaseApi.setVariable(
                "preserve_interword_spaces",
                "1"
            )
            tessBaseApi.setVariable(
                "user_defined_dpi",
                "300"
            )

            val outputs = mutableListOf<String>()

            recognizeBitmap(
                tessBaseApi = tessBaseApi,
                bitmap = bitmap,
                pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
            ).takeIf { it.isNotBlank() }?.let(outputs::add)

            val windowHeight = (
                bitmap.height * WINDOW_HEIGHT_RATIO
            ).roundToInt()
                .coerceAtLeast(MINIMUM_WINDOW_HEIGHT)
                .coerceAtMost(bitmap.height)

            val stepHeight = (
                bitmap.height * WINDOW_STEP_RATIO
            ).roundToInt().coerceAtLeast(1)

            var top = 0

            while (top < bitmap.height) {
                val bottom = (top + windowHeight)
                    .coerceAtMost(bitmap.height)

                val height = bottom - top

                if (height >= MINIMUM_VALID_CROP_HEIGHT) {
                    val windowBitmap = Bitmap.createBitmap(
                        bitmap,
                        0,
                        top,
                        bitmap.width,
                        height
                    )

                    try {
                        val windowText = recognizeBitmap(
                            tessBaseApi = tessBaseApi,
                            bitmap = windowBitmap,
                            pageSegMode =
                                TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK
                        )

                        if (windowText.isNotBlank()) {
                            outputs += windowText
                        }
                    } finally {
                        windowBitmap.recycle()
                    }
                }

                if (bottom >= bitmap.height) {
                    break
                }

                top += stepHeight
            }

            val result = outputs
                .joinToString("\n")
                .replace(Regex("\\n{3,}"), "\n\n")
                .trim()

            check(result.isNotBlank()) {
                "\u0645\u062A\u0646\u06CC \u062F\u0631 \u062A\u0635\u0648\u06CC\u0631 \u0634\u0646\u0627\u0633\u0627\u06CC\u06CC \u0646\u0634\u062F."
            }

            return result
        } finally {
            tessBaseApi.recycle()
        }
    }

    private fun recognizeBitmap(
        tessBaseApi: TessBaseAPI,
        bitmap: Bitmap,
        pageSegMode: Int
    ): String {
        tessBaseApi.clear()
        tessBaseApi.pageSegMode = pageSegMode
        tessBaseApi.setImage(bitmap)

        return normalizeRecognizedText(
            tessBaseApi.utF8Text.orEmpty()
        )
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
                var total = 0L
                val buffer = ByteArray(COPY_BUFFER_SIZE)

                while (true) {
                    val count = input.read(buffer)

                    if (count < 0) {
                        break
                    }

                    total += count
                }

                total
            }

        val mustCopy = !destinationFile.exists() ||
            destinationFile.length() != assetLength

        if (mustCopy) {
            val temporaryFile = File(
                tessDataDirectory,
                "$TRAINED_DATA_FILE.tmp"
            )

            if (temporaryFile.exists()) {
                temporaryFile.delete()
            }

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
            ?.use { it.readBytes() }
            ?: error(
                "\u062A\u0635\u0648\u06CC\u0631 \u0642\u0627\u0628\u0644 \u062E\u0648\u0627\u0646\u062F\u0646 \u0646\u06CC\u0633\u062A."
            )

        val boundsOptions = BitmapFactory.Options().apply {
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

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
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
        val targetWidth = source.width.coerceIn(
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
                        1.25f, 0f, 0f, 0f, -20f,
                        0f, 1.25f, 0f, 0f, -20f,
                        0f, 0f, 1.25f, 0f, -20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        val paint = Paint(
            Paint.ANTI_ALIAS_FLAG or
                Paint.FILTER_BITMAP_FLAG
        ).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
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
        const val TRAINED_DATA_FILE = "fas.traineddata"
        const val ASSET_PATH = "tessdata/fas.traineddata"

        const val WINDOW_HEIGHT_RATIO = 0.48f
        const val WINDOW_STEP_RATIO = 0.18f
        const val MINIMUM_WINDOW_HEIGHT = 900
        const val MINIMUM_VALID_CROP_HEIGHT = 500

        const val MINIMUM_OCR_WIDTH = 2200
        const val MAXIMUM_OCR_WIDTH = 3200
        const val MAX_IMAGE_DIMENSION = 7000
        const val COPY_BUFFER_SIZE = 8192
    }
}
