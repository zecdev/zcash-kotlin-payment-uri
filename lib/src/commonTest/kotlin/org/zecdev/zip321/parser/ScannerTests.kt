package org.zecdev.zip321.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScannerTests {
    private fun isDigit(c: Char): Boolean = c in '0'..'9'

    private fun isAlpha(c: Char): Boolean = c in 'a'..'z' || c in 'A'..'Z'

    @Test
    fun `empty input is at end`() {
        val scanner = Scanner("")
        assertTrue(scanner.isAtEnd)
        assertNull(scanner.peek())
        assertNull(scanner.advance())
        assertEquals(0, scanner.currentOffset)
    }

    @Test
    fun `peek does not consume`() {
        val scanner = Scanner("ab")
        assertEquals('a', scanner.peek())
        assertEquals('a', scanner.peek())
        assertEquals(0, scanner.currentOffset)
    }

    @Test
    fun `advance consumes one character`() {
        val scanner = Scanner("ab")
        assertEquals('a', scanner.advance())
        assertEquals(1, scanner.currentOffset)
        assertEquals('b', scanner.advance())
        assertTrue(scanner.isAtEnd)
        assertNull(scanner.advance())
    }

    @Test
    fun `expect consumes on match only`() {
        val scanner = Scanner("=x")
        assertFalse(scanner.expect('&'))
        assertEquals(0, scanner.currentOffset)
        assertTrue(scanner.expect('='))
        assertEquals(1, scanner.currentOffset)
    }

    @Test
    fun `takeWhile consumes maximal run`() {
        val scanner = Scanner("123abc")
        assertEquals("123", scanner.takeWhile(::isDigit))
        assertEquals(3, scanner.currentOffset)
        // No leading match -> empty, offset unchanged.
        assertEquals("", scanner.takeWhile(::isDigit))
        assertEquals(3, scanner.currentOffset)
        assertEquals("abc", scanner.takeWhile(::isAlpha))
        assertTrue(scanner.isAtEnd)
    }

    @Test
    fun `matchLiteral consumes only on full match`() {
        val scanner = Scanner("zcash:foo")
        assertTrue(scanner.matchLiteral("zcash:"))
        assertEquals(6, scanner.currentOffset)

        val tooShort = Scanner("zcas")
        // literal longer than remaining input -> no consume
        assertFalse(tooShort.matchLiteral("zcash:"))
        assertEquals(0, tooShort.currentOffset)

        val partial = Scanner("zXcash")
        assertFalse(partial.matchLiteral("zcash:"))
        assertEquals(0, partial.currentOffset)
    }

    @Test
    fun `scans a non-ASCII character as a single unit`() {
        // A char cursor treats "é" (U+00E9, a single BMP char) as one unit.
        val scanner = Scanner("é!")
        assertEquals('é', scanner.advance())
        assertEquals('!', scanner.advance())
        assertTrue(scanner.isAtEnd)
    }

    @Test
    fun `splits on delimiter via takeWhile`() {
        val scanner = Scanner("name=value")
        assertEquals("name", scanner.takeWhile { it != '=' })
        assertTrue(scanner.expect('='))
        assertEquals("value", scanner.takeWhile { true })
        assertTrue(scanner.isAtEnd)
    }
}
