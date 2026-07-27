// `LegacyAmount` (the v1 amount type; it carried the `NonNegativeAmount` name before v2) is
// deprecated in favor of the v2 `NonNegativeAmount` but remains in use until the parser adopts
// it (v2 parser rewrite); keep this file warning-free meanwhile.
@file:Suppress("DEPRECATION")

package org.zecdev.zip321

import org.zecdev.zip321.model.LegacyAmount
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
 * `java.math.BigDecimal` interop for `LegacyAmount`, which is provided
 * by `jvmMain` extensions and does not exist on other targets. Every case
 * from the previous kotest `FreeSpec` is preserved 1:1 (including the exact
 * duplicates the kotest duplicate-name mangling used to keep); duplicates are
 * disambiguated with a section suffix in the function name.
 */
class AmountTests {
    // BigDecimal Conversion Tests: Constructor

    @Test
    fun `testAmountStringDecimals`() {
        assertEquals("123.456", LegacyAmount(BigDecimal("123.456")).toZecValueString())
        assertEquals("123.456", LegacyAmount(BigDecimal("123.456")).toZecValueString())
    }

    @Test
    fun `testAmountTrailing`() {
        assertEquals("50", LegacyAmount(BigDecimal("50.000")).toZecValueString())
    }

    @Test
    fun `testAmountLeadingZeros`() {
        assertEquals("0.5", LegacyAmount(BigDecimal("0000.5")).toZecValueString())
    }

    @Test
    fun `testAmountMaxDecimals`() {
        assertEquals("0.12345678", LegacyAmount(BigDecimal("0.12345678")).toZecValueString())
    }

    @Test
    fun `testAmountThrowsIfMaxDecimalsWithTrailingZeroes`() {
        assertFailsWith<LegacyAmount.AmountError.TooManyFractionalDigits> {
            LegacyAmount(BigDecimal("0.123456780")).toZecValueString()
        }
    }

    @Test
    fun `testAmountThrowsIfTooManyDecimals`() {
        assertFailsWith<LegacyAmount.AmountError.TooManyFractionalDigits> {
            LegacyAmount(BigDecimal("0.123456789")).toZecValueString()
        }
    }

    @Test
    fun `testAmountThrowsIfMaxSupply`() {
        assertFailsWith<LegacyAmount.AmountError> {
            LegacyAmount(BigDecimal("21000000.00000001")).toZecValueString()
        }
    }

    @Test
    fun `testAmountNotThrowsIfZeroAmount`() {
        assertEquals("0", LegacyAmount(BigDecimal("0")).toZecValueString())
    }

    @Test
    fun `testAmountThrowsIfNegativeAmount`() {
        assertFailsWith<LegacyAmount.AmountError> {
            LegacyAmount(BigDecimal("-1")).toZecValueString()
        }
    }

    // BigDecimal Conversion Tests: Factory Method

    @Test
    fun `testAmountStringDecimalsCreateMethod`() {
        assertEquals("123.456", LegacyAmount(BigDecimal("123.456")).toZecValueString())
    }

    @Test
    fun `testAmountTrailing factory method`() {
        assertEquals("50", LegacyAmount(BigDecimal("50.000")).toZecValueString())
    }

    @Test
    fun `testAmountLeadingZeros factory method`() {
        assertEquals("0.5", LegacyAmount(BigDecimal("0000.5")).toZecValueString())
    }

    @Test
    fun `testAmountMaxDecimals factory method`() {
        assertEquals("0.12345678", LegacyAmount(BigDecimal("0.12345678")).toZecValueString())
    }

    // FIXME: Fails because input is rounded
    @Test
    fun `testAmountThrowsIfMaxDecimalsWithTrailingZeroes factory method`() {
        assertFailsWith<LegacyAmount.AmountError.TooManyFractionalDigits> {
            LegacyAmount(BigDecimal("0.123456780", MathContext(9, RoundingMode.HALF_EVEN))).toZecValueString()
        }
    }

    // FIXME: Fails because input is rounded
//    @Test
//    fun `testAmountThrowsIfTooManyDecimals factory method`() {
//        assertFailsWith<LegacyAmount.AmountError.TooManyFractionalDigits> {
//            LegacyAmount(BigDecimal("0.123456789")).toZecValueString()
//        }
//    }

