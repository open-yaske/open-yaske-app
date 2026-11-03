package org.openyaske.core.media

private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

fun encodeBase64(data: ByteArray): String {
    if (data.isEmpty()) return ""
    val out = StringBuilder((data.size + 2) / 3 * 4)
    var i = 0
    while (i < data.size) {
        val b0 = data[i].toInt() and 0xFF
        val b1 = if (i + 1 < data.size) data[i + 1].toInt() and 0xFF else 0
        val b2 = if (i + 2 < data.size) data[i + 2].toInt() and 0xFF else 0
        out.append(ALPHABET[b0 shr 2])
        out.append(ALPHABET[((b0 and 0x03) shl 4) or (b1 shr 4)])
        out.append(if (i + 1 < data.size) ALPHABET[((b1 and 0x0F) shl 2) or (b2 shr 6)] else '=')
        out.append(if (i + 2 < data.size) ALPHABET[b2 and 0x3F] else '=')
        i += 3
    }
    return out.toString()
}

fun decodeBase64(text: String): ByteArray {
    val clean = text.filter { it != '\n' && it != '\r' && it != ' ' }
    if (clean.isEmpty()) return ByteArray(0)
    val out = ByteArray(clean.length * 3 / 4 + 3)
    var buffer = 0
    var bits = 0
    var index = 0
    for (ch in clean) {
        if (ch == '=') break
        val value = ALPHABET.indexOf(ch)
        if (value < 0) continue
        buffer = (buffer shl 6) or value
        bits += 6
        if (bits >= 8) {
            bits -= 8
            out[index++] = ((buffer shr bits) and 0xFF).toByte()
        }
    }
    return out.copyOf(index)
}
