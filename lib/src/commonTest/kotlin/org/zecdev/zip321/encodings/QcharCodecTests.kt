package org.zecdev.zip321.encodings

import kotlin.test.Test
import kotlin.test.assertEquals

class QCharCodecTests {
    @Test
    fun `should encode + correctly`() {
        val expected = "apple+bananas"
        assertEquals(expected, QCharCodec.encode("apple+bananas"))
    }

    @Test
    fun `should encode string with spaces and special characters`() {
        val input = "hello world! & clean = 100%"
        val expected = "hello%20world!%20%26%20clean%20%3D%20100%25"
        assertEquals(expected, QCharCodec.encode(input))
    }

    @Test
    fun `should decode encoded string back to original`() {
        val encoded = "hello%20world!%20%26%20clean%20%3D%20100%25"
        val expected = "hello world! & clean = 100%"
        assertEquals(expected, QCharCodec.decode(encoded))
    }

    @Test
    fun `should not encode allowed qchar characters`() {
        val input = "AZaz09-._~!\$'()*+,;:"
        assertEquals(input, QCharCodec.encode(input))
    }

    @Test
    fun `should correctly encode non-ascii characters`() {
        val input = "café ∆"
        val encoded = QCharCodec.encode(input)
        assertEquals("caf%C3%A9%20%E2%88%86", encoded)
        assertEquals(input, QCharCodec.decode(encoded))
    }

    @Test
    fun `round-trip encoding and decoding should return the original string`() {
        val inputs =
            listOf(
                "simple",
                "with spaces",
                "with symbols !@#$%^&*()",
                "unicode π≈ß漢字",
                "edge-case: % & = ? /",
            )
        for (input in inputs) {
            assertEquals(input, QCharCodec.decode(QCharCodec.encode(input)))
        }
    }
}
