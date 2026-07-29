package com.hoosha.examai.service.document

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.hoosha.examai.service.ocr.OcrService
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.parsers.DocumentBuilderFactory

data class ExtractedPage(
    val pageNumber: Int?,
    val section: String?,
    val text: String
)

data class ExtractedDocument(
    val pageCount: Int?,
    val pages: List<ExtractedPage>
)

@Singleton
class DocumentExtractor @Inject constructor(
    private val ocrService: OcrService
) {
    suspend fun extract(
        file: File,
        mimeType: String
    ): ExtractedDocument {
        require(file.exists() && file.isFile) {
            "فایل منبع پیدا نشد."
        }

        return when {
            mimeType == "application/pdf" ||
                file.extension.equals("pdf", true) -> extractPdf(file)

            mimeType.startsWith("image/") -> extractImage(file)

            mimeType == "text/plain" ||
                file.extension.equals("txt", true) -> extractTxt(file)

            mimeType ==
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
                file.extension.equals("docx", true) -> extractDocx(file)

            else -> error("فرمت فایل پشتیبانی نمی‌شود.")
        }
    }

    private suspend fun extractPdf(file: File): ExtractedDocument {
        val textResult = withContext(Dispatchers.IO) {
            PDDocument.load(file).use { document ->
                val stripper = PDFTextStripper()
                val pages = (1..document.numberOfPages).map { pageNumber ->
                    stripper.startPage = pageNumber
                    stripper.endPage = pageNumber
                    ExtractedPage(
                        pageNumber = pageNumber,
                        section = null,
                        text = stripper.getText(document).trim()
                    )
                }
                ExtractedDocument(document.numberOfPages, pages)
            }
        }

        val meaningfulCharacters = textResult.pages.sumOf {
            it.text.count(Char::isLetterOrDigit)
        }

        return if (meaningfulCharacters >= 40) {
            textResult
        } else {
            extractScannedPdf(file)
        }
    }

    private suspend fun extractScannedPdf(
        file: File
    ): ExtractedDocument = withContext(Dispatchers.IO) {
        val descriptor = ParcelFileDescriptor.open(
            file,
            ParcelFileDescriptor.MODE_READ_ONLY
        )

        PdfRenderer(descriptor).use { renderer ->
            val pages = mutableListOf<ExtractedPage>()

            for (index in 0 until renderer.pageCount) {
                renderer.openPage(index).use { page ->
                    val scale = 2
                    val bitmap = Bitmap.createBitmap(
                        page.width * scale,
                        page.height * scale,
                        Bitmap.Config.ARGB_8888
                    )

                    try {
                        page.render(
                            bitmap,
                            null,
                            null,
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        )

                        val result = ocrService.recognize(bitmap)
                        pages += ExtractedPage(
                            pageNumber = index + 1,
                            section = null,
                            text = result.text
                        )
                    } finally {
                        bitmap.recycle()
                    }
                }
            }

            ExtractedDocument(
                pageCount = renderer.pageCount,
                pages = pages
            )
        }
    }

    private suspend fun extractImage(file: File): ExtractedDocument {
        val result = ocrService.recognize(
            android.net.Uri.fromFile(file)
        )
        return ExtractedDocument(
            pageCount = 1,
            pages = listOf(
                ExtractedPage(
                    pageNumber = 1,
                    section = null,
                    text = result.text
                )
            )
        )
    }

    private suspend fun extractTxt(
        file: File
    ): ExtractedDocument = withContext(Dispatchers.IO) {
        val bytes = file.readBytes()
        val text = runCatching {
            bytes.toString(Charsets.UTF_8)
        }.getOrElse {
            bytes.toString(Charsets.ISO_8859_1)
        }

        ExtractedDocument(
            pageCount = null,
            pages = listOf(
                ExtractedPage(
                    pageNumber = null,
                    section = null,
                    text = text.trim()
                )
            )
        )
    }

    private suspend fun extractDocx(
        file: File
    ): ExtractedDocument = withContext(Dispatchers.IO) {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry("word/document.xml")
                ?: error("ساختار فایل Word معتبر نیست.")

            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true
                )
                setFeature(
                    "http://xml.org/sax/features/external-general-entities",
                    false
                )
                setFeature(
                    "http://xml.org/sax/features/external-parameter-entities",
                    false
                )
            }

            val document = zip.getInputStream(entry).use {
                factory.newDocumentBuilder().parse(it)
            }

            val paragraphs = document.getElementsByTagNameNS(
                "http://schemas.openxmlformats.org/wordprocessingml/2006/main",
                "p"
            )

            val text = buildString {
                for (index in 0 until paragraphs.length) {
                    val paragraph = paragraphs.item(index) as? Element
                        ?: continue
                    val nodes = paragraph.getElementsByTagNameNS(
                        "http://schemas.openxmlformats.org/wordprocessingml/2006/main",
                        "t"
                    )

                    val line = buildString {
                        for (nodeIndex in 0 until nodes.length) {
                            append(nodes.item(nodeIndex).textContent)
                        }
                    }.trim()

                    if (line.isNotBlank()) appendLine(line)
                }
            }.trim()

            ExtractedDocument(
                pageCount = null,
                pages = listOf(
                    ExtractedPage(
                        pageNumber = null,
                        section = null,
                        text = text
                    )
                )
            )
        }
    }
}