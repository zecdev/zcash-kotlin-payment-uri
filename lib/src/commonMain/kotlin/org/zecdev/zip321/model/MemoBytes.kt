package org.zecdev.zip321.model

class MemoBytes {
    companion object {
        const val maxLength: Int = 512
        fun fromBase64URL(string: String): MemoBytes {
            return string.decodeBase64URL()?.let { MemoBytes(it) } ?: throw MemoError.InvalidBase64URL
        }
    }

    val data: ByteArray
    sealed class MemoError(message: String) : RuntimeException(message) {
        object MemoTooLong : MemoError("MemoBytes exceeds max length of 512 bytes") {
            private fun readResolve(): Any = MemoTooLong
        }

        object MemoEmpty : MemoError("MemoBytes can't be initialized with empty bytes") {
            private fun readResolve(): Any = MemoEmpty
        }

        object InvalidBase64URL : MemoError("MemoBytes can't be initialized with invalid Base64URL") {
            private fun readResolve(): Any = InvalidBase64URL
        }
    }

    @Throws(MemoError::class)
    constructor(data: ByteArray) {
        require(data.isNotEmpty()) { throw MemoError.MemoEmpty }
        require(data.size <= maxLength) { throw MemoError.MemoTooLong }

        this.data = data
    }

    @Throws(MemoError::class)
    constructor(string: String) {
        require(string.isNotEmpty()) { throw MemoError.MemoEmpty }
        require(string.length <= maxLength) { throw MemoError.MemoTooLong }

        this.data = string.encodeToByteArray()
    }

    fun toBase64URL(): String {
        return data.encodeBase64URLNoPadding()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MemoBytes

        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        return 31 * data.contentHashCode()
    }
}

// Base64url alphabet (RFC 4648 section 5); no padding on encode.
private const val BASE64URL_ALPHABET =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

/**
 * Encodes bytes as unpadded base64url, byte-for-byte identical to the previous
 * `java.util.Base64.getUrlEncoder().encodeToString(data)` with `=` stripped.
 */
private fun ByteArray.encodeBase64URLNoPadding(): String {
    if (isEmpty()) return ""
    val sb = StringBuilder((size + 2) / 3 * 4)
    var i = 0
    while (i + 2 < size) {
        val n = (this[i].toInt() and 0xFF shl 16) or
            (this[i + 1].toInt() and 0xFF shl 8) or
            (this[i + 2].toInt() and 0xFF)
        sb.append(BASE64URL_ALPHABET[n ushr 18 and 0x3F])
        sb.append(BASE64URL_ALPHABET[n ushr 12 and 0x3F])
        sb.append(BASE64URL_ALPHABET[n ushr 6 and 0x3F])
        sb.append(BASE64URL_ALPHABET[n and 0x3F])
        i += 3
    }
    when (size - i) {
        1 -> {
            val n = this[i].toInt() and 0xFF
            sb.append(BASE64URL_ALPHABET[n ushr 2 and 0x3F])
            sb.append(BASE64URL_ALPHABET[n shl 4 and 0x3F])
        }
        2 -> {
            val n = (this[i].toInt() and 0xFF shl 8) or (this[i + 1].toInt() and 0xFF)
            sb.append(BASE64URL_ALPHABET[n ushr 10 and 0x3F])
            sb.append(BASE64URL_ALPHABET[n ushr 4 and 0x3F])
            sb.append(BASE64URL_ALPHABET[n shl 2 and 0x3F])
        }
    }
    return sb.toString()
}

/**
 * Decodes a base64url string into bytes, returning null on any malformed input.
 *
 * Preserves the exact behavior of the previous implementation, which mapped
 * `-`/`_` to `+`/`/`, right-padded with `=` to a multiple of 4, and then used
 * `java.util.Base64.getDecoder()`: a group of four with 3 or 4 `=` is invalid,
 * `=` outside the final group is invalid, and any character outside the
 * standard base64 alphabet is invalid.
 */
fun String.decodeBase64URL(): ByteArray? {
    // Replace base64url-specific characters, then pad to a multiple of 4.
    val normalized = StringBuilder(this.length + 3)
    for (c in this) {
        normalized.append(
            when (c) {
                '-' -> '+'
                '_' -> '/'
                else -> c
            }
        )
    }
    val padCount = (4 - normalized.length % 4) % 4
    repeat(padCount) { normalized.append('=') }
    val s = normalized.toString()

    if (s.isEmpty()) return ByteArray(0)
    if (s.length % 4 != 0) return null

    val output = ArrayList<Byte>(s.length / 4 * 3)
    var i = 0
    while (i < s.length) {
        val isLastGroup = i + 4 == s.length
        var acc = 0
        var pads = 0
        for (j in 0 until 4) {
            val c = s[i + j]
            if (c == '=') {
                // Padding only allowed in the last group, only as a suffix.
                if (!isLastGroup) return null
                if (j < 2) return null // at least 2 data chars required
                pads++
                acc = acc shl 6
            } else {
                if (pads > 0) return null // data char after padding
                val v = base64Value(c)
                if (v < 0) return null
                acc = (acc shl 6) or v
            }
        }
        output.add((acc ushr 16 and 0xFF).toByte())
        if (pads < 2) output.add((acc ushr 8 and 0xFF).toByte())
        if (pads < 1) output.add((acc and 0xFF).toByte())
        i += 4
    }
    return output.toByteArray()
}

private fun base64Value(c: Char): Int = when (c) {
    in 'A'..'Z' -> c - 'A'
    in 'a'..'z' -> c - 'a' + 26
    in '0'..'9' -> c - '0' + 52
    '+' -> 62
    '/' -> 63
    else -> -1
}
