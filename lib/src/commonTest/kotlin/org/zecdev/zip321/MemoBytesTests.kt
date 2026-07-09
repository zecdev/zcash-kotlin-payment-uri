package org.zecdev.zip321

import org.zecdev.zip321.model.MemoBytes
import org.zecdev.zip321.parser.Base64URL
import kotlin.math.ceil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MemoBytesTests {
    @Test
    fun `InitWithString`() {
        val expectedBase64 = "VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"
        val memoBytes = MemoBytes("This is a simple memo.")
        assertEquals(expectedBase64, memoBytes.toBase64URL())
    }

    @Test
    fun `InitWithBytes`() {
        val bytes =
            byteArrayOf(
                0x54, 0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x61, 0x20, 0x73, 0x69, 0x6d, 0x70,
                0x6c, 0x65, 0x20, 0x6d, 0x65, 0x6d, 0x6f, 0x2e,
            )
        val expectedBase64 = "VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"
        val memo = MemoBytes(bytes)
        assertEquals(expectedBase64, memo.toBase64URL())
        assertTrue(MemoBytes.fromBase64URL(expectedBase64).equals(memo))
        assertTrue(MemoBytes.fromBase64URL(expectedBase64).data.contentEquals(memo.data))
    }

    @Test
    fun `InitWithOneNullByte`() {
        val bytes = byteArrayOf(0x0)
        val expectedBase64 = "AA"
        val memo = MemoBytes(bytes)
        assertEquals(expectedBase64, memo.toBase64URL())
        assertTrue(MemoBytes.fromBase64URL(expectedBase64).equals(memo))
        assertTrue(MemoBytes.fromBase64URL(expectedBase64).data.contentEquals(memo.data))
    }

    @Test
    fun `InitWithMaxNullBytes`() {
        val bytes = ByteArray(MemoBytes.maxLength)
        val expectedBase64 = "A".repeat(ceil(MemoBytes.maxLength * 8 / 6.0).toInt())
        val memo = MemoBytes(bytes)
        assertEquals(expectedBase64, memo.toBase64URL())
        assertTrue(MemoBytes.fromBase64URL(expectedBase64).equals(memo))
        assertTrue(MemoBytes.fromBase64URL(expectedBase64).data.contentEquals(memo.data))
    }

    @Test
    fun `UnicodeMemo`() {
        val memoUTF8Text = "This is a unicode memo ✨🦄🏆🎉"
        val expectedBase64 = "VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok"

        val memo = MemoBytes(memoUTF8Text)
        assertEquals(expectedBase64, memo.toBase64URL())

        assertTrue(MemoBytes.fromBase64URL(expectedBase64).data.contentEquals(memo.data))

        assertTrue(MemoBytes.fromBase64URL(expectedBase64).equals(memo))
    }

    @Test
    fun `UTF8StringRoundTrip`() {
        val memo = MemoBytes("This is a unicode memo ✨🦄🏆🎉")
        val decoded = MemoBytes.fromBase64URL(memo.toBase64URL())

        assertEquals(memo, decoded)
        assertEquals("This is a unicode memo ✨🦄🏆🎉", decoded.data.decodeToString())
    }

    @Test
    fun `RawBytesRoundTrip`() {
        val bytes = byteArrayOf(0x00, 0xFF.toByte(), 0x10, 0x80.toByte(), 0x7F)

        val memo = MemoBytes(bytes)
        val decoded = MemoBytes.fromBase64URL(memo.toBase64URL())

        assertEquals(memo, decoded)
        assertTrue(decoded.data.contentEquals(bytes))
    }

    // MARK: - length boundaries: 0..512 bytes are valid, 513 is not.

    @Test
    fun `EmptyMemoIsValidAndRoundTrips`() {
        // consensus zero-pads memos to 512 bytes, so a zero-length memo is a
        // well-defined empty memo (`memo=` in a ZIP-321 URI).
        val fromBytes = MemoBytes(byteArrayOf())
        val fromString = MemoBytes("")
        val fromBase64 = MemoBytes.fromBase64URL("")

        assertEquals(fromString, fromBytes)
        assertEquals(fromBase64, fromBytes)
        assertEquals("", fromBytes.toBase64URL())
        assertTrue(fromBytes.data.isEmpty())
    }

    @Test
    fun `LengthBoundaries`() {
        assertEquals(0, MemoBytes(byteArrayOf()).data.size)
        assertEquals(1, MemoBytes(byteArrayOf(0x61)).data.size)
        assertEquals(512, MemoBytes(ByteArray(MemoBytes.maxLength) { 0x61 }).data.size)

        assertFailsWith<MemoBytes.MemoError.MemoTooLong> {
            MemoBytes(ByteArray(MemoBytes.maxLength + 1) { 0x61 })
        }

        assertEquals(512, MemoBytes("a".repeat(MemoBytes.maxLength)).data.size)

        assertFailsWith<MemoBytes.MemoError.MemoTooLong> {
            MemoBytes("a".repeat(MemoBytes.maxLength + 1))
        }

        // 513 bytes of valid base64url decode fine but exceed the memo limit.
        val oversized = Base64URL.encode(ByteArray(MemoBytes.maxLength + 1) { 0x61 })
        assertFailsWith<MemoBytes.MemoError.MemoTooLong> {
            MemoBytes.fromBase64URL(oversized)
        }
    }

    // MARK: - base64url rejections (strict unpadded RFC 4648 §5 decoding)

    private val rejectedBase64URLStrings: List<Pair<String, String>> =
        listOf(
            "QTw+Qg" to "'+' belongs to classic base64, not base64url",
            "QTw/Qg" to "'/' belongs to classic base64, not base64url",
            "Zg==" to "'=' padding is forbidden",
            "AB=" to "'=' padding is forbidden",
            "A===" to "'=' padding is forbidden",
            "A" to "length % 4 == 1 is impossible",
            "Zg Zg" to "whitespace is rejected",
            "Zg\n" to "whitespace is rejected",
            "QR" to "nonzero trailing bits (non-canonical encoding)",
            "····" to "non-ASCII characters are rejected",
        )

    @Test
    fun `InitWithInvalidBase64URLFails`() {
        for ((input, reason) in rejectedBase64URLStrings) {
            assertFailsWith<MemoBytes.MemoError.InvalidBase64URL>(reason) {
                MemoBytes.fromBase64URL(input)
            }
        }
    }

    // MARK: - equals()/hashCode() contract

    @Test
    fun `equals handles identity null a different type and unequal content`() {
        val memo = MemoBytes("hello")
        assertTrue(memo == memo) // this === other fast path
        assertFalse(memo.equals(null))
        @Suppress("EqualsBetweenInconvertibleTypes")
        assertFalse(memo.equals("not a MemoBytes"))
        assertFalse(memo.equals(MemoBytes("goodbye")))
        assertEquals(memo.hashCode(), MemoBytes("hello").hashCode())
    }
}
