package org.openyaske.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.openyaske.core.media.PdfBuilder
import org.openyaske.core.media.PdfImage
import org.openyaske.core.media.PdfPageSize

class PdfBuilderTest {
    @Test
    fun writesOneObjectPerPageAndKeepsTheJpegBytes() {
        val first = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3, 0xFF.toByte(), 0xD9.toByte())
        val second = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 9, 8, 0xFF.toByte(), 0xD9.toByte())
        val pdf = PdfBuilder.build(
            images = listOf(
                PdfImage(first, width = 1200, height = 1600),
                PdfImage(second, width = 800, height = 600)
            ),
            pageSize = PdfPageSize.A4
        )
        val text = pdf.decodeToString()
        assertTrue(text.startsWith("%PDF-1.4"))
        assertTrue(text.contains("/Count 2"))
        assertTrue(text.contains("/MediaBox [0 0 595 842]"))
        assertTrue(text.contains("/Filter /DCTDecode"))
        assertTrue(text.contains("%%EOF"))
        assertTrue(indexOf(pdf, first) > 0)
        assertTrue(indexOf(pdf, second) > 0)
    }

    @Test
    fun xrefPointsAtTheFirstObject() {
        val pdf = PdfBuilder.build(listOf(PdfImage(byteArrayOf(1, 2, 3), 10, 10)))
        val text = pdf.decodeToString()
        val startXref = text.substringAfter("startxref\n").substringBefore("\n").toInt()
        assertEquals("%PDF-1.4", pdf.decodeToString(0, 8))
        // xref の開始位置に "xref" が来る
        assertTrue(pdf.decodeToString(startXref, startXref + 4) == "xref")
    }

    private fun indexOf(haystack: ByteArray, needle: ByteArray): Int {
        if (needle.isEmpty()) return -1
        outer@ for (i in 0..haystack.size - needle.size) {
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) continue@outer
            }
            return i
        }
        return -1
    }
}