    @Test
    fun `testAmountThrowsIfMaxSupply factory method`() {
        assertFailsWith<LegacyAmount.AmountError> {
            LegacyAmount(BigDecimal("21000000.00000001")).toZecValueString()
        }
    }

    @Test
    fun `testAmountThrowsIfNegativeAmount factory method`() {
        assertFailsWith<LegacyAmount.AmountError> {
            LegacyAmount(BigDecimal("-1")).toZecValueString()
        }
    }

    @Test
    fun `testAmountDoesNotThrowIfZeroAmount`() {
        // shouldNotThrowAny: any exception fails the test.
        LegacyAmount(BigDecimal("0")).toZecValueString()
    }

    // Text Conversion Tests: Constructor

    @Test
    fun `testAmountThrowsIfTooManyFractionalDigits`() {
        assertFailsWith<LegacyAmount.AmountError.TooManyFractionalDigits> {
            LegacyAmount("0.123456789")
        }
    }

    @Test
    fun `testAmountParsesMaxFractionalDigits`() {
        assertEquals(
            LegacyAmount(BigDecimal("0.12345678")).toZecValueString(),
            LegacyAmount("0.12345678").toZecValueString(),
        )
    }

    @Test
    fun `testAmountThrowsIfMaxFractionalDigitsWithTrailingZeroes`() {
        assertFailsWith<LegacyAmount.AmountError.TooManyFractionalDigits> {
            LegacyAmount("0.1234567890")
        }
    }

    @Test
    fun `testAmountParsesMaxAmount`() {
        assertEquals(
            LegacyAmount(BigDecimal("21000000")).toZecValueString(),
            LegacyAmount("21000000").toZecValueString(),
        )
    }

    @Test
    fun `testAmountParsesMaxAmountWithTrailingZeroes`() {
        assertEquals(
            LegacyAmount(BigDecimal("21000000")).toZecValueString(),
            LegacyAmount("21000000.00000000").toZecValueString(),
        )
    }

    @Test
    fun `testAmountThrowsIfMaxSupply text conversion`() {
        assertFailsWith<LegacyAmount.AmountError.GreaterThanSupply> {
            LegacyAmount("21000000.00000001")
        }
    }

    // Text Conversion Tests: Factory Method

    @Test
    fun `testAmountThrowsIfTooManyFractionalDigits factory method`() {
        assertFailsWith<LegacyAmount.AmountError.TooManyFractionalDigits> {
            LegacyAmount("0.123456789")
        }
    }

    @Test
    fun `testAmountParsesMaxFractionalDigits factory method`() {
        assertEquals(
            LegacyAmount(BigDecimal("0.12345678")).toZecValueString(),
            LegacyAmount("0.12345678").toZecValueString(),
        )
    }

    @Test
    fun `testAmountThrowsIfMaxFractionalDigitsWithTrailingZeroes factory method`() {
        assertFailsWith<LegacyAmount.AmountError.TooManyFractionalDigits> {
            LegacyAmount("0.1234567890")
        }
    }

    @Test
    fun `testAmountParsesMaxAmount factory method`() {
        assertEquals(
            LegacyAmount(BigDecimal("21000000")).toZecValueString(),
            LegacyAmount("21000000").toZecValueString(),
        )
    }

    @Test
    fun `testAmountParsesMaxAmountWithTrailingZeroes factory method`() {
        assertEquals(
            LegacyAmount(BigDecimal("21000000")).toZecValueString(),
            LegacyAmount("21000000.00000000").toZecValueString(),
        )
    }

    @Test
    fun `testAmountThrowsIfMaxSupply text conversion factory method`() {
        assertFailsWith<LegacyAmount.AmountError.GreaterThanSupply> {
            LegacyAmount("21000000.00000001")
        }
    }

    // Equality tests

    @Test
    fun `testEquality`() {
        assertTrue(LegacyAmount(BigDecimal("123.456789")).equals(LegacyAmount("123.456789")))
    }

    @Test
    fun `testInEquality`() {
        assertFalse(LegacyAmount(BigDecimal("123.456789")).equals(LegacyAmount("123.45678")))
    }
}
