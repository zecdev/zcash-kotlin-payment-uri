package org.zecdev.zip321

import org.zecdev.zip321.extensions.qcharDecode
import org.zecdev.zip321.extensions.qcharEncoded
import org.zecdev.zip321.parser.QcharString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class QcharStringTests {
    @Test
    fun `QcharString is initialized from a valid raw string`() {
        val string = "valid QcharString"
        val result = QcharString.from(string)
        assertNotNull(result)
    }

    @Test
    fun `QcharString fails to initialize from an already qchar-encoded string in strict mode`() {
        val encodedString = "Thank%20You!"
        val result = QcharString.from(encodedString, true)
        assertNull(result)
    }

    @Test
    fun `QcharString fails to initialize from empty string`() {
        val result = QcharString.from("")
        assertNull(result)
    }

    @Test
    fun `qcharDecode returns same string for input with no encodings`() {
        val input = "nospecialcharacters"
        assertEquals("nospecialcharacters", input.qcharDecode())
    }

    @Test
    fun `qcharEncode returns same string for input with no special characters`() {
        val input = "nospecialcharacters"
        assertEquals("nospecialcharacters", input.qcharEncoded())
    }
}
