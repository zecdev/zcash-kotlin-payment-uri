package org.zecdev.zip321

import org.zecdev.zip321.model.NonNegativeAmount
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/*
 * NOTE (K1/v2): this file intentionally stays in `jvmTest`. It exercises the
 * `java.math.BigDecimal` interop for `NonNegativeAmount`, which is provided
 * by `jvmMain` extensions and does not exist on other targets. Every case
 * from the previous kotest `FreeSpec` is preserved 1:1 (including the exact
 * duplicates the kotest duplicate-name mangling used to keep); duplicates are
 * disambiguated with a section suffix in the function name.
 */
class AmountTests {

    // BigDecimal Conversion Tests: Constructor

    @Test
    fun `testAmountStringDecimals`() {
        assertEquals("123.456", NonNegativeAmount(BigDecimal("123.456")).toZecValueString())
        assertEquals("123.456", NonNegativeAmount(BigDecimal("123.456")).toZecValueString())
    }

    @Test
    fun `testAmountTrailing`() {
        assertEquals("50", NonNegativeAmount(BigDecimal("50.000")).toZecValueString())
    }

    @Test
    fun `testAmountLeadingZeros`() {
        assertEquals("0.5", NonNegativeAmount(BigDecimal("0000.5")).toZecValueString())
    }

    @Test
    fun `testAmountMaxDecimals`() {
        assertEquals("0.12345678", NonNegativeAmount(BigDecimal("0.12345678")).toZecValueString())
    }

    @Test
    fun `testAmountThrowsIfMaxDecimalsWithTrailingZeroes`() {
        assertFailsWith<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
            NonNegativeAmount(BigDecimal("0.123456780")).toZecValueString()
        }
    }

    @Test
    fun `testAmountThrowsIfTooManyDecimals`() {
        assertFailsWith<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
            NonNegativeAmount(BigDecimal("0.123456789")).toZecValueString()
        }
    }

    @Test
    fun `testAmountThrowsIfMaxSupply`() {
        assertFailsWith<NonNegativeAmount.AmountError> {
            NonNegativeAmount(BigDecimal("21000000.00000001")).toZecValueString()
        }
    }

    @Test
    fun `testAmountNotThrowsIfZeroAmount`() {
        assertEquals("0", NonNegativeAmount(BigDecimal("0")).toZecValueString())
    }

    @Test
    fun `testAmountThrowsIfNegativeAmount`() {
        assertFailsWith<NonNegativeAmount.AmountError> {
            NonNegativeAmount(BigDecimal("-1")).toZecValueString()
        }
    }

    // BigDecimal Conversion Tests: Factory Method

    @Test
    fun `testAmountStringDecimalsCreateMethod`() {
        assertEquals("123.456", NonNegativeAmount(BigDecimal("123.456")).toZecValueString())
    }

    @Test
    fun `testAmountTrailing factory method`() {
        assertEquals("50", NonNegativeAmount(BigDecimal("50.000")).toZecValueString())
    }

    @Test
    fun `testAmountLeadingZeros factory method`() {
        assertEquals("0.5", NonNegativeAmount(BigDecimal("0000.5")).toZecValueString())
    }

    @Test
    fun `testAmountMaxDecimals factory method`() {
        assertEquals("0.12345678", NonNegativeAmount(BigDecimal("0.12345678")).toZecValueString())
    }

    // FIXME: Fails because input is rounded
    @Test
    fun `testAmountThrowsIfMaxDecimalsWithTrailingZeroes factory method`() {
        assertFailsWith<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
            NonNegativeAmount(BigDecimal("0.123456780", MathContext(9, RoundingMode.HALF_EVEN))).toZecValueString()
        }
    }

    // FIXME: Fails because input is rounded
