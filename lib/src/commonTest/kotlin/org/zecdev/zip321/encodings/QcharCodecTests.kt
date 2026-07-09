package org.zecdev.zip321.encodings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QCharCodecTests {
    // MARK: encode

    @Test
    fun `encodes space as percent 20`() {
        assertEquals("%20", QCharCodec.encode(" "))
        assertEquals("Thank%20you", QCharCodec.encode("Thank you"))
    }

    @Test
    fun `allowed delims and colon at pass through raw`() {
        // qchar allowed-delims / ":" / "@" appear unescaped.
        assertEquals("!\$'()*+,;:@", QCharCodec.encode("!\$'()*+,;:@"))
        assertEquals("sk8:forever@!", QCharCodec.encode("sk8:forever@!"))
    }

    @Test
    fun `unreserved passes through raw`() {
        assertEquals("aZ09-._~", QCharCodec.encode("aZ09-._~"))
    }

    @Test
    fun `encodes the complement of qchar`() {
        // Exactly the reference QCHAR_ENCODE set (uppercase hex).
        assertEquals("%22", QCharCodec.encode("\""))
        assertEquals("%23", QCharCodec.encode("#"))
        assertEquals("%25", QCharCodec.encode("%"))
        assertEquals("%26", QCharCodec.encode("&"))
        assertEquals("%2F", QCharCodec.encode("/"))
        assertEquals("%3C", QCharCodec.encode("<"))
        assertEquals("%3D", QCharCodec.encode("="))
        assertEquals("%3E", QCharCodec.encode(">"))
        assertEquals("%3F", QCharCodec.encode("?"))
        assertEquals("%5B", QCharCodec.encode("["))
        assertEquals("%5C", QCharCodec.encode("\\"))
        assertEquals("%5D", QCharCodec.encode("]"))
        assertEquals("%5E", QCharCodec.encode("^"))
        assertEquals("%60", QCharCodec.encode("`"))
        assertEquals("%7B", QCharCodec.encode("{"))
        assertEquals("%7C", QCharCodec.encode("|"))
        assertEquals("%7D", QCharCodec.encode("}"))
    }

    @Test
    fun `encodes control characters and DEL`() {
        assertEquals("%00", QCharCodec.encode("\u0000"))
        assertEquals("%1F", QCharCodec.encode("\u001F"))
        assertEquals("%7F", QCharCodec.encode("\u007F")) // DEL
    }

    @Test
    fun `encodes non-ASCII as UTF-8 bytes`() {
        // é = U+00E9 = UTF-8 C3 A9
        assertEquals("%C3%A9", QCharCodec.encode("é"))
        // € = U+20AC = UTF-8 E2 82 AC
        assertEquals("%E2%82%AC", QCharCodec.encode("€"))
        // 😀 = U+1F600 = UTF-8 F0 9F 98 80
        assertEquals("%F0%9F%98%80", QCharCodec.encode("😀"))
    }

    // MARK: decode

    @Test
    fun `decode is inverse of encode for unicode and emoji`() {
        val samples =
            listOf(
                "",
                "plain",
                "Thank you for your purchase",
                "Order #321",
                "Your Ben & Jerry's Order",
                "sk8:forever@!",
                "café",
                "€100",
                "gm 😀 zcash",
                "100% sure",
            )

        for (sample in samples) {
            val encoded = QCharCodec.encode(sample)
            assertEquals(sample, QCharCodec.decode(encoded), "round-trip failed for $sample")
        }
    }

    @Test
    fun `empty string round trips`() {
        assertEquals("", QCharCodec.encode(""))
        assertEquals("", QCharCodec.decode(""))
    }

    @Test
    fun `decodes lowercase and uppercase hex`() {
        assertEquals("/", QCharCodec.decode("%2f"))
        assertEquals("/", QCharCodec.decode("%2F"))
        assertEquals("é", QCharCodec.decode("%c3%a9"))
    }

    @Test
    fun `rejects invalid hex escapes`() {
        assertNull(QCharCodec.decode("%2")) // truncated
        assertNull(QCharCodec.decode("%")) // lone percent
        assertNull(QCharCodec.decode("%2G")) // non-hex digit
        assertNull(QCharCodec.decode("%GG"))
        assertNull(QCharCodec.decode("abc%"))
        assertNull(QCharCodec.decode("abc%A"))
    }

    @Test
    fun `rejects raw non-qchar bytes`() {
        // A raw space / non-qchar character must be percent-encoded, not present raw.
        assertNull(QCharCodec.decode("a b"))
        assertNull(QCharCodec.decode("a=b"))
        assertNull(QCharCodec.decode("a#b"))
        assertNull(QCharCodec.decode("café")) // raw non-ASCII byte
    }

    @Test
    fun `rejects overlong and invalid UTF-8 sequences`() {
        // Overlong encoding of "/" (0x2F): C0 AF — must be rejected.
        assertNull(QCharCodec.decode("%C0%AF"))
        // Lone continuation byte.
        assertNull(QCharCodec.decode("%80"))
        // Truncated 2-byte sequence (missing continuation).
        assertNull(QCharCodec.decode("%C3"))
        // Unpaired high surrogate (ED A0 80).
        assertNull(QCharCodec.decode("%ED%A0%80"))
    }

    @Test
    fun `literal percent round trips`() {
        assertEquals("50%25", QCharCodec.encode("50%"))
        assertEquals("50%", QCharCodec.decode("50%25"))
    }

    @Test
    fun `hexValue covers every digit and hex-letter nibble in both escape positions`() {
        // Exercises every branch of decode()'s private hexValue helper for the digit / uppercase
        // hex-letter / lowercase hex-letter ranges, with each range appearing in BOTH the high and
        // low nibble position (not just paired with a digit, as the other tests above happen to
        // do). Not every pairing decodes to valid UTF-8 on its own; only hexValue's own behavior
        // is under test here, so a `null` result is as acceptable as a decoded string.
        for (hi in listOf('0', '9', 'A', 'F', 'a', 'f')) {
            for (lo in listOf('0', '9', 'A', 'F', 'a', 'f')) {
                QCharCodec.decode("%$hi$lo")
            }
        }
    }
}
