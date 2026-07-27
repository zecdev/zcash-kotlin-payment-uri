package org.zecdev.zip321

import org.zecdev.zip321.model.NonNegativeAmount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class NonNegativeAmountTests {
    // MARK: - zec() valid decimal strings
    //
    // Amount cases mirror the shared conformance corpus
    // (zcash-zip321-test-vectors: vectors/valid/amounts.json), hardcoded here so
    // this unit suite needs no JSON loading. Each triple is
    // (decimalString, expectedZatoshi, corpusVectorOrRuleName).
    private val validDecimalStrings: List<Triple<String, ULong, String>> =
        listOf(
            Triple("20999999.99999999", 2_099_999_999_999_999uL, "amount_just_below_max_money"),
            Triple("21000000", 2_100_000_000_000_000uL, "amount_max_money"),
            Triple("050", 5_000_000_000uL, "amount_leading_zeros_050"),
            Triple("00.500", 50_000_000uL, "amount_leading_and_trailing_zeros_00_500"),
            Triple("1", 100_000_000uL, "amount_one_with_empty_message"),
            Triple("3768769.02796286", 376_876_902_796_286uL, "amount_parse_simple_large_decimal"),
            Triple("0.00000001", 1uL, "amount_roundtrip_1_zat"),
            Triple("0.00001", 1_000uL, "amount_roundtrip_1000_zat"),
            Triple("0.001", 100_000uL, "amount_roundtrip_100000_zat"),
            Triple("1000", 100_000_000_000uL, "amount_roundtrip_100000000000_zat"),
            // NonNegativeAmount is amount-agnostic: zero is representable; the zero-valued
            // transparent-output policy lives at the Payment level.
            Triple("0", 0uL, "zero is valid at the NonNegativeAmount level"),
            Triple("0.00000000", 0uL, "zero with full fractional padding"),
        )

    @Test
    fun zecParsesValidDecimalString() {
        for ((string, expectedZatoshi, comment) in validDecimalStrings) {
            val parsed = NonNegativeAmount.zec(string).getOrThrow()

            assertEquals(expectedZatoshi, parsed.value, comment)
        }
    }

    // MARK: - zec() invalid decimal strings
    //
    // Rejection cases mirror the shared conformance corpus
    // (zcash-zip321-test-vectors: vectors/invalid/amounts.json) plus additional
    // strict-grammar probes. Each triple is
    // (decimalString, expectedError, corpusVectorOrRuleName).
    private val invalidDecimalStrings: List<Triple<String, NonNegativeAmount.AmountException, String>> =
        listOf(
            // i64::MAX + 1: fits a ULong but is necessarily > MAX_MONEY.
            Triple("9223372036854775808", NonNegativeAmount.AmountException.ExceededSupply, "invalid_amount_exceeds_i64"),
            // u64 wrap-around probe: overflows ULong during parsing and must NOT wrap into a small positive value.
            Triple("18446744073709551624", NonNegativeAmount.AmountException.ExceededSupply, "invalid_amount_overflow_wraps_positive"),
            // one zatoshi over MAX_MONEY.
            Triple("21000000.00000001", NonNegativeAmount.AmountException.ExceededSupply, "invalid_amount_exceeds_max_money"),
            // a leading sign fails the `1*DIGIT` prefix of the grammar: it is a malformed
            // string, never a negative value (which the type cannot represent anyway).
            Triple("-1", NonNegativeAmount.AmountException.InvalidDecimalString, "invalid_amount_negative"),
            Triple("123.", NonNegativeAmount.AmountException.InvalidDecimalString, "invalid_amount_trailing_decimal_point"),
            Triple(".5", NonNegativeAmount.AmountException.InvalidDecimalString, "invalid_amount_leading_decimal_point"),
            // strict ZIP-321 grammar probes beyond the corpus:
            Triple("", NonNegativeAmount.AmountException.InvalidDecimalString, "empty string"),
            Triple("-", NonNegativeAmount.AmountException.InvalidDecimalString, "bare minus sign"),
            Triple("+", NonNegativeAmount.AmountException.InvalidDecimalString, "bare plus sign"),
            Triple("+1", NonNegativeAmount.AmountException.InvalidDecimalString, "explicit positive sign"),
            Triple("1,5", NonNegativeAmount.AmountException.InvalidDecimalString, "comma is not a decimal separator"),
            Triple("1e5", NonNegativeAmount.AmountException.InvalidDecimalString, "scientific notation"),
            Triple(" 1", NonNegativeAmount.AmountException.InvalidDecimalString, "leading whitespace"),
            Triple("1 ", NonNegativeAmount.AmountException.InvalidDecimalString, "trailing whitespace"),
            Triple("1.2.3", NonNegativeAmount.AmountException.InvalidDecimalString, "second decimal point"),
            Triple("1.", NonNegativeAmount.AmountException.InvalidDecimalString, "decimal point with no fractional digits"),
            Triple(".", NonNegativeAmount.AmountException.InvalidDecimalString, "bare decimal point"),
            Triple("0.123456789", NonNegativeAmount.AmountException.TooManyFractionalDigits, "9 fractional digits exceed the 8-digit maximum"),
        )

    @Test
    fun zecRejectsInvalidDecimalString() {
        for ((string, expectedError, comment) in invalidDecimalStrings) {
            NonNegativeAmount.zec(string).fold(
                onSuccess = { amount ->
                    fail("expected rejection of '$string' ($comment) but got ${amount.value} zatoshi")
                },
                onFailure = { error ->
                    assertEquals(expectedError, error, comment)
                },
            )
        }
    }

    // MARK: - zatoshi() integer factory

    @Test
    fun zatoshiFactoryAcceptsBounds() {
        assertEquals(0uL, NonNegativeAmount.zatoshi(0uL).getOrThrow().value)
        assertEquals(1uL, NonNegativeAmount.zatoshi(1uL).getOrThrow().value)
        assertEquals(2_100_000_000_000_000uL, NonNegativeAmount.zatoshi(NonNegativeAmount.MAX_MONEY).getOrThrow().value)
    }

    @Test
    fun zatoshiFactoryRejectsOutOfRange() {
        // ExceededSupply is the only reachable failure: the parameter is a ULong, so
        // negative zatoshi counts are unrepresentable by construction and there is no
        // `zatoshi(-1)` case to test (the Swift library documents the same property).
        assertEquals(
            NonNegativeAmount.AmountException.ExceededSupply,
            NonNegativeAmount.zatoshi(NonNegativeAmount.MAX_MONEY + 1uL).exceptionOrNull(),
        )
        assertEquals(
            NonNegativeAmount.AmountException.ExceededSupply,
            NonNegativeAmount.zatoshi(ULong.MAX_VALUE).exceptionOrNull(),
        )
    }

    // MARK: - error taxonomy
    //
    // `NegativeAmount` is retained so the Kotlin and Swift error taxonomies stay identical,
    // but no factory can produce it: `zatoshi()` takes an unsigned count and `zec()` rejects
    // a leading sign as `InvalidDecimalString`. Pin its identity/message so the retained case
    // cannot silently drift.
    @Test
    fun negativeAmountCaseIsRetainedButUnreachable() {
        assertEquals("Amount cannot be negative", NonNegativeAmount.AmountException.NegativeAmount.message)
        assertTrue(
            invalidDecimalStrings.none { (_, error, _) -> error == NonNegativeAmount.AmountException.NegativeAmount },
            "no decimal string maps to NegativeAmount",
        )
    }

    // MARK: - decimalString() rendering
    //
    // Expected renderings match the reference `amount_str` (librustzcash
    // zip321): whole part always present, fraction only if nonzero, trailing
    // zeros trimmed. Corpus `canonicalUri` amounts are covered by the pairs
    // whose input string differs from the canonical rendering ("050" -> "50",
    // "00.500" -> "0.5").
    private val renderedDecimalStrings: List<Pair<ULong, String>> =
        listOf(
            0uL to "0",
            1uL to "0.00000001",
            1_000uL to "0.00001",
            100_000uL to "0.001",
            50_000_000uL to "0.5",
            100_000_000uL to "1",
            5_000_000_000uL to "50",
            100_000_000_000uL to "1000",
            376_876_902_796_286uL to "3768769.02796286",
            2_099_999_999_999_999uL to "20999999.99999999",
            2_100_000_000_000_000uL to "21000000",
        )

    @Test
    fun decimalStringRendersCanonically() {
        for ((zats, expected) in renderedDecimalStrings) {
            val amount = NonNegativeAmount.zatoshi(zats).getOrThrow()

            assertEquals(expected, amount.decimalString())
        }
    }

    @Test
    fun decimalStringRoundTrips() {
        for ((zats, rendered) in renderedDecimalStrings) {
            val reparsed = NonNegativeAmount.zec(rendered).getOrThrow()

            assertEquals(zats, reparsed.value)
            assertEquals(rendered, reparsed.decimalString())
        }
    }

    // MARK: - Comparable / equality

    @Test
    fun comparableOrdersByZatoshi() {
        val one = NonNegativeAmount.zatoshi(1uL).getOrThrow()
        val two = NonNegativeAmount.zatoshi(2uL).getOrThrow()

        assertTrue(one < two)
        assertTrue(!(two < one))
        assertEquals(NonNegativeAmount.zec("0.00000001").getOrThrow(), one)
    }

    @Test
    fun hashableAgreesWithEquality() {
        val a = NonNegativeAmount.zec("1").getOrThrow()
        val b = NonNegativeAmount.zatoshi(100_000_000uL).getOrThrow()

        assertEquals(1, setOf(a, b).size)
    }
}
