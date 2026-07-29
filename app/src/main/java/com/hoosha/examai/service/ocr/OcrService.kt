package com.hoosha.examai.service.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OcrResult(
    val text: String,
    val blocks: List<OcrBlock>
)

data class OcrBlock(
    val text: String,
    val left: Int?,
    val top: Int?,
    val right: Int?,
    val bottom: Int?
)

interface OcrService {
    suspend fun recognize(uri: Uri): OcrResult
    suspend fun recognize(bitmap: Bitmap): OcrResult
}

@Singleton
class MlKitOcrService @Inject constructor(
    @ApplicationContext private val context: Context
) : OcrService {

    private val recognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    override suspend fun recognize(uri: Uri): OcrResult {
        val image = InputImage.fromFilePath(context, uri)
        return process(image)
    }

    override suspend fun recognize(bitmap: Bitmap): OcrResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        return process(image)
    }

    private suspend fun process(image: InputImage): OcrResult {
        val recognized = recognizer.process(image).awaitResult()

        return OcrResult(
            text = recognized.text.trim(),
            blocks = recognized.textBlocks.map { block ->
                val box = block.boundingBox
                OcrBlock(
                    text = block.text,
                    left = box?.left,
                    top = box?.top,
                    right = box?.right,
                    bottom = box?.bottom
                )
            }
        )
    }
}

private suspend fun <T> Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        addOnFailureListener { error ->
            if (continuation.isActive) {
                continuation.resumeWithException(error)
            }
        }
        addOnCanceledListener {
            continuation.cancel()
        }
    }