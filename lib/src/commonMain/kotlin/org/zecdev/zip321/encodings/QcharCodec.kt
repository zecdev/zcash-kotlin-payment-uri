package org.zecdev.zip321.encodings

private const val HEX_DIGITS = "0123456789ABCDEF"

/**
 * The ZIP-321 `qchar` percent-encoding codec.
 *
 * ZIP-321 defines the character set permitted (unescaped) in a parameter value:
 * ```
 * unreserved      = ALPHA / DIGIT / "-" / "." / "_" / "~"
 * allowed-delims  = "!" / "$" / "'" / "(" / ")" / "*" / "+" / "," / ";"
 * qchar           = unreserved / pct-encoded / allowed-delims / ":" / "@"
 * ```
 *
 * [encode] percent-encodes exactly the *complement* of the raw `qchar` set, mirroring the
 * reference `QCHAR_ENCODE` `AsciiSet` in librustzcash `zip321` (`lib.rs` ~518-536): every byte
 * that is not a raw `qchar` byte — space, `"`, `#`, `%`, `&`, `/`, `<`, `=`, `>`, `?`, `[`, `\`,
 * `]`, `^`, `` ` ``, `{`, `|`, `}`, the C0 controls and DEL, and every non-ASCII UTF-8 byte — is
 * written as an uppercase `%XX` escape.
 *
 * [decode] is the strict inverse used when parsing `label`/`message`/`other` values: each `%XX`
 * escape must be two hex digits (either case), each raw byte must be a `qchar` byte, and the
 * resulting decoded byte sequence must be valid UTF-8. The empty string is a valid (zero-length)
 * `*qchar` value and round-trips to itself.
 */
object QCharCodec {
    /**
     * Whether [byte] is a raw (unescaped) `qchar` byte. Note that `%` is deliberately NOT a raw
     * `qchar` byte here: it only ever appears as the leading byte of a `pct-encoded` triplet, so
     * [encode] escapes a literal `%` and [decode] treats it as an escape marker.
     */
    @Suppress("MagicNumber")
    fun isQcharByte(byte: Int): Boolean {
        val b = byte and 0xFF
        return when (b) {
            // ALPHA
            in 0x41..0x5A, in 0x61..0x7A -> true
            // DIGIT
            in 0x30..0x39 -> true
            // unreserved extras: "-" "." "_" "~"
            0x2D, 0x2E, 0x5F, 0x7E -> true
            // allowed-delims: "!" "$" "'" "(" ")" "*" "+" "," ";"
            0x21, 0x24, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x3B -> true
            // ":" "@"
            0x3A, 0x40 -> true
            else -> false
        }
    }

    /**
     * Whether [byte] may appear (raw) in a parameter value token: a `qchar` byte or `%`. This is
     * the charset accepted by the reference `qchars` parser (`alphanum_or("-._~!$'()*+,;:@%")`),
     * used by the URI tokenizer to bound a value.
     */
    @Suppress("MagicNumber")
    fun isValueByte(byte: Int): Boolean {
        // 0x25 == "%": permitted raw so the tokenizer can bound a pct-encoded value.
        return (byte and 0xFF) == 0x25 || isQcharByte(byte)
    }

    /** Percent-encodes [input] per the ZIP-321 `qchar` grammar. Always succeeds. */
    @Suppress("MagicNumber")
    fun encode(input: String): String {
        val result = StringBuilder()
        for (byte in input.encodeToByteArray()) {
            val v = byte.toInt() and 0xFF
            if (isQcharByte(v)) {
                result.append(v.toChar())
            } else {
                result.append('%')
                result.append(HEX_DIGITS[v ushr 4])
                result.append(HEX_DIGITS[v and 0x0F])
            }
        }
        return result.toString()
    }

    /**
     * Strictly percent-decodes a `qchar` value. Returns `null` if any `%` is not followed by two
     * hex digits, any raw byte is not a `qchar` byte, or the decoded bytes are not valid UTF-8.
     * The empty string decodes to the empty string.
     */
    @Suppress("MagicNumber", "ReturnCount")
    fun decode(input: String): String? {
        val bytes = input.encodeToByteArray()
        val out = ArrayList<Byte>(bytes.size)

        var i = 0
        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xFF
            if (b == 0x25) { // "%"
                if (i + 3 > bytes.size) return null
                val high = hexValue(bytes[i + 1].toInt() and 0xFF) ?: return null
                val low = hexValue(bytes[i + 2].toInt() and 0xFF) ?: return null
                out.add(((high shl 4) or low).toByte())
                i += 3
            } else {
                if (!isQcharByte(b)) return null
                out.add(bytes[i])
                i += 1
            }
        }

        // The decoded bytes must form a valid UTF-8 string (this rejects overlong sequences, lone
        // continuation bytes, unpaired surrogates, and truncated multi-byte sequences).
        return try {
            out.toByteArray().decodeToString(throwOnInvalidSequence = true)
        } catch (_: CharacterCodingException) {
            null
        }
    }

    @Suppress("MagicNumber")
    private fun hexValue(byte: Int): Int? =
        when (byte) {
            in 0x30..0x39 -> byte - 0x30 // 0-9
            in 0x41..0x46 -> byte - 0x41 + 10 // A-F
            in 0x61..0x66 -> byte - 0x61 + 10 // a-f
            else -> null
        }
}
