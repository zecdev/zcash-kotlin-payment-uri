package org.zecdev.zip321.parser

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Base64URLTests {
    // MARK: - RFC 4648 §10 test vectors, adapted to the unpadded §5 encoding.
    private val rfc4648Vectors: List<Pair<String, String>> =
        listOf(
            "" to "",
            "f" to "Zg",
            "fo" to "Zm8",
            "foo" to "Zm9v",
            "foob" to "Zm9vYg",
            "fooba" to "Zm9vYmE",
            "foobar" to "Zm9vYmFy",
        )

    @Test
    fun rfc4648VectorEncodes() {
        for ((plain, encoded) in rfc4648Vectors) {
            assertEquals(encoded, Base64URL.encode(plain.encodeToByteArray()))
        }
    }

    @Test
    fun rfc4648VectorDecodes() {
        for ((plain, encoded) in rfc4648Vectors) {
            assertContentEquals(plain.encodeToByteArray(), Base64URL.decode(encoded))
        }
    }

    // MARK: - conformance corpus memo strings (zcash-zip321-test-vectors /
    // librustzcash zip321 lib.rs memo round-trip vectors).
    private val corpusMemoVectors: List<Pair<String, String>> =
        listOf(
            "This is a simple memo." to "VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg",
            "{ \"key\": \"This is a JSON-structured memo.\" }" to
                "eyAia2V5IjogIlRoaXMgaXMgYSBKU09OLXN0cnVjdHVyZWQgbWVtby4iIH0",
            "This is a unicode memo ✨🦄🏆🎉" to "VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok",
        )

    @Test
    fun corpusMemoRoundTrips() {
        for ((utf8Memo, encoded) in corpusMemoVectors) {
            val bytes = utf8Memo.encodeToByteArray()

            assertEquals(encoded, Base64URL.encode(bytes))
            assertContentEquals(bytes, Base64URL.decode(encoded))
        }
    }

    /**
     * the URL-safe alphabet uses `-` (62) and `_` (63) where classic base64
     * uses `+` and `/`.
     */
    @Test
    fun urlSafeAlphabetCharacters() {
        assertEquals("_w", Base64URL.encode(bytes(0xFF)))
        assertEquals("-_8", Base64URL.encode(bytes(0xFB, 0xFF)))
        assertContentEquals(bytes(0xFF), Base64URL.decode("_w"))
        assertContentEquals(bytes(0xFB, 0xFF), Base64URL.decode("-_8"))
    }

    @Test
    fun fullAlphabetRoundTrip() {
        val allChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        val expected =
            bytes(
                0x00, 0x10, 0x83, 0x10, 0x51, 0x87, 0x20, 0x92, 0x8b, 0x30, 0xd3, 0x8f, 0x41, 0x14, 0x93, 0x51,
                0x55, 0x97, 0x61, 0x96, 0x9b, 0x71, 0xd7, 0x9f, 0x82, 0x18, 0xa3, 0x92, 0x59, 0xa7, 0xa2, 0x9a,
                0xab, 0xb2, 0xdb, 0xaf, 0xc3, 0x1c, 0xb3, 0xd3, 0x5d, 0xb7, 0xe3, 0x9e, 0xbb, 0xf3, 0xdf, 0xbf,
            )

        assertContentEquals(expected, Base64URL.decode(allChars))
        assertEquals(allChars, Base64URL.encode(expected))
    }

    // MARK: - rejections

    private val rejectedStrings: List<Pair<String, String>> =
        listOf(
            "Zm9v+g" to "classic-base64 '+' is not in the url-safe alphabet",
            "Zm9v/g" to "classic-base64 '/' is not in the url-safe alphabet",
            "Zg==" to "'=' padding is forbidden in the unpadded encoding",
            "Zm8=" to "'=' padding is forbidden in the unpadded encoding",
            "AB=" to "'=' padding is forbidden even when it fixes length % 4",
            "A===" to "padding-only completion is forbidden",
            "A" to "length % 4 == 1 is impossible for any byte sequence",
            "Zm9vY" to "length % 4 == 1 is impossible for any byte sequence",
            "Zg Zg" to "whitespace is rejected",
            " Zg" to "leading whitespace is rejected",
            "Zg\n" to "trailing newline is rejected",
            "Zg\t" to "tab is rejected",
            "QR" to "nonzero trailing bits (non-canonical encoding)",
            "Zm9vYh" to "nonzero trailing bits (non-canonical encoding)",
            "Zm9vYmF!" to "'!' is outside the base64url alphabet",
            "····" to "non-ASCII characters are rejected",
            "QTw+Qg" to "'<'-containing classic base64 probe: '+' rejected",
        )

    @Test
    fun decodeRejects() {
        for ((input, reason) in rejectedStrings) {
            assertNull(Base64URL.decode(input), reason)
        }
    }

    /** the empty string is the canonical encoding of zero bytes. */
    @Test
    fun emptyStringDecodesToEmptyBytes() {
        assertContentEquals(ByteArray(0), Base64URL.decode(""))
        assertEquals("", Base64URL.encode(ByteArray(0)))
    }

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }
}
