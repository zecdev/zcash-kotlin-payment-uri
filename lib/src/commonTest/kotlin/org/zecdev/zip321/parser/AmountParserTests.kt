package org.zecdev.zip321.parser

import org.zecdev.zip321.ZIP321
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Accept/reject table drawn from the shared conformance corpus
 * (`vectors/{valid,invalid}/amounts.json`), oracle-verified against the librustzcash `zip321`
 * reference `parse_amount`. Provenance for each case is noted inline.
 */
class AmountParserTests {
    // MARK: Accepted (valid/amounts.json)

    @Test
    fun `accepts valid amounts`() {
        // Each pair is (input, expected zatoshi); the provenance vector name precedes it.
        val cases =
            listOf(
                // amount_just_below_max_money
                "20999999.99999999" to 2_099_999_999_999_999uL,
                // amount_max_money (== MAX_MONEY)
                "21000000" to 2_100_000_000_000_000uL,
                // amount_leading_zeros_050
                "050" to 5_000_000_000uL,
                // amount_leading_and_trailing_zeros_00_500
                "00.500" to 50_000_000uL,
                // amount_roundtrip_1_zat
                "0.00000001" to 1uL,
                // amount_roundtrip_1000_zat
                "0.00001" to 1_000uL,
                // amount_roundtrip_100000_zat
                "0.001" to 100_000uL,
                // amount_roundtrip_100000000_zat
                "1" to 100_000_000uL,
                // amount_roundtrip_100000000000_zat
                "1000" to 100_000_000_000uL,
            )
        for ((input, expected) in cases) {
            assertEquals(expected, AmountParser.parse(input, 0u).value, "amount '$input'")
        }
    }

    // MARK: Rejected (invalid/amounts.json + grammar edges)

    @Test
    fun `rejects amount exceeding i64`() {
        // invalid_amount_exceeds_i64: i64::MAX + 1
        assertTrue(throwsAmountError { AmountParser.parse("9223372036854775808", 0u) })
    }

    @Test
    fun `rejects amount overflow wraps positive`() {
        // invalid_amount_overflow_wraps_positive
        assertTrue(throwsAmountError { AmountParser.parse("18446744073709551624", 0u) })
    }

    @Test
    fun `rejects amount exceeding max money`() {
        // invalid_amount_exceeds_max_money: one zatoshi over MAX_MONEY
        val error =
            assertFailsWith<ZIP321.Errors.AmountExceededSupply> {
                AmountParser.parse("21000000.00000001", 3u)
            }
        assertEquals(3u, error.value)
    }

    @Test
    fun `rejects negative amount`() {
        // invalid_amount_negative: "-1" is not representable by the amountparam grammar
        assertTrue(throwsAmountError { AmountParser.parse("-1", 0u) })
    }

    @Test
    fun `rejects trailing decimal point`() {
        // invalid_amount_trailing_decimal_point: "123." has no fractional digits
        val error =
            assertFailsWith<ZIP321.Errors.InvalidParamValue> {
                AmountParser.parse("123.", 0u)
            }
        assertEquals("amount", error.param)
        assertEquals(null, error.index)
    }

    @Test
    fun `rejects leading decimal point`() {
        // invalid_amount_leading_decimal_point: ".5" has no whole-number part
        assertTrue(throwsAmountError { AmountParser.parse(".5", 0u) })
    }

    @Test
    fun `rejects percent escape in amount`() {
        // invalid_percent_encoded_amount: amount values are never percent-decoded
        assertTrue(throwsAmountError { AmountParser.parse("1%30", 0u) })
    }

    @Test
    fun `rejects more than eight fractional digits`() {
        val error =
            assertFailsWith<ZIP321.Errors.AmountTooSmall> {
                AmountParser.parse("1.123456789", 2u)
            }
        assertEquals(2u, error.value)
    }

    @Test
    fun `rejects empty and garbage`() {
        assertTrue(throwsAmountError { AmountParser.parse("", 0u) })
        assertTrue(throwsAmountError { AmountParser.parse("abc", 0u) })
        assertTrue(throwsAmountError { AmountParser.parse("1.2.3", 0u) })
        assertTrue(throwsAmountError { AmountParser.parse(" 1", 0u) })
        assertTrue(throwsAmountError { AmountParser.parse("1e8", 0u) })
    }

    @Test
    fun `rejects garbage at a non-zero paramindex`() {
        // The catch-all "invalid decimal string" mapping tags the concrete index when non-zero
        // (as opposed to the empty-paramindex `null` asserted by the other malformed-amount cases).
        val error =
            assertFailsWith<ZIP321.Errors.InvalidParamValue> {
                AmountParser.parse("abc", 5u)
            }
        assertEquals("amount", error.param)
        assertEquals(5u, error.index)
    }

    private fun throwsAmountError(body: () -> Unit): Boolean =
        try {
            body()
            false
        } catch (_: ZIP321.Errors) {
            true
        }
}