//    @Test
//    fun `testAmountThrowsIfTooManyDecimals factory method`() {
//        assertFailsWith<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
//            NonNegativeAmount(BigDecimal("0.123456789")).toZecValueString()
//        }
//    }

    @Test
    fun `testAmountThrowsIfMaxSupply factory method`() {
        assertFailsWith<NonNegativeAmount.AmountError> {
            NonNegativeAmount(BigDecimal("21000000.00000001")).toZecValueString()
        }
    }

    @Test
    fun `testAmountThrowsIfNegativeAmount factory method`() {
        assertFailsWith<NonNegativeAmount.AmountError> {
            NonNegativeAmount(BigDecimal("-1")).toZecValueString()
        }
    }

    @Test
    fun `testAmountDoesNotThrowIfZeroAmount`() {
        // shouldNotThrowAny: any exception fails the test.
        NonNegativeAmount(BigDecimal("0")).toZecValueString()
    }

    // Text Conversion Tests: Constructor

    @Test
    fun `testAmountThrowsIfTooManyFractionalDigits`() {
        assertFailsWith<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
            NonNegativeAmount("0.123456789")
        }
    }

    @Test
    fun `testAmountParsesMaxFractionalDigits`() {
        assertEquals(
            NonNegativeAmount(BigDecimal("0.12345678")).toZecValueString(),
            NonNegativeAmount("0.12345678").toZecValueString()
        )
    }

    @Test
    fun `testAmountThrowsIfMaxFractionalDigitsWithTrailingZeroes`() {
        assertFailsWith<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
            NonNegativeAmount("0.1234567890")
        }
    }

    @Test
    fun `testAmountParsesMaxAmount`() {
        assertEquals(
            NonNegativeAmount(BigDecimal("21000000")).toZecValueString(),
            NonNegativeAmount("21000000").toZecValueString()
        )
    }

    @Test
    fun `testAmountParsesMaxAmountWithTrailingZeroes`() {
        assertEquals(
            NonNegativeAmount(BigDecimal("21000000")).toZecValueString(),
            NonNegativeAmount("21000000.00000000").toZecValueString()
        )
    }

    @Test
    fun `testAmountThrowsIfMaxSupply text conversion`() {
        assertFailsWith<NonNegativeAmount.AmountError.GreaterThanSupply> {
            NonNegativeAmount("21000000.00000001")
        }
    }

    // Text Conversion Tests: Factory Method

    @Test
    fun `testAmountThrowsIfTooManyFractionalDigits factory method`() {
        assertFailsWith<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
            NonNegativeAmount("0.123456789")
        }
    }

    @Test
    fun `testAmountParsesMaxFractionalDigits factory method`() {
        assertEquals(
            NonNegativeAmount(BigDecimal("0.12345678")).toZecValueString(),
            NonNegativeAmount("0.12345678").toZecValueString()
        )
    }

    @Test
    fun `testAmountThrowsIfMaxFractionalDigitsWithTrailingZeroes factory method`() {
        assertFailsWith<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
            NonNegativeAmount("0.1234567890")
        }
    }

    @Test
    fun `testAmountParsesMaxAmount factory method`() {
        assertEquals(
            NonNegativeAmount(BigDecimal("21000000")).toZecValueString(),
            NonNegativeAmount("21000000").toZecValueString()
        )
    }

    @Test
    fun `testAmountParsesMaxAmountWithTrailingZeroes factory method`() {
        assertEquals(
            NonNegativeAmount(BigDecimal("21000000")).toZecValueString(),
            NonNegativeAmount("21000000.00000000").toZecValueString()
        )
    }

    @Test
    fun `testAmountThrowsIfMaxSupply text conversion factory method`() {
        assertFailsWith<NonNegativeAmount.AmountError.GreaterThanSupply> {
            NonNegativeAmount("21000000.00000001")
        }
    }

    // Equality tests

    @Test
    fun `testEquality`() {
        assertTrue(NonNegativeAmount(BigDecimal("123.456789")).equals(NonNegativeAmount("123.456789")))
    }

    @Test
    fun `testInEquality`() {
        assertFalse(NonNegativeAmount(BigDecimal("123.456789")).equals(NonNegativeAmount("123.45678")))
    }
}
