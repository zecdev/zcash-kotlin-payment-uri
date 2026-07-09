package org.zecdev.zip321

import org.zecdev.zip321.model.MemoBytes
import kotlin.math.ceil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
    fun `InitWithStringThrows`() {
        assertFailsWith<MemoBytes.MemoError.MemoEmpty> {
            MemoBytes("")
        }
        assertFailsWith<MemoBytes.MemoError.MemoTooLong> {
            MemoBytes("a".repeat(MemoBytes.maxLength + 1))
        }
    }

    @Test
    fun `InitWithBytesThrows`() {
        assertFailsWith<MemoBytes.MemoError.MemoEmpty> {
            MemoBytes(byteArrayOf())
        }
        assertFailsWith<MemoBytes.MemoError.MemoTooLong> {
            MemoBytes(ByteArray(MemoBytes.maxLength + 1))
        }
    }

    @Test
    fun `SingleBase64CharacterThrows`() {
        assertFailsWith<MemoBytes.MemoError.InvalidBase64URL> {
            MemoBytes.fromBase64URL("A")
        }
        assertFailsWith<MemoBytes.MemoError.InvalidBase64URL> {
            MemoBytes.fromBase64URL("AAAAA")
        }
    }
}
