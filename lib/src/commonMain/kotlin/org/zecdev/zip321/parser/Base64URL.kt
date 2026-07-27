package org.zecdev.zip321.parser

/**
 * A strict [RFC 4648 §5](https://www.rfc-editor.org/rfc/rfc4648.html#section-5)
 * base64url codec **without padding**, matching the encoding ZIP-321 mandates for
 * `memo` parameter values (the reference implementation uses
 * `BASE64_URL_SAFE_NO_PAD`).
 *
 * Unlike the lenient translate-and-pad decoders, this codec:
 * - uses the URL-safe alphabet (`-` and `_` instead of `+` and `/`),
 * - never emits `=` padding on encode,
 * - rejects on decode: `+`, `/`, `=`, whitespace, any character outside the
 *   base64url alphabet, impossible lengths (`length % 4 == 1`), and encodings
 *   whose trailing bits are nonzero (non-canonical encodings).
 *
 * The numeric shifts and masks throughout are the RFC 4648 bit layout
 * (3 bytes == 4 sextets), not tunable values.
 */
@Suppress("MagicNumber")
internal object Base64URL {
    /** The RFC 4648 §5 base64url alphabet. */
    private val ALPHABET: ByteArray =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".encodeToByteArray()

    /**
     * Maps an ASCII byte to its 6-bit alphabet index, or `-1` if the byte is
     * not part of the base64url alphabet.
     */
    private val DECODE_TABLE: IntArray =
        IntArray(256) { -1 }.also { table ->
            ALPHABET.forEachIndexed { index, char ->
                table[char.toInt()] = index
            }
        }

    /** Encodes [bytes] as an unpadded base64url string. */
    fun encode(bytes: ByteArray): String {
        val output = StringBuilder((bytes.size * 4 + 2) / 3)

        var index = 0
        while (index + 3 <= bytes.size) {
            val chunk = (bytes.byteAt(index) shl 16) or (bytes.byteAt(index + 1) shl 8) or bytes.byteAt(index + 2)
            output.append(ALPHABET[(chunk shr 18) and 0x3F].toInt().toChar())
            output.append(ALPHABET[(chunk shr 12) and 0x3F].toInt().toChar())
            output.append(ALPHABET[(chunk shr 6) and 0x3F].toInt().toChar())
            output.append(ALPHABET[chunk and 0x3F].toInt().toChar())
            index += 3
        }

        when (bytes.size - index) {
            1 -> {
                val chunk = bytes.byteAt(index) shl 16
                output.append(ALPHABET[(chunk shr 18) and 0x3F].toInt().toChar())
                output.append(ALPHABET[(chunk shr 12) and 0x3F].toInt().toChar())
            }
            2 -> {
                val chunk = (bytes.byteAt(index) shl 16) or (bytes.byteAt(index + 1) shl 8)
                output.append(ALPHABET[(chunk shr 18) and 0x3F].toInt().toChar())
                output.append(ALPHABET[(chunk shr 12) and 0x3F].toInt().toChar())
                output.append(ALPHABET[(chunk shr 6) and 0x3F].toInt().toChar())
            }
        }

        return output.toString()
    }

    /**
     * Decodes an unpadded base64url string into its bytes.
     * @return the decoded bytes, or `null` if [string] is not a canonical,
     * unpadded base64url encoding (see the object documentation for the exact
     * rejection rules). The empty string decodes to the empty byte array.
     *
     * Note: grammar-driven early exits mirroring the Swift reference codec.
     */
    @Suppress("ReturnCount")
    fun decode(string: String): ByteArray? {
        // Non-ASCII characters produce multi-byte UTF-8 sequences whose bytes are
        // >= 0x80 and therefore map to -1 in the decode table below, so they are
        // rejected by the per-character check without special-casing.
        val input = string.encodeToByteArray()

        // an unpadded base64url encoding of n bytes has length 4*(n/3) plus
        // 0, 2, or 3 characters for the final partial group; length % 4 == 1
        // is impossible.
        if (input.size % 4 == 1) return null

        val output = ByteArray(input.size * 3 / 4)
        var outputCount = 0

        var buffer = 0
        var bitsCollected = 0

        for (char in input) {
            val sextet = DECODE_TABLE[char.toInt() and 0xFF]
            if (sextet < 0) return null

            buffer = (buffer shl 6) or sextet
            bitsCollected += 6

            if (bitsCollected >= 8) {
                bitsCollected -= 8
                output[outputCount++] = ((buffer shr bitsCollected) and 0xFF).toByte()
            }
        }

        // a canonical encoding zero-pads the final sextet: any leftover bits
        // must be zero (e.g. "QR" is rejected because 'R' carries nonzero
        // bits beyond the single encoded byte).
        if (buffer and ((1 shl bitsCollected) - 1) != 0) return null

        return output
    }

    private fun ByteArray.byteAt(index: Int): Int = this[index].toInt() and 0xFF
}
