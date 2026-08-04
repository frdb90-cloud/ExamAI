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

class OfflineOcrEngine {

    suspend fun recognize(
        context: Context,
        uri: Uri
    ): String {
        return withContext(Dispatchers.IO) {
            val sourceBitmap = decodeBitmap(context, uri)

            try {
                val preparedBitmap = prepareForOcr(sourceBitmap)

                try {
                    recognizeBitmap(context, preparedBitmap)
                } finally {
                    if (preparedBitmap !== sourceBitmap) {
                        preparedBitmap.recycle()
                    }
                }
            } finally {
                sourceBitmap.recycle()
            }
        }
    }

    private fun recognizeBitmap(
        context: Context,
        bitmap: Bitmap
    ): String {
        val dataDirectory = prepareLanguageData(context)
        val tessBaseApi = TessBaseAPI()

        return try {
            val initialized = tessBaseApi.init(
                dataDirectory.absolutePath,
                LANGUAGE
            )

            check(initialized) {
                "\u0645\u062F\u0644 OCR \u0641\u0627\u0631\u0633\u06CC \u0628\u0627\u0631\u06AF\u0630\u0627\u0631\u06CC \u0646\u0634\u062F."
            }

            tessBaseApi.pageSegMode =
                TessBaseAPI.PageSegMode.PSM_AUTO

            tessBaseApi.setVariable(
                "preserve_interword_spaces",
                "1"
            )

            tessBaseApi.setVariable(
                "user_defined_dpi",
                "300"
            )

            tessBaseApi.setImage(bitmap)

            val recognizedText = tessBaseApi.utF8Text
                ?.replace('\u064A', '\u06CC')
                ?.replace('\u0643', '\u06A9')
                ?.replace("\r\n", "\n")
                ?.replace(Regex("[ \\t]+"), " ")
                ?.replace(Regex("\\n{3,}"), "\n\n")
                ?.trim()
                .orEmpty()

            check(recognizedText.isNotBlank()) {
                "\u0645\u062A\u0646\u06CC \u062F\u0631 \u062A\u0635\u0648\u06CC\u0631 \u0634\u0646\u0627\u0633\u0627\u06CC\u06CC \u0646\u0634\u062F."
            }

            recognizedText
        } finally {
            tessBaseApi.recycle()
        }
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

        val destination = File(
            tessDataDirectory,
            TRAINED_DATA_FILE
        )

        val assetSize = runCatching {
            context.assets
                .open(ASSET_PATH)
                .use { it.available().toLong() }
        }.getOrDefault(-1L)

        val mustCopy = !destination.exists() ||
            destination.length() == 0L ||
            (assetSize > 0L && destination.length() != assetSize)

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

            if (destination.exists()) {
                destination.delete()
            }

            check(temporaryFile.renameTo(destination)) {
                temporaryFile.delete()
                "\u06A9\u067E\u06CC \u0645\u062F\u0644 OCR \u0641\u0627\u0631\u0633\u06CC \u0646\u0627\u0645\u0648\u0641\u0642 \u0628\u0648\u062F."
            }
        }

        check(destination.exists() && destination.length() > 0L) {
            "\u0641\u0627\u06CC\u0644 fas.traineddata \u067E\u06CC\u062F\u0627 \u0646\u0634\u062F."
        }

        return dataDirectory
    }

    private fun decodeBitmap(
        context: Context,
        uri: Uri
    ): Bitmap {
        val bytes = context.contentResolver
            .openInputStream(uri)
            ?.use { it.readBytes() }
            ?: error(
                "\u062A\u0635\u0648\u06CC\u0631 \u0642\u0627\u0628\u0644 \u062E\u0648\u0627\u0646\u062F\u0646 \u0646\u06CC\u0633\u062A."
            )

        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
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
            bytes,
            0,
            bytes.size,
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
        val targetWidth = when {
            source.width < MINIMUM_OCR_WIDTH -> {
                MINIMUM_OCR_WIDTH
            }

            source.width > MAXIMUM_OCR_WIDTH -> {
                MAXIMUM_OCR_WIDTH
            }

            else -> {
                source.width
            }
        }

        val scale = targetWidth.toFloat() /
            source.width.toFloat()

        val targetHeight = (
            source.height.toFloat() * scale
        ).toInt().coerceAtLeast(1)

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

        val grayscaleBitmap = Bitmap.createBitmap(
            scaledBitmap.width,
            scaledBitmap.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(grayscaleBitmap)
        canvas.drawColor(Color.WHITE)

        val colorMatrix = ColorMatrix().apply {
            setSaturation(0f)

            postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1.35f, 0f, 0f, 0f, -28f,
                        0f, 1.35f, 0f, 0f, -28f,
                        0f, 0f, 1.35f, 0f, -28f,
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

        return grayscaleBitmap
    }

    private companion object {
        const val LANGUAGE = "fas"
        const val TESSERACT_DIRECTORY = "tesseract"
        const val TESSDATA_DIRECTORY = "tessdata"
        const val TRAINED_DATA_FILE = "fas.traineddata"
        const val ASSET_PATH = "tessdata/fas.traineddata"

        const val MINIMUM_OCR_WIDTH = 1800
        const val MAXIMUM_OCR_WIDTH = 2600
        const val MAX_IMAGE_DIMENSION = 5000
    }
}