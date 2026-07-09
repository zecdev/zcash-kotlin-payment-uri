package org.zecdev.zip321.encodings
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class QCharCodecTests : StringSpec({
    "should encode + correctly" {
        val expected = "apple+bananas"
        QCharCodec.encode("apple+bananas") shouldBe expected
    }
    "should encode string with spaces and special characters" {
        val input = "hello world! & clean = 100%"
        val expected = "hello%20world!%20%26%20clean%20%3D%20100%25"
        QCharCodec.encode(input) shouldBe expected
    }
    "should decode encoded string back to original" {
        val encoded = "hello%20world!%20%26%20clean%20%3D%20100%25"
        val expected = "hello world! & clean = 100%"
        QCharCodec.decode(encoded) shouldBe expected
    }

    "should not encode allowed qchar characters" {
        val input = "AZaz09-._~!\$'()*+,;:"
        QCharCodec.encode(input) shouldBe input
    }

    "should correctly encode non-ascii characters" {
        val input = "café ∆"
        val encoded = QCharCodec.encode(input)
        encoded shouldBe "caf%C3%A9%20%E2%88%86"
        QCharCodec.decode(encoded) shouldBe input
    }

    "round-trip encoding and decoding should return the original string" {
        val inputs = listOf(
            "simple",
            "with spaces",
            "with symbols !@#$%^&*()",
            "unicode π≈ß漢字",
            "edge-case: % & = ? /"
        )
        for (input in inputs) {
            QCharCodec.decode(QCharCodec.encode(input)) shouldBe input
        }
    }
})