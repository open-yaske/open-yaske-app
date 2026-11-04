package org.openyaske.core.net

/**
 * クエリ文字列用のパーセントエンコード。
 * 英数字と `-_.~` 以外を UTF-8 のバイト列で %XX にする。
 */
fun urlEncodeQuery(value: String): String = buildString {
    for (byte in value.encodeToByteArray()) {
        val code = byte.toInt() and 0xFF
        val safe = code in 0x30..0x39 ||
            code in 0x41..0x5A ||
            code in 0x61..0x7A ||
            code == 0x2D || code == 0x2E || code == 0x5F || code == 0x7E
        if (safe) {
            append(code.toChar())
        } else {
            append('%')
            append(HEX[code shr 4])
            append(HEX[code and 0x0F])
        }
    }
}

private const val HEX = "0123456789ABCDEF"
