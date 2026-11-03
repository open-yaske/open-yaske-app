package org.openyaske.core.media

/** PDF の用紙。 */
enum class PdfPageSize(val widthPt: Int, val heightPt: Int) {
    A4(595, 842),
    LETTER(612, 792)
}

/** 1 ページ分の JPEG。 */
data class PdfImage(
    val jpeg: ByteArray,
    val width: Int,
    val height: Int
)

/**
 * JPEG をそのまま埋め込む最小の PDF 書き出し。
 *
 * 画像は再圧縮せず DCTDecode で流し込むので、画質は入力のまま。
 * 圧縮は呼び出し側（各プラットフォームの JPEG エンコーダ）で行う。
 */
object PdfBuilder {
    fun build(
        images: List<PdfImage>,
        pageSize: PdfPageSize = PdfPageSize.A4,
        margin: Int = 24
    ): ByteArray {
        require(images.isNotEmpty()) { "at least one image is required" }
        val out = ByteSink()
        val offsets = mutableListOf<Int>()

        out.ascii("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n")

        // 1: カタログ, 2: ページツリー
        val pageObjNumbers = images.indices.map { 3 + it * 3 }
        fun beginObj(number: Int) {
            offsets.add(out.size)
            out.ascii("$number 0 obj\n")
        }

        beginObj(1)
        out.ascii("<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

        beginObj(2)
        out.ascii("<< /Type /Pages /Count ${images.size} /Kids [")
        pageObjNumbers.forEach { out.ascii("$it 0 R ") }
        out.ascii("] >>\nendobj\n")

        images.forEachIndexed { index, image ->
            val pageNo = pageObjNumbers[index]
            val contentNo = pageNo + 1
            val imageNo = pageNo + 2
            val box = fit(image.width, image.height, pageSize, margin)

            beginObj(pageNo)
            out.ascii(
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${pageSize.widthPt} ${pageSize.heightPt}] " +
                    "/Resources << /XObject << /Im$index $imageNo 0 R >> >> " +
                    "/Contents $contentNo 0 R >>\nendobj\n"
            )

            val content = "q ${box.w} 0 0 ${box.h} ${box.x} ${box.y} cm /Im$index Do Q\n"
            beginObj(contentNo)
            out.ascii("<< /Length ${content.length} >>\nstream\n")
            out.ascii(content)
            out.ascii("endstream\nendobj\n")

            beginObj(imageNo)
            out.ascii(
                "<< /Type /XObject /Subtype /Image /Width ${image.width} /Height ${image.height} " +
                    "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode " +
                    "/Length ${image.jpeg.size} >>\nstream\n"
            )
            out.bytes(image.jpeg)
            out.ascii("\nendstream\nendobj\n")
        }

        val xrefStart = out.size
        out.ascii("xref\n0 ${offsets.size + 1}\n")
        out.ascii("0000000000 65535 f \n")
        offsets.forEach { offset ->
            out.ascii(offset.toString().padStart(10, '0') + " 00000 n \n")
        }
        out.ascii(
            "trailer\n<< /Size ${offsets.size + 1} /Root 1 0 R >>\nstartxref\n$xrefStart\n%%EOF\n"
        )
        return out.toByteArray()
    }

    private data class Box(val x: Int, val y: Int, val w: Int, val h: Int)

    /** 縦横比を保って用紙に収める。 */
    private fun fit(width: Int, height: Int, page: PdfPageSize, margin: Int): Box {
        val maxW = (page.widthPt - margin * 2).coerceAtLeast(1)
        val maxH = (page.heightPt - margin * 2).coerceAtLeast(1)
        val scale = minOf(maxW.toDouble() / width, maxH.toDouble() / height)
        val w = (width * scale).toInt().coerceAtLeast(1)
        val h = (height * scale).toInt().coerceAtLeast(1)
        return Box(
            x = (page.widthPt - w) / 2,
            y = (page.heightPt - h) / 2,
            w = w,
            h = h
        )
    }
}

/** 小さなバイト列ビルダー。共通コードだけで PDF を組み立てるために使う。 */
private class ByteSink {
    private var buffer = ByteArray(8192)
    var size: Int = 0
        private set

    fun ascii(text: String) {
        val bytes = text.encodeToByteArray()
        ensure(bytes.size)
        bytes.copyInto(buffer, size)
        size += bytes.size
    }

    fun bytes(data: ByteArray) {
        ensure(data.size)
        data.copyInto(buffer, size)
        size += data.size
    }

    fun toByteArray(): ByteArray = buffer.copyOf(size)

    private fun ensure(extra: Int) {
        if (size + extra <= buffer.size) return
        var next = buffer.size
        while (next < size + extra) next *= 2
        buffer = buffer.copyOf(next)
    }
}
