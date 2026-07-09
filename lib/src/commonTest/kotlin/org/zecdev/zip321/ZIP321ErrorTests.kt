package org.zecdev.zip321

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Direct, exhaustive tests of [ZIP321Error]'s two mapping functions, neither of which is fully
 * exercised by the parser/conformance suites (which only ever reach a handful of cases per real
 * input): [ZIP321Error.withIndex] over every case, and the internal [ZIP321Error.Companion.from]
 * translation from the v1 [ZIP321.Errors] taxonomy over every legacy case (mirrors the Swift
 * reference's equivalent exhaustive `ZIP321Error.withIndex`/`init(_:)`/`mapFrom` tests, S16).
 */
class ZIP321ErrorTests {
    // MARK: - withIndex

    @Test
    fun `withIndex replaces the index on every index-bearing case`() {
        val cases: List<Pair<ZIP321Error, (UInt?) -> ZIP321Error>> =
            listOf(
                ZIP321Error.InvalidBase64(1u) to { i -> ZIP321Error.InvalidBase64(i) },
                ZIP321Error.MemoBytesError(1u) to { i -> ZIP321Error.MemoBytesError(i) },
                ZIP321Error.TransparentMemo(1u) to { i -> ZIP321Error.TransparentMemo(i) },
                ZIP321Error.ZeroValuedTransparentOutput(1u) to { i -> ZIP321Error.ZeroValuedTransparentOutput(i) },
                ZIP321Error.RecipientMissing(1u) to { i -> ZIP321Error.RecipientMissing(i) },
                ZIP321Error.InvalidAddress(1u) to { i -> ZIP321Error.InvalidAddress(i) },
                ZIP321Error.AmountExceededSupply(1u) to { i -> ZIP321Error.AmountExceededSupply(i) },
                ZIP321Error.AmountInvalid(1u) to { i -> ZIP321Error.AmountInvalid(i) },
            )

        for ((original, expected) in cases) {
            assertEquals(expected(5u), original.withIndex(5u), "$original -> withIndex(5u)")
            assertEquals(expected(null), original.withIndex(null), "$original -> withIndex(null)")
        }
    }

    @Test
    fun `withIndex on DuplicateParameter replaces the index but keeps the parameter name`() {
        val original = ZIP321Error.DuplicateParameter("address", 1u)
        assertEquals(ZIP321Error.DuplicateParameter("address", 7u), original.withIndex(7u))
        assertEquals(ZIP321Error.DuplicateParameter("address", null), original.withIndex(null))
    }

    @Test
    fun `withIndex is a no-op on every non-indexed case`() {
        val nonIndexed: List<ZIP321Error> =
            listOf(
                ZIP321Error.TooManyPayments(10_000u),
                ZIP321Error.UnknownRequiredParameter("req-future"),
                ZIP321Error.InvalidParamIndex("00123"),
                ZIP321Error.InvalidURI(ZIP321Error.StaticReason.NOT_ZCASH_SCHEME),
                ZIP321Error.ParseError(ZIP321Error.StaticReason.MALFORMED_URI),
            )

        for (error in nonIndexed) {
            assertSame(error, error.withIndex(9u), "$error should be returned unchanged by withIndex")
        }
    }

    // MARK: - ZIP321Error.Companion.from (v1 -> v2 taxonomy translation)

    @Test
    fun `from maps every v1 Errors case onto its v2 ZIP321Error counterpart`() {
        assertEquals(
            ZIP321Error.AmountExceededSupply(5u),
            ZIP321Error.from(ZIP321.Errors.AmountExceededSupply(5u)),
        )
        assertEquals(
            ZIP321Error.AmountExceededSupply(null),
            ZIP321Error.from(ZIP321.Errors.AmountExceededSupply(0u)),
            "index 0u normalizes to null (the empty paramindex sentinel)",
        )

        assertEquals(
            ZIP321Error.AmountInvalid(3u),
            ZIP321Error.from(ZIP321.Errors.AmountTooSmall(3u)),
        )

        assertEquals(
            ZIP321Error.DuplicateParameter("amount", 2u),
            ZIP321Error.from(ZIP321.Errors.DuplicateParameter("amount", 2u)),
        )

        assertEquals(
            ZIP321Error.InvalidAddress(4u),
            ZIP321Error.from(ZIP321.Errors.InvalidAddress(4u)),
        )

        assertEquals(ZIP321Error.InvalidBase64(null), ZIP321Error.from(ZIP321.Errors.InvalidBase64))

        assertEquals(
            ZIP321Error.InvalidURI(ZIP321Error.StaticReason.MALFORMED_URI),
            ZIP321Error.from(ZIP321.Errors.InvalidURI),
        )

        assertEquals(
            ZIP321Error.MemoBytesError(6u),
            ZIP321Error.from(ZIP321.Errors.MemoBytesError(IllegalStateException("boom"), 6u)),
        )

        assertEquals(
            ZIP321Error.TooManyPayments(10_001u),
            ZIP321Error.from(ZIP321.Errors.TooManyPayments(10_001u)),
        )

        assertEquals(
            ZIP321Error.TransparentMemo(7u),
            ZIP321Error.from(ZIP321.Errors.TransparentMemoNotAllowed(7u)),
        )

        assertEquals(
            ZIP321Error.RecipientMissing(8u),
            ZIP321Error.from(ZIP321.Errors.RecipientMissing(8u)),
        )

        assertEquals(
            ZIP321Error.InvalidParamIndex("00123"),
            ZIP321Error.from(ZIP321.Errors.InvalidParamIndex("00123")),
        )

        // InvalidParamValue: the `amount` param name maps to AmountInvalid; every other param name
        // maps to the generic structural ParseError.
        assertEquals(
            ZIP321Error.AmountInvalid(9u),
            ZIP321Error.from(ZIP321.Errors.InvalidParamValue("amount", 9u)),
        )
        assertEquals(
            ZIP321Error.ParseError(ZIP321Error.StaticReason.INVALID_PARAMETER),
            ZIP321Error.from(ZIP321.Errors.InvalidParamValue("label", 9u)),
        )

        assertEquals(
            ZIP321Error.ParseError(ZIP321Error.StaticReason.MALFORMED_URI),
            ZIP321Error.from(ZIP321.Errors.ParseError("unexpected input")),
        )

        assertEquals(
            ZIP321Error.ParseError(ZIP321Error.StaticReason.INVALID_PARAMETER),
            ZIP321Error.from(ZIP321.Errors.QcharDecodeFailed(1u, "label", "100%")),
        )

        assertEquals(
            ZIP321Error.UnknownRequiredParameter("req-future"),
            ZIP321Error.from(ZIP321.Errors.UnknownRequiredParameter("req-future")),
        )

        assertEquals(
            ZIP321Error.ParseError(ZIP321Error.StaticReason.INVALID_PARAMETER),
            ZIP321Error.from(ZIP321.Errors.InvalidParamName("1nvalid")),
        )
    }

    // MARK: - data class equals/hashCode/toString/copy (auto-generated but otherwise unexercised)

    @Test
    fun `every case supports equals hashCode and copy`() {
        val original = ZIP321Error.InvalidAddress(3u)
        val same = ZIP321Error.InvalidAddress(3u)
        val copied = original.copy(index = 9u)

        assertEquals(original, same)
        assertEquals(original.hashCode(), same.hashCode())
        assertEquals(ZIP321Error.InvalidAddress(9u), copied)
        assertEquals("InvalidAddress(index=3)", original.toString())
    }
}
