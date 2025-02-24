package org.zecdev.zip321
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.zecdev.zip321.model.MemoBytes
import kotlin.math.ceil

class MemoBytesTests : FunSpec({

    test("InitWithString") {
        val expectedBase64 = "VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"
        val memoBytes = MemoBytes("This is a simple memo.")
        memoBytes.toBase64URL() shouldBe expectedBase64
    }

    test("InitWithBytes") {
        val bytes = byteArrayOf(
            0x54, 0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x61, 0x20, 0x73, 0x69, 0x6d, 0x70,
            0x6c, 0x65, 0x20, 0x6d, 0x65, 0x6d, 0x6f, 0x2e
        )
        val expectedBase64 = "VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"
        val memo = MemoBytes(bytes)
        memo.toBase64URL() shouldBe expectedBase64
    }

    test("UnicodeMemo") {
        val memoUTF8Text = "This is a unicode memo ✨🦄🏆🎉"
        val expectedBase64 = "VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok"

        val memo = MemoBytes(memoUTF8Text)
        memo.toBase64URL() shouldBe expectedBase64

        MemoBytes.fromBase64URL(expectedBase64).data.contentEquals(memo.data) shouldBe true

        MemoBytes.fromBase64URL(expectedBase64).equals(memo) shouldBe true
    }

    test("InitWithStringThrows") {
        shouldThrow<MemoBytes.MemoError.MemoEmpty> {
            MemoBytes("")
        }
        shouldThrow<MemoBytes.MemoError.MemoTooLong> {
            MemoBytes("a".repeat(513))
        }
    }

    test("InitWithBytesThrows") {
        shouldThrow<MemoBytes.MemoError.MemoEmpty> {
            MemoBytes(byteArrayOf())
        }
        shouldThrow<MemoBytes.MemoError.MemoTooLong> {
            MemoBytes(ByteArray(MemoBytes.maxLength + 1))
        }
    }
})
